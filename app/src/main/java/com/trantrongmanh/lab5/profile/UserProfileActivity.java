package com.trantrongmanh.lab5.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import com.trantrongmanh.lab5.R;
import com.trantrongmanh.lab5.auth.AuthManager;
import com.trantrongmanh.lab5.auth.LoginActivity;
import com.trantrongmanh.lab5.model.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserProfileActivity extends AppCompatActivity {

    private TextView tvProfileName, tvProfileEmail, tvMemberSince;
    private ImageView ivAvatar;
    private TextInputLayout tilName, tilCity;
    private TextInputEditText etName, etCity;
    private MaterialButton btnSave, btnLogout;
    private ProgressBar progressProfile;

    private AuthManager authManager;
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        authManager = AuthManager.getInstance();

        // Guard: if not logged in, go to login
        if (!authManager.isLoggedIn()) {
            goToLogin();
            return;
        }

        setupToolbar();
        bindViews();
        setupClickListeners();
        loadProfile();
        animateEntry();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.profile);
        }
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
    }

    private void bindViews() {
        tvProfileName  = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvMemberSince  = findViewById(R.id.tvMemberSince);
        ivAvatar       = findViewById(R.id.ivAvatar);
        tilName        = findViewById(R.id.tilName);
        tilCity        = findViewById(R.id.tilCity);
        etName         = findViewById(R.id.etName);
        etCity         = findViewById(R.id.etCity);
        btnSave        = findViewById(R.id.btnSave);
        btnLogout      = findViewById(R.id.btnLogout);
        progressProfile= findViewById(R.id.progressProfile);
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void animateEntry() {
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        findViewById(R.id.tilName).startAnimation(slideIn);
    }

    // ── Load profile ───────────────────────────────────────────────────────────

    private void loadProfile() {
        setLoading(true);
        authManager.getUserProfile(new AuthManager.UserCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                if (user == null) return;
                populateUI(user);
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                // Fall back to Firebase Auth data
                FirebaseUser fbUser = authManager.getCurrentUser();
                if (fbUser != null) {
                    tvProfileName.setText(fbUser.getDisplayName() != null
                            ? fbUser.getDisplayName() : "Người dùng");
                    tvProfileEmail.setText(fbUser.getEmail());
                    etName.setText(fbUser.getDisplayName());
                }
                showSnack("Không thể tải hồ sơ: " + errorMessage);
            }
        });
    }

    private void populateUI(User user) {
        tvProfileName.setText(user.getDisplayName() != null
                ? user.getDisplayName() : "Người dùng");
        tvProfileEmail.setText(user.getEmail());

        etName.setText(user.getDisplayName());
        etCity.setText(user.getDefaultCity());

        if (user.getCreatedAt() > 0) {
            String since = DATE_FORMAT.format(new Date(user.getCreatedAt()));
            tvMemberSince.setText(getString(R.string.member_since, since));
        }

        // Avatar: use initials-based color background (simple approach)
        setAvatarInitials(user.getDisplayName());
    }

    private void setAvatarInitials(String name) {
        // Simple placeholder — in a full app you'd use Glide with a URL
        if (name != null && !name.isEmpty()) {
            ivAvatar.setBackgroundResource(R.drawable.bg_circle_white);
            ivAvatar.setImageResource(android.R.mipmap.sym_def_app_icon);
        }
    }

    // ── Save profile ───────────────────────────────────────────────────────────

    private void saveProfile() {
        tilName.setError(null);
        tilCity.setError(null);

        String name = getText(etName);
        String city = getText(etCity);

        if (TextUtils.isEmpty(name)) {
            tilName.setError("Vui lòng nhập tên");
            return;
        }

        setLoading(true);
        btnSave.setEnabled(false);

        // Update display name
        authManager.updateDisplayName(name, new AuthManager.VoidCallback() {
            @Override
            public void onSuccess() {
                // Also update city
                if (!TextUtils.isEmpty(city)) {
                    authManager.updateDefaultCity(city, new AuthManager.VoidCallback() {
                        @Override
                        public void onSuccess() {
                            onSaveComplete(name);
                        }
                        @Override
                        public void onError(String errorMessage) {
                            onSaveComplete(name); // name was saved, city failed — non-fatal
                        }
                    });
                } else {
                    onSaveComplete(name);
                }
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                btnSave.setEnabled(true);
                showSnack("Lỗi: " + errorMessage);
            }
        });
    }

    private void onSaveComplete(String newName) {
        setLoading(false);
        btnSave.setEnabled(true);
        tvProfileName.setText(newName);

        // Button bounce animation
        Animation bounce = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        btnSave.startAnimation(bounce);

        showSnack(getString(R.string.update_success));
    }

    // ── Logout ─────────────────────────────────────────────────────────────────

    private void confirmLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.logout)
                .setMessage("Bạn có chắc muốn đăng xuất?")
                .setPositiveButton(R.string.logout, (d, w) -> doLogout())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void doLogout() {
        authManager.signOut();
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        progressProfile.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showSnack(String msg) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show();
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
