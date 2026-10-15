package com.trantrongmanh.lab5.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.trantrongmanh.lab5.model.User;

/**
 * Central Firebase Auth + Firestore helper.
 */
public class AuthManager {

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String errorMessage);
    }

    public interface VoidCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface UserCallback {
        void onSuccess(User user);
        void onError(String errorMessage);
    }

    private static AuthManager instance;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private static final String USERS_COLLECTION = "users";

    private AuthManager() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore    = FirebaseFirestore.getInstance();
    }

    public static synchronized AuthManager getInstance() {
        if (instance == null) instance = new AuthManager();
        return instance;
    }

    // ── State ──────────────────────────────────────────────────────────────────

    public boolean isLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public void signOut() {
        firebaseAuth.signOut();
    }

    // ── Register ───────────────────────────────────────────────────────────────

    public void register(String email, String password, String displayName, AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser fbUser = firebaseAuth.getCurrentUser();
                        if (fbUser == null) { callback.onError("Đăng ký thất bại."); return; }

                        // Update display name
                        fbUser.updateProfile(new UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName).build());

                        // Save to Firestore
                        User user = new User(fbUser.getUid(), displayName, email);
                        firestore.collection(USERS_COLLECTION)
                                .document(fbUser.getUid())
                                .set(user)
                                .addOnSuccessListener(v -> callback.onSuccess(fbUser))
                                .addOnFailureListener(e -> callback.onSuccess(fbUser));
                    } else {
                        callback.onError(localizeError(
                                task.getException() != null ? task.getException().getMessage() : ""));
                    }
                });
    }

    // ── Login ──────────────────────────────────────────────────────────────────

    public void login(String email, String password, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(firebaseAuth.getCurrentUser());
                    } else {
                        callback.onError(localizeError(
                                task.getException() != null ? task.getException().getMessage() : ""));
                    }
                });
    }

    // ── Reset password ─────────────────────────────────────────────────────────

    public void resetPassword(String email, VoidCallback callback) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(localizeError(e.getMessage())));
    }

    // ── Update profile ─────────────────────────────────────────────────────────

    public void updateDisplayName(String newName, VoidCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) { callback.onError("Chưa đăng nhập"); return; }

        user.updateProfile(new UserProfileChangeRequest.Builder()
                .setDisplayName(newName).build())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        firestore.collection(USERS_COLLECTION)
                                .document(user.getUid())
                                .update("displayName", newName)
                                .addOnSuccessListener(v -> callback.onSuccess())
                                .addOnFailureListener(e -> callback.onSuccess());
                    } else {
                        callback.onError("Cập nhật thất bại");
                    }
                });
    }

    public void updateDefaultCity(String city, VoidCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) { callback.onError("Chưa đăng nhập"); return; }
        firestore.collection(USERS_COLLECTION)
                .document(user.getUid())
                .update("defaultCity", city)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ── Read Firestore profile ─────────────────────────────────────────────────

    public void getUserProfile(UserCallback callback) {
        FirebaseUser fbUser = getCurrentUser();
        if (fbUser == null) { callback.onError("Chưa đăng nhập"); return; }

        firestore.collection(USERS_COLLECTION)
                .document(fbUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onSuccess(doc.toObject(User.class));
                    } else {
                        User user = new User(fbUser.getUid(),
                                fbUser.getDisplayName() != null ? fbUser.getDisplayName() : "Người dùng",
                                fbUser.getEmail());
                        callback.onSuccess(user);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ── Error localizer ────────────────────────────────────────────────────────

    private String localizeError(String msg) {
        if (msg == null) return "Đã xảy ra lỗi";
        if (msg.contains("email address is already in use")) return "Email này đã được sử dụng.";
        if (msg.contains("password is invalid") || msg.contains("no user record"))
            return "Email hoặc mật khẩu không đúng.";
        if (msg.contains("badly formatted"))    return "Định dạng email không hợp lệ.";
        if (msg.contains("weak-password"))      return "Mật khẩu quá yếu (tối thiểu 6 ký tự).";
        if (msg.contains("network"))            return "Lỗi kết nối mạng. Vui lòng thử lại.";
        return msg;
    }
}
