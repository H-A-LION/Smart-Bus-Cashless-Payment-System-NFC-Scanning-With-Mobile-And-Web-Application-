package com.example.smartbuspayment;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TransactionsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TransactionsAdapter adapter;
    private List<Transaction> transactionList = new ArrayList<>();
    private FirebaseFirestore db;
    private String userId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_transactions);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Setup RecyclerView
        recyclerView = findViewById(R.id.transactions_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionsAdapter(transactionList);
        recyclerView.setAdapter(adapter);

        // Load transactions
        loadTransactions();
    }

    private void loadTransactions() {
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        transactionList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Transaction transaction = new Transaction(
                                    document.getDouble("amount"),
                                    document.getLong("timestamp"),
                                    document.getString("busId"),
                                    document.getString("userId")
                            );
                            transaction.setId(document.getId());
                            transactionList.add(transaction);
                        }
                        adapter.notifyDataSetChanged();
                    }else {
                        Toast.makeText(this, "Error loading transactions", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public static class Transaction {
        private String id;
        private double amount;
        private long timestamp;
        private String busId;
        private String userId; // Add this field

        // Empty constructor needed for Firestore
        public Transaction() {}

        // Constructor
        public Transaction(double amount, long timestamp, String busId, String userId) {
            this.amount = amount;
            this.timestamp = timestamp;
            this.busId = busId;
            this.userId = userId;
        }

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public String getBusId() {
            return busId;
        }

        public void setBusId(String busId) {
            this.busId = busId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }
}

