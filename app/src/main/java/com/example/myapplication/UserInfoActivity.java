package com.example.myapplication;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserInfoActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView tvUsernameValue, tvEmailValue;
    private Button btnEditInfo, btnLogout;
    private ImageButton backButton;
    private ImageView profileImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        tvUsernameValue = findViewById(R.id.tv_username_value);
        tvEmailValue = findViewById(R.id.tv_email_value);
        btnEditInfo = findViewById(R.id.btn_edit_info);
        btnLogout = findViewById(R.id.logoutButton);
        backButton = findViewById(R.id.backButton);
        profileImageView = findViewById(R.id.profile_image);

        // Set up click listeners
        setupClickListeners();
    }

    /**
     * onResume is called when the activity comes into the foreground.
     * We reload user info here to reflect any changes made on the EditInfoActivity.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadUserInfo();
    }

    private void loadUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Set the email
            tvEmailValue.setText(currentUser.getEmail());

            // Set the username, with a fallback if DisplayName is not set
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                tvUsernameValue.setText(displayName);
            } else {
                String email = currentUser.getEmail();
                if (email != null && email.contains("@")) {
                    tvUsernameValue.setText(email.split("@")[0]);
                } else {
                    tvUsernameValue.setText("No Username");
                }
            }

            // Use Glide to load the profile picture from the user's photo URL
            if (currentUser.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .placeholder(R.drawable.pro)
                        .into(profileImageView);
            } else {
                profileImageView.setImageResource(R.drawable.pro);
            }

        } else {
            Toast.makeText(this, "User not found. Please log in again.", Toast.LENGTH_LONG).show();
            navigateToLogin();
        }
    }

    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());

        // Edit Info button
        btnEditInfo.setOnClickListener(v -> {
            startActivity(new Intent(UserInfoActivity.this, EditInfoActivity.class));
        });

        // Logout button - Show confirmation dialog
        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    /**
     * Shows a custom logout confirmation dialog
     */
    private void showLogoutConfirmationDialog() {
        // Create custom dialog
        Dialog logoutDialog = new Dialog(this);
        logoutDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        logoutDialog.setContentView(R.layout.logout_confirmation_dialog);
        logoutDialog.setCancelable(true);

        // Make dialog background transparent to show rounded corners
        if (logoutDialog.getWindow() != null) {
            logoutDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Load user's profile image in the dialog
        ImageView dialogProfileImage = logoutDialog.findViewById(R.id.dialog_profile_image);
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null && currentUser.getPhotoUrl() != null) {
            // Load user's profile image using Glide
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .placeholder(R.drawable.pro)
                    .into(dialogProfileImage);
        } else {
            // Show default pro.png image
            dialogProfileImage.setImageResource(R.drawable.pro);
        }

        // Get dialog buttons
        Button btnOk = logoutDialog.findViewById(R.id.btn_dialog_ok);
        Button btnCancel = logoutDialog.findViewById(R.id.btn_dialog_cancel);

        // OK button - Proceed with logout
        btnOk.setOnClickListener(v -> {
            logoutDialog.dismiss();
            performLogout();
        });

        // Cancel button - Close dialog
        btnCancel.setOnClickListener(v -> logoutDialog.dismiss());

        // Show the dialog
        logoutDialog.show();
    }

    /**
     * Performs the actual logout operation
     */
    private void performLogout() {
        mAuth.signOut();
        Toast.makeText(this, "You have been logged out.", Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(UserInfoActivity.this, loginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}