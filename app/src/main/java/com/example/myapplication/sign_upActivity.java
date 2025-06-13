package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;






public class sign_upActivity extends AppCompatActivity {

    private TextInputLayout tilUsername, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etUsername, etEmail, etPassword, etConfirmPassword;
    private Button btnSignup;
    private TextView tvSignin;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore fStore;
    public static final String TAG = "SignUpActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        initializeViews();
        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        setupClickListeners();
        setupTextWatchers();
    }

    private void attemptRegistration() {
        if (!isInputValid()) {
            return;
        }

        showLoading(true);

        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(etPassword.getText()).toString().trim();
        String username = Objects.requireNonNull(etUsername.getText()).toString().trim();


        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // If user creation is successful, now store their data in Firestore.
                        Log.d(TAG, "createUserWithEmail:success");
                        storeUserData(username, email);
                    } else {
                        // If user creation fails, stop loading and show error.
                        showLoading(false);
                        handleRegistrationError(task);
                    }
                });
    }

    private void storeUserData(String username, String email) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            showLoading(false);
            showErrorDialog("Critical Error", "User was not found after creation.");
            return;
        }
        String userId = firebaseUser.getUid();
        DocumentReference docRef = fStore.collection("users").document(userId);

        Map<String, Object> newUser = new HashMap<>();
        newUser.put("username", username);
        newUser.put("email", email);

        docRef.set(newUser).addOnSuccessListener(aVoid -> {
            // --- REFINED UX ---
            // Only stop loading and navigate when data is successfully saved.
            showLoading(false);
            Log.d(TAG, "User profile created for " + userId);
            Toast.makeText(getApplicationContext(), "Account created successfully!", Toast.LENGTH_LONG).show();

            // Navigate to login screen
            Intent intent = new Intent(sign_upActivity.this, loginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();

        }).addOnFailureListener(e -> {
            // If storing data fails, show an error.
            showLoading(false);
            Log.e(TAG, "Firestore Error: ", e);
            showErrorDialog("Registration Error", "Your account was created, but we failed to save your profile. Please contact support.");
        });
    }

    // --- Other methods (initializeViews, setupClickListeners, etc.) remain the same ---
    // --- They are included here for completeness but have no changes. ---

    private void initializeViews() {
        tilUsername = findViewById(R.id.til_username);
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnSignup = findViewById(R.id.btn_signup);
        tvSignin = findViewById(R.id.tv_signin);

    }

    private void setupClickListeners() {
        btnSignup.setOnClickListener(v -> attemptRegistration());
        tvSignin.setOnClickListener(v -> {
            startActivity(new Intent(sign_upActivity.this, loginActivity.class));
            finish();
        });
    }

    private boolean isInputValid() {
        // Clear previous errors
        tilUsername.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        boolean isValid = true;

        if (Objects.requireNonNull(etUsername.getText()).toString().trim().isEmpty()) {
            tilUsername.setError("Username is required");
            isValid = false;
        }

        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("A valid email is required");
            isValid = false;
        }

        String password = Objects.requireNonNull(etPassword.getText()).toString();
        String confirmPassword = Objects.requireNonNull(etConfirmPassword.getText()).toString();

        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError("Please confirm your password");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            tilPassword.setError("Passwords do not match");
            tilConfirmPassword.setError("Passwords do not match");
            etPassword.getText().clear();
            etConfirmPassword.getText().clear();
            etPassword.requestFocus();
            isValid = false;
        }

        return isValid;
    }

    private void handleRegistrationError(Task<AuthResult> task) {
        String title = "Registration Failed";
        String message;

        try {
            throw Objects.requireNonNull(task.getException());
        } catch (FirebaseAuthUserCollisionException e) {
            message = "An account with this email address already exists.";
            tilEmail.setError(message);
            etEmail.requestFocus();
        } catch (Exception e) {
            message = "An unexpected error occurred. Please try again.";
            Log.e(TAG, "Registration Exception: ", e);
        }

        showErrorDialog(title, message);
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void showLoading(boolean isLoading) {
        if (progressBar == null) return;
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnSignup.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSignup.setEnabled(true);
        }
    }

    private void setupTextWatchers() {
        TextWatcher errorClearingWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // When user starts typing, clear the error on the corresponding layout
                if(tilUsername.getError() != null) tilUsername.setError(null);
                if(tilEmail.getError() != null) tilEmail.setError(null);
                if(tilPassword.getError() != null) tilPassword.setError(null);
                if(tilConfirmPassword.getError() != null) tilConfirmPassword.setError(null);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        etUsername.addTextChangedListener(errorClearingWatcher);
        etEmail.addTextChangedListener(errorClearingWatcher);
        etPassword.addTextChangedListener(errorClearingWatcher);
        etConfirmPassword.addTextChangedListener(errorClearingWatcher);
    }
}
