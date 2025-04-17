package com.example.smartbuspayment;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class ChargeBalance extends AppCompatActivity {

    private TextView balanceText;
    private EditText balanceEditText;
    private Button incrementBtn, decrementBtn, confirmBtn;
    private double currentBalance = 0.0;
    private FirebaseFirestore db;
    private String userId;
    private  Double totalBalance=0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_charge_balance);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //init views
        balanceText=findViewById(R.id.tv_balance);
        balanceEditText=findViewById(R.id.et_balance);
        incrementBtn=findViewById(R.id.btn_increment);
        decrementBtn=findViewById(R.id.btn_decrement);
        confirmBtn=findViewById(R.id.btn_confirm);


        //init firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        if(user!=null){
            userId = user.getUid();
            // Load user data
            loadUserData();
        }else {
            Toast.makeText(getApplicationContext(),"User not logged in",Toast.LENGTH_LONG).show();
        }

        // Increment button click listener
        incrementBtn.setOnClickListener(v -> {
            currentBalance += 1.0;

            updateBalanceDisplay();
        });

        // Decrement button click listener
        decrementBtn.setOnClickListener(v -> {
            if (currentBalance > 0) {
                currentBalance -= 1.0;
                updateBalanceDisplay();
            }else{
                Toast.makeText(this,"You can't discharge your balance",Toast.LENGTH_LONG).show();
            }
        });

        // Confirm charging
        confirmBtn.setOnClickListener(v -> {
            if(currentBalance>0)
                updateBalance();
            else
                Toast.makeText(getApplicationContext(),"You can't discharge your balance",Toast.LENGTH_SHORT).show();
        });
    }



    private void updateBalanceDisplay(){
        balanceEditText.setText(String.format("$%.2f",currentBalance));
    }

    private void loadUserData() {
        db.collection("agents").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Double firestoreBalance=document.getDouble("balance");
                            totalBalance=firestoreBalance!=null? firestoreBalance:0.0;
                            balanceText.setText(String.format("$%.2f", totalBalance));
                        } else {
                            Toast.makeText(getApplicationContext(),"User Data not found",Toast.LENGTH_LONG).show();
                        }
                    }else {
                        Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //uploading transaction for the updateBalance is missing
    private void updateBalance(){
        // Remove the $ symbol and parse the number
        String input = balanceEditText.getText().toString().replace("$", "").trim();

        double addedAmount = Double.parseDouble(input);
        totalBalance+=addedAmount;
        // Create a Map with the data to update
        Map<String, Object> userData = new HashMap<>();
        userData.put("balance", totalBalance);

        // In ChargeBalance.java's updateBalance() method:
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("amount", addedAmount);
        transaction.put("type", "charging");
        transaction.put("timestamp", FieldValue.serverTimestamp());

        DocumentReference userRef = db.collection("agents").document(userId);
        CollectionReference transactionsRef = userRef.collection("transactions");

        // Create batch write to ensure both operations succeed or fail together
        WriteBatch batch = db.batch();

        batch.update(userRef, userData);

        // Add transaction record to user's transactions subcollection
        DocumentReference newTransactionRef = transactionsRef.document();
        batch.set(newTransactionRef, transaction);

        // Update Firestore document
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Balance Charged Successfully!", Toast.LENGTH_SHORT).show();
                    currentBalance=0.0;
                    balanceEditText.setText("");
                    balanceText.setText(String.format("$%.2f",totalBalance));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error Charging Balance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error writing document", e);
                });

    }

}