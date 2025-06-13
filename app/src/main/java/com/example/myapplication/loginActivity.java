package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.FirebaseNetworkException;

public class loginActivity extends AppCompatActivity {

    // --- UI Elements ---
    private EditText etUsernameAsEmail, etPassword;
    private Button btnLogin;
    private TextView tvSignup;

    // --- Firebase & SharedPreferences ---
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    public static final String TAG = "LoginActivity";
    public static final String PREFS_NAME = "AppSessionPrefs";
    public static final String KEY_LAST_LOGIN_TIME = "lastLoginTime";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance(); // This line now runs after FirebaseApp is initialized
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        initializeViews();
        setupClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkSessionValidity();
    }

    private void loginUser() {
        String email = etUsernameAsEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etUsernameAsEmail.setError("Email is required");
            etUsernameAsEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etUsernameAsEmail.setError("Please enter a valid email");
            etUsernameAsEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        showCustomToast("Logging in...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        saveLoginTimestamp();
                        showCustomToast("Login Successful!");
                        navigateToNewsScreen();
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        handleLoginError(task.getException());
                    }
                });
    }

    private void handleLoginError(Exception exception) {
        String message;
        String title = "Login Failed";

        try {
            throw exception;
        } catch (FirebaseNetworkException e) {
            // Handle network connectivity issues
            title = "Network Error";
            message = "Please check your internet connection and try again.";
            Log.e(TAG, "Network error during login: ", e);
        } catch (FirebaseAuthInvalidUserException e) {
            message = "No account found with this email address.";
        } catch (FirebaseAuthInvalidCredentialsException e) {
            message = "Incorrect password. Please try again.";
        } catch (Exception e) {
            message = "Login failed. An unexpected error occurred.";
            Log.e(TAG, "Unhandled login error: ", e);
        }

        showErrorDialog(title, message);
    }

    // --- UPDATED: This method now uses your custom theme ---
    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this, R.style.CustomDialogTheme) // Use the custom theme here
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    // --- NEW: Helper method to show your custom styled Toast ---
    private void showCustomToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast_layout, null);

        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.BOTTOM, 0, 150); // Position it a little higher from the bottom
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    // --- Other methods (no changes needed) ---
    private void checkSessionValidity() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            long lastLoginTime = sharedPreferences.getLong(KEY_LAST_LOGIN_TIME, 0);
            long currentTime = System.currentTimeMillis();
            long oneDayInMillis = 24 * 60 * 60 * 1000;
            if (currentTime - lastLoginTime > oneDayInMillis) {
                mAuth.signOut();
                showCustomToast("Session expired. Please log in again.");
            } else {
                navigateToNewsScreen();
            }
        }
    }

    private void saveLoginTimestamp() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(KEY_LAST_LOGIN_TIME, System.currentTimeMillis());
        editor.apply();
    }

    private void initializeViews() {
        etUsernameAsEmail = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvSignup = findViewById(R.id.tv_signup);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> loginUser());
        tvSignup.setOnClickListener(v -> {
            startActivity(new Intent(loginActivity.this, sign_upActivity.class));
        });
    }

    private void navigateToNewsScreen() {
        Intent intent = new Intent(loginActivity.this, NewsScreenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}