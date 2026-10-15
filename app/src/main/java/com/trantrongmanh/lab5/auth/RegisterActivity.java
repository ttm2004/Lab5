package com.trantrongmanh.lab5.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import com.trantrongmanh.lab5.MainActivity;
import com.trantrongmanh.lab5.R;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private static final int TIMEOUT_MS = 15000; // 15 giây timeout

    private TextInputLayout tilDisplayName, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etDisplayName, etEmail, etPassword, etConfirmPassword;
    private View progressRegister;
    private AuthManager authManager;
    private Handler timeoutHandler = new Handler();
    private boolean requestDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authManager = AuthManager.getInstance();
        bindViews();
        setupClickListeners();
    }

    private void bindViews() {
        tilDisplayName     = findViewById(R.id.tilDisplayName);
        tilEmail           = findViewById(R.id.tilEmail);
        tilPassword        = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etDisplayName      = findViewById(R.id.etDisplayName);
        etEmail            = findViewById(R.id.etEmail);
        etPassword         = findViewById(R.id.etPassword);
        etConfirmPassword  = findViewById(R.id.etConfirmPassword);
        progressRegister   = findViewById(R.id.progressRegister);
    }

    private void setupClickListeners() {
        findViewById(R.id.btnRegister).setOnClickListener(v -> attemptRegister());
        findViewById(R.id.tvGoToLogin).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
    }

    private void attemptRegister() {
        clearErrors();

        String name    = getText(etDisplayName);
        String email   = getText(etEmail);
        String pass    = getText(etPassword);
        String confirm = getText(etConfirmPassword);

        boolean valid = true;
        if (TextUtils.isEmpty(name)) {
            tilDisplayName.setError("Vui lòng nhập họ tên");
            valid = false;
        }
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Vui lòng nhập email");
            valid = false;
        }
        if (TextUtils.isEmpty(pass) || pass.length() < 6) {
            tilPassword.setError("Mật khẩu tối thiểu 6 ký tự");
            valid = false;
        }
        if (!pass.equals(confirm)) {
            tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            valid = false;
        }
        if (!valid) return;

        requestDone = false;
        setLoading(true);

        // Timeout 15 giây — nếu Firebase không phản hồi thì thông báo lỗi mạng
        timeoutHandler.postDelayed(() -> {
            if (!requestDone) {
                requestDone = true;
                setLoading(false);
                showError("Hết thời gian chờ. Kiểm tra kết nối mạng và thử lại.");
                Log.e(TAG, "Register timeout after " + TIMEOUT_MS + "ms");
            }
        }, TIMEOUT_MS);

        Log.d(TAG, "Attempting register with email: " + email);

        authManager.register(email, pass, name, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                if (requestDone) return; // timeout đã xử lý
                requestDone = true;
                timeoutHandler.removeCallbacksAndMessages(null);
                setLoading(false);
                Log.d(TAG, "Register success: " + user.getUid());
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }

            @Override
            public void onError(String errorMessage) {
                if (requestDone) return;
                requestDone = true;
                timeoutHandler.removeCallbacksAndMessages(null);
                setLoading(false);
                Log.e(TAG, "Register error: " + errorMessage);
                showError(errorMessage);
            }
        });
    }

    private void setLoading(boolean loading) {
        progressRegister.setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnRegister).setEnabled(!loading);
    }

    private void showError(String msg) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show();
    }

    private void clearErrors() {
        tilDisplayName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timeoutHandler.removeCallbacksAndMessages(null);
    }
}
