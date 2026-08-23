package com.example.meusic;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SongOptionAdapter extends RecyclerView.Adapter<SongOptionAdapter.SongOptionItemViewHolder>{


    private List<SongOptionItem> songOptions;

    public interface OnSongOptionItemClickListener {
        void onSongOptionClick(SongOptionItem item, int position);
    }
    private final SongOptionAdapter.OnSongOptionItemClickListener listener;


    public SongOptionAdapter(List<SongOptionItem> songOptions, OnSongOptionItemClickListener listener) {
        this.songOptions = songOptions;
        this.listener = listener;
    }
    @NonNull
    @Override
    public SongOptionItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_option_item, parent, false);
        return new SongOptionItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongOptionItemViewHolder holder, int position) {
        SongOptionItem item = songOptions.get(position);
        holder.icon.setImageResource(item.getIconResId());
        holder.title.setText(item.getTitle());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onSongOptionClick(item, position);
                    }
                }
            }
        });

        if (position == songOptions.size() - 1) {
            holder.divider.setVisibility(View.GONE);
        }

    }

    @Override
    public int getItemCount() {
        return songOptions.size();
    }

    static class SongOptionItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView icon;
        private TextView title;
        private View divider;
        public SongOptionItemViewHolder(@NonNull View itemView) {
            super(itemView);
            this.icon = itemView.findViewById(R.id.option_icon);
            this.title = itemView.findViewById(R.id.option_txt);
            this.divider = itemView.findViewById(R.id.option_divider);
        }
    }
}
