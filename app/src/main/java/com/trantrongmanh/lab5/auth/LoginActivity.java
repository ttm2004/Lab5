package com.trantrongmanh.lab5.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import com.trantrongmanh.lab5.MainActivity;
import com.trantrongmanh.lab5.R;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private View progressLogin;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = AuthManager.getInstance();

        // If already logged in skip login
        if (authManager.isLoggedIn()) {
            goToMain();
            return;
        }

        bindViews();
        setupClickListeners();
        animateEntry();
    }

    private void bindViews() {
        tilEmail       = findViewById(R.id.tilEmail);
        tilPassword    = findViewById(R.id.tilPassword);
        etEmail        = findViewById(R.id.etEmail);
        etPassword     = findViewById(R.id.etPassword);
        progressLogin  = findViewById(R.id.progressLogin);
    }

    private void setupClickListeners() {
        // Login button
        findViewById(R.id.btnLogin).setOnClickListener(v -> attemptLogin());

        // Register link
        findViewById(R.id.tvGoToRegister).setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // Forgot password
        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void animateEntry() {
        View card = findViewById(R.id.layoutHeader);
        if (card != null) {
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
            card.startAnimation(anim);
        }
    }

    private void attemptLogin() {
        clearErrors();

        String email    = getText(etEmail);
        String password = getText(etPassword);

        boolean valid = true;
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Vui lòng nhập email");
            valid = false;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            valid = false;
        }
        if (!valid) return;

        setLoading(true);
        authManager.login(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                setLoading(false);
                goToMain();
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                showError(errorMessage);
            }
        });
    }

    private void showForgotPasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reset_password, null);
        TextInputEditText etResetEmail = dialogView.findViewById(R.id.etResetEmail);

        // Pre-fill with current email if any
        String currentEmail = getText(etEmail);
        if (!TextUtils.isEmpty(currentEmail)) etResetEmail.setText(currentEmail);

        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_password)
                .setView(dialogView)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String email = etResetEmail.getText() != null
                            ? etResetEmail.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(email)) return;

                    authManager.resetPassword(email, new AuthManager.VoidCallback() {
                        @Override
                        public void onSuccess() {
                            Snackbar.make(findViewById(android.R.id.content),
                                    R.string.reset_email_sent, Snackbar.LENGTH_LONG).show();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            showError(errorMessage);
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void setLoading(boolean loading) {
        progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnLogin).setEnabled(!loading);
    }

    private void showError(String msg) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show();
    }

    private void clearErrors() {
        tilEmail.setError(null);
        tilPassword.setError(null);
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
