package com.example.vendorconnect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ClientRegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText editTextFullName, editTextPhone, editTextEmail, editTextPassword, editTextConfirmPassword;
    private Button buttonRegister;
    private TextView loginTextView;
    private Spinner userTypeSpinner;
    private String selectedUserType = "Customer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_register);
        
        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Initialize views
        editTextFullName = findViewById(R.id.editTextFullName);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        loginTextView = findViewById(R.id.loginTextView);
        userTypeSpinner = findViewById(R.id.userTypeSpinner);
        
        // Setup user type spinner
        setupUserTypeSpinner();
        
        // Set click listeners
        buttonRegister.setOnClickListener(v -> registerUser());
        loginTextView.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
    
    private void setupUserTypeSpinner() {
        String[] userTypes = {"Customer", "Vendor"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, userTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        userTypeSpinner.setAdapter(adapter);
        
        userTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                selectedUserType = userTypes[position];
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedUserType = "Customer";
            }
        });
    }
    
    private void registerUser() {
        String fullName = editTextFullName.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();
        
        // Validate required fields
        if (fullName.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields (*)", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate phone number (basic check)
        if (phone.length() < 10) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate email format (only if provided)
        if (!email.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address or leave it empty", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate password
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate password confirmation
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create email from phone number if no email provided
        // For testing, let's use a real email format
        String loginEmail = email.isEmpty() ? phone + "@test.com" : email;
        
        // Show loading state
        buttonRegister.setEnabled(false);
        buttonRegister.setText("Registering...");
        
        // Test Firebase connection first
        if (auth == null) {
            Toast.makeText(this, "Firebase not initialized", Toast.LENGTH_SHORT).show();
            buttonRegister.setEnabled(true);
            buttonRegister.setText("Register");
            return;
        }
        
        
        auth.createUserWithEmailAndPassword(loginEmail, password)
            .addOnCompleteListener(this, task -> {
                buttonRegister.setEnabled(true);
                buttonRegister.setText("Register");
                
                if (task.isSuccessful()) {
                    // Save user data to Firestore
                    saveUserDataToFirestore(fullName, phone, email, selectedUserType);
                    
                    // Sign out the user immediately after registration
                    auth.signOut();
                    
                    Toast.makeText(this, "Registration successful! Please login.", Toast.LENGTH_LONG).show();
                    
                    // Navigate to LoginActivity
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMessage = "Registration failed: ";
                    if (task.getException() != null) {
                        errorMessage += task.getException().getMessage();
                    } else {
                        errorMessage += "Unknown error";
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void saveUserDataToFirestore(String fullName, String phone, String email, String userType) {
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            Map<String, Object> userData = new HashMap<>();
            userData.put("fullName", fullName);
            userData.put("phone", phone);
            userData.put("email", email);
            userData.put("userType", userType);
            userData.put("createdAt", System.currentTimeMillis());
            
            if (userType.equals("Vendor")) {
                userData.put("latitude", 0.0);
                userData.put("longitude", 0.0);
                userData.put("isLocationSet", false);
            }
            
            db.collection("users").document(userId).set(userData)
                .addOnSuccessListener(aVoid -> {
                    // User data saved successfully
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        }
    }
}
