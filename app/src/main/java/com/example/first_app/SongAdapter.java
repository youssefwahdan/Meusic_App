package com.example.first_app;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songList;

    private OnSongClickListener listener; // 1. Add listener

    // 2. Define the Interface
    public interface OnSongClickListener {
        void onSongClick(Song song, int position);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(Song song, int position);
    }

    private OnItemLongClickListener longClickListener;

    public SongAdapter(List<Song> songList, OnSongClickListener listener) {
        this.songList = songList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songList.get(position);

        holder.title.setText(song.getTitle());
        holder.artist.setText(song.getArtist());
        holder.album.setText(song.getAlbum());

        // Format duration from milliseconds to MM:SS
        holder.duration.setText(formatDuration(song.getDuration()));

        if (position == songList.size() - 1) {
            holder.divider.setVisibility(View.GONE);
//            holder.itemView.setPadding(holder.itemView.getPaddingLeft(), holder.itemView.getPaddingTop(), holder.itemView.getPaddingRight(), 400);
        }

        // 4. Set the click listener on the whole row
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onSongClick(songList.get(pos), pos);
                }
            }
        });

        holder.optionsDots.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOptionsDialog(song, v.getContext());
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                return longClickListener.onItemLongClick(song, holder.getAdapterPosition());
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }


    // 1. The Main Options Dialog
    private void showOptionsDialog(Song song, Context context) {

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_song_options,null);

        View addToFavOption = dialogView.findViewById(R.id.favourite_option);
        View infoOption = dialogView.findViewById(R.id.song_info_option);

        ImageView favouriteImage = dialogView.findViewById(R.id.favourite_icon);

        FavoriteManager.getInstance(context)
                .getFavoriteStatus(song.getId())
                .observeForever(isFav -> {
                    // isFav is 1 if favorite, 0 if not
                    if (isFav != null && isFav == 1) {
                        favouriteImage.setImageResource(R.drawable.ic_favorite);
                    } else {
                        favouriteImage.setImageResource(R.drawable.ic_favorite_border);
                    }
                });

        addToFavOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FavoriteManager.getInstance(v.getContext()).toggleFavorite(song.getId());
            }
        });

        infoOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSongDetailsDialog(song, context);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(context).setView(dialogView);

        AlertDialog dialog = builder.create();

        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().getAttributes().y = 50;

        dialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_dialog_bg);

        dialog.show();
        // 1. Get the screen width
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;

        // 2. Calculate the dialog width (e.g., 0.90 = 90% of the screen)
        int dialogWidth = (int) (screenWidth * 0.9);

        // 3. Apply the width and set height to wrap content
        dialog.getWindow().setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    // 2. The Song Details Dialog
    private void showSongDetailsDialog(Song song, Context context) {
        // Inflate the custom layout
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_song_details, null);

        // Find views and set data
        TextView titleText = dialogView.findViewById(R.id.detail_title);
        TextView artistText = dialogView.findViewById(R.id.detail_artist);
        TextView albumText = dialogView.findViewById(R.id.detail_album);
        TextView durationText = dialogView.findViewById(R.id.detail_duration);
        TextView pathText = dialogView.findViewById(R.id.detail_path);

        titleText.setText(song.getTitle() != null ? song.getTitle() : "Unknown");
        artistText.setText(song.getArtist() != null ? song.getArtist() : "Unknown Artist");
        albumText.setText(song.getAlbum() != null ? song.getAlbum() : "Unknown Album");

        // Format duration from milliseconds to MM:SS
        durationText.setText(formatDuration(song.getDuration()));

        // Show file path (truncate if too long if you want, but ScrollView handles it)
        pathText.setText(song.getData() != null ? song.getData() : "Unknown Path");

        // Build and show the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle("Song Details")
                .setView(dialogView);
//                .setPositiveButton("Close", null);

        AlertDialog dialog = builder.create();

        dialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_dialog_bg);

        dialog.show();

        // 1. Get the screen width
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;

        // 2. Calculate the dialog width (e.g., 0.90 = 90% of the screen)
        int dialogWidth = (int) (screenWidth * 0.9);

        // 3. Apply the width and set height to wrap content
        dialog.getWindow().setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    // 3. Helper to format milliseconds into MM:SS
    private String formatDuration(long durationMs) {
        long totalSeconds = durationMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView title, artist, duration, album;
        ImageView optionsDots;
        View divider;
        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.song_title);
            artist = itemView.findViewById(R.id.song_artist);
            album = itemView.findViewById(R.id.album_name);
            duration = itemView.findViewById(R.id.song_duration);
            divider = itemView.findViewById(R.id.song_divider);
            optionsDots = itemView.findViewById(R.id.two_dots_option);
        }
    }
}