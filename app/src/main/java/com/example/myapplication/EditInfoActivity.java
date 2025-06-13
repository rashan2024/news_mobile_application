package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditInfoActivity extends AppCompatActivity {

    private EditText etUsername, etEmail;
    private Button btnOk, btnCancel;
    private ShapeableImageView profileImageView;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private Uri newImageUri = null;
    public static final String TAG = "EditInfoActivity";

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    newImageUri = result.getData().getData();
                    profileImageView.setImageURI(newImageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_info);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initializeViews();
        loadCurrentUserInfo();

        btnCancel.setOnClickListener(v -> finish());
        btnOk.setOnClickListener(v -> saveUserInfo());
        profileImageView.setOnClickListener(v -> openImagePicker());
    }

    private void initializeViews() {
        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        btnOk = findViewById(R.id.btn_ok);
        btnCancel = findViewById(R.id.btn_cancel);
        profileImageView = findViewById(R.id.edit_profile_image);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void loadCurrentUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            etUsername.setText(user.getDisplayName());
            etEmail.setText(user.getEmail());
            if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).placeholder(R.drawable.pro).into(profileImageView);
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void saveUserInfo() {
        String newUsername = etUsername.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();

        if (newUsername.isEmpty()) {
            etUsername.setError("Username cannot be empty");
            etUsername.requestFocus();
            return;
        }

        if (newEmail.isEmpty()) {
            etEmail.setError("Email cannot be empty");
            etEmail.requestFocus();
            return;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        if (newImageUri != null) {
            uploadImageAndUpdateProfile(newUsername, newEmail);
        } else {
            updateProfile(newUsername, newEmail, null);
        }
    }

    private void uploadImageAndUpdateProfile(String username, String email) {
        StorageReference profilePicRef = storage.getReference().child("profile_pictures/" + UUID.randomUUID().toString());
        profilePicRef.putFile(newImageUri)
                .addOnSuccessListener(taskSnapshot -> profilePicRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> updateProfile(username, email, uri)))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    showErrorDialog("Upload Failed", "Could not upload the profile picture. Please try again.");
                });
    }

    private void updateProfile(String newUsername, String newEmail, Uri newPhotoUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        UserProfileChangeRequest.Builder profileUpdatesBuilder = new UserProfileChangeRequest.Builder()
                .setDisplayName(newUsername);

        if (newPhotoUrl != null) {
            profileUpdatesBuilder.setPhotoUri(newPhotoUrl);
        }
        UserProfileChangeRequest profileUpdates = profileUpdatesBuilder.build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "User profile name/photo updated in Auth.");
                updateUserEmail(user, newUsername, newEmail, newPhotoUrl);
            } else {
                progressBar.setVisibility(View.GONE);
                showErrorDialog("Update Failed", "Your profile could not be updated at this time.");
            }
        });
    }

    private void updateUserEmail(FirebaseUser user, String username, String newEmail, Uri photoUrl) {
        if (newEmail.equals(user.getEmail())) {
            updateFirestoreUser(user.getUid(), username, newEmail, photoUrl);
            return;
        }

        user.updateEmail(newEmail).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "User email address updated in Auth.");
                updateFirestoreUser(user.getUid(), username, newEmail, photoUrl);
            } else {
                progressBar.setVisibility(View.GONE);
                try {
                    throw task.getException();
                } catch (FirebaseAuthUserCollisionException e) {
                    showErrorDialog("Email Update Failed", "This email address is already in use by another account.");
                } catch (FirebaseAuthRecentLoginRequiredException e) {
                    showReauthDialog();
                } catch (Exception e) {
                    Log.e(TAG, "updateEmail:failure", e);
                    // --- THIS IS THE DEBUGGING CHANGE ---
                    // It will show the real error message from Firebase in the dialog
                    String detailedErrorMessage = e.getMessage();
                    showErrorDialog("Email Update Failed (DEBUG)", "Firebase returned an error:\n\n" + detailedErrorMessage);
                    // --- END OF CHANGE ---
                }
            }
        });
    }

    private void updateFirestoreUser(String uid, String username, String email, Uri photoUrl) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("email", email);

        String photoUrlString = (photoUrl != null) ? photoUrl.toString() : (mAuth.getCurrentUser().getPhotoUrl() != null ? mAuth.getCurrentUser().getPhotoUrl().toString() : "");
        userData.put("photoUrl", photoUrlString);

        db.collection("users").document(uid).update(userData)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(EditInfoActivity.this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    showErrorDialog("Database Error", "Your profile was authenticated, but could not be saved to the database. Please check your Firestore rules.");
                    Log.e(TAG, "Firestore update failed: ", e);
                });
    }

    private void showReauthDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Session Expired")
                .setMessage("For your security, please log in again to change your email address.")
                .setPositiveButton("Log In", (dialog, which) -> {
                    Intent intent = new Intent(EditInfoActivity.this, loginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}