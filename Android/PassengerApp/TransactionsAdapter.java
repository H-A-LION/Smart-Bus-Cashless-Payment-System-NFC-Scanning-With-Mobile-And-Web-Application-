package com.example.smartbuspayment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.ViewHolder> {
    private final List<TransactionsActivity.Transaction> transactions;

    public TransactionsAdapter(List<TransactionsActivity.Transaction> transactions) {
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionsActivity.Transaction transaction = transactions.get(position);

        // Set amount with color based on type
        holder.tvAmount.setText(String.format("%.2f", transaction.getAmount()));
        if ("charging".equals(transaction.getType())) {
            holder.tvAmount.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_green_dark));
            holder.tvAmount.setText(String.format("+%.2f", transaction.getAmount()));
        } else {
            holder.tvAmount.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_red_dark));
            holder.tvAmount.setText(String.format("-%.2f", transaction.getAmount()));
        }

        // Format date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        holder.tvDate.setText(sdf.format(transaction.getTimestamp()));

        // Set transaction details
        if ("payment".equals(transaction.getType())) {
            holder.tvDetails.setText(String.format("Bus %s • %s",
                    transaction.getBusId(),
                    transaction.getMethod()));
        } else {
            holder.tvDetails.setText("Balance Top-up");
        }

        // Set transaction type icon/text
        holder.tvType.setText(transaction.getType().equals("charging") ? "↗" : "↘");
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAmount, tvDate, tvDetails,tvType;

        public ViewHolder(View itemView) {
            super(itemView);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvDetails = itemView.findViewById(R.id.tv_details);
            tvType=itemView.findViewById(R.id.tv_type);
        }
    }
}

