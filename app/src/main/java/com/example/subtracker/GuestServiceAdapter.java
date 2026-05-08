package com.example.subtracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class GuestServiceAdapter extends RecyclerView.Adapter<GuestServiceAdapter.ViewHolder> {

    private List<GuestCalculatorActivity.GuestService> items;
    private OnDeleteListener deleteListener;

    public interface OnDeleteListener {
        void onDelete(int position);
    }

    public GuestServiceAdapter(List<GuestCalculatorActivity.GuestService> items, OnDeleteListener deleteListener) {
        this.items = items;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_guest_service, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GuestCalculatorActivity.GuestService item = items.get(position);
        holder.tvIcon.setText(item.icon);
        holder.tvName.setText(item.name);
        holder.tvDate.setText("с " + item.startDate);
        holder.tvAmount.setText(String.format("%.0f", item.amount) + " ₽");
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvName, tvDate, tvAmount;
        ImageView btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvName = itemView.findViewById(R.id.tvName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}