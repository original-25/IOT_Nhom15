package com.example.smarthome.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthome.R;
import com.example.smarthome.model.response.HomeResponse;

import org.jspecify.annotations.NonNull;

import java.util.List;

public class InvitationAdapter extends RecyclerView.Adapter<InvitationAdapter.ViewHolder> {
    private List<HomeResponse.InvitationData> list;
    private OnInvitationClickListener listener;

    public interface OnInvitationClickListener {
        void onAccept(HomeResponse.InvitationData invitation);
        void onDecline(HomeResponse.InvitationData invitation);
    }

    public InvitationAdapter(List<HomeResponse.InvitationData> list, OnInvitationClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invitation, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HomeResponse.InvitationData item = list.get(position);
        holder.textHome.setText("Lời mời vào: " + item.getHome().getName());
        holder.textFrom.setText("Từ: " + item.getInviter().getUsername());

        holder.btnAccept.setOnClickListener(v -> listener.onAccept(item));
        holder.btnDecline.setOnClickListener(v -> listener.onDecline(item));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textHome, textFrom;
        Button btnAccept, btnDecline;
        ViewHolder(View v) {
            super(v);
            textHome = v.findViewById(R.id.text_invitation_home_name);
            textFrom = v.findViewById(R.id.text_invitation_from);
            btnAccept = v.findViewById(R.id.button_accept_invite);
            btnDecline = v.findViewById(R.id.button_decline_invite);
        }
    }
}
