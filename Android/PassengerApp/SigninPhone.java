package com.example.smartbuspayment;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class SigninPhone extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private EditText etPhone, etOtp;
    private Button btnSendOtp, btnVerify;
    private Switch switchEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signin_phone);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth=FirebaseAuth.getInstance();
        etPhone = findViewById(R.id.et_phone);
        etOtp = findViewById(R.id.et_otp);
        btnSendOtp = findViewById(R.id.btn_send_otp);
        btnVerify = findViewById(R.id.btn_verify);
        switchEmail=findViewById(R.id.switch_to_phone);

        switchEmail.setOnClickListener(v -> {
            if (switchEmail.isChecked()){
                startActivity(new Intent(getApplicationContext(),MainActivity.class));
            }
        });

        btnSendOtp.setOnClickListener(v -> {
            String phone = "+" + etPhone.getText().toString().trim();
            if (phone.isEmpty()) {
                etPhone.setError("Enter phone number");
                return;
            }
            startPhoneNumberVerification(phone);
        });

        btnVerify.setOnClickListener(v -> {
            String code = etOtp.getText().toString().trim();
            if (code.isEmpty()) {
                etOtp.setError("Enter OTP");
                return;
            }
            verifyPhoneNumberWithCode(mVerificationId, code);
        });
    }
    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNumber, 60, TimeUnit.SECONDS, this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(getApplicationContext(), "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        mVerificationId = verificationId;
                        mResendToken = token;
                        Toast.makeText(getApplicationContext(), "OTP sent", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Success - update UI or move to next activity
                        startActivity(new Intent(getApplicationContext(), MainActivity2.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Authentication failed: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

}