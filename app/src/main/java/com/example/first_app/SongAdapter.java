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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songList;

    private SongOptionAdapter optionAdapter;

    private final List<SongOptionItem> songOptions = new ArrayList<>();

    private OnSongClickListener listener; // 1. Add listener

    // 2. Define the Interface
    public interface OnSongClickListener {
        void onSongClick(Song song, int position);
    }

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
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }


    // 1. The Main Options Dialog
    private void showOptionsDialog(Song song, Context context) {

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_song_options,null);
        RecyclerView songOptionsRecyclerView = dialogView.findViewById(R.id.song_options_recycler);
        songOptionsRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        FavoriteManager.getInstance(context)
                .getFavoriteStatus(song.getId())
                .observeForever(isFav -> {
                    // isFav is 1 if favorite, 0 if not
                    if (isFav != null && isFav == 1) {
                        songOptions.get(0).setIconResId(R.drawable.ic_favorite);
                        optionAdapter.notifyItemChanged(0);
                    } else {
                        songOptions.get(0).setIconResId(R.drawable.ic_favorite_border);
                        optionAdapter.notifyItemChanged(0);
                    }
                });

        addSongOptions();
        setupSongOptionAdapter(context, song, songOptionsRecyclerView);

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


    private void addSongOptions() {
        songOptions.clear();
        songOptions.add(new SongOptionItem(R.drawable.ic_favorite_border, "Add to favourite"));
        songOptions.add(new SongOptionItem(R.drawable.ic_playlist_add, "Add to playlist"));
        songOptions.add(new SongOptionItem(R.drawable.ic_info_outline, "Song info."));
    }
    private void setupSongOptionAdapter(Context context, Song song, RecyclerView songOptionsRecyclerView) {
        optionAdapter = new SongOptionAdapter(songOptions, new SongOptionAdapter.OnSongOptionItemClickListener() {
            @Override
            public void onSongOptionClick(SongOptionItem item, int position) {
                handleOptionsClick(context, song, item, position);
            }
        });
        songOptionsRecyclerView.setAdapter(optionAdapter);


    }

    private void handleOptionsClick(Context context, Song song, SongOptionItem item, int position) {
        switch (position) {
            case 0:
                FavoriteManager.getInstance(context).toggleFavorite(song.getId());
                break;
            case 1:
                showAddToPlaylistDialog(context, song);
                break;
            case 2:
                showSongDetailsDialog(context, song);
                break;
        }
    }
    private void showAddToPlaylistDialog(Context context, Song song) {
        // 1. Fetch all playlists from the database
        LiveData<List<PlaylistEntity>> playlistsLiveData = PlaylistManager.getInstance(context).getAllPlaylists();


        Observer<List<PlaylistEntity>> observer = new Observer<List<PlaylistEntity>>() {
            @Override
            public void onChanged(List<PlaylistEntity> playlists) {
                playlistsLiveData.removeObserver(this);

                if (playlists == null || playlists.isEmpty()) {
                    Toast.makeText(context, "No playlists found. Create one first!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 2. Extract just the names for the dialog
                String[] playlistNames = new String[playlists.size()];
                int[] playlistIds = new int[playlists.size()];
                for (int i = 0; i < playlists.size(); i++) {
                    playlistNames[i] = playlists.get(i).name;
                    playlistIds[i] = playlists.get(i).playlistId;
                }

                // 3. Show the dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(context)
                        .setTitle("Add to Playlist")
                        .setItems(playlistNames, (dialog, which) -> {
                            int selectedPlaylistId = playlistIds[which];

                            // Use the new safe method
                            PlaylistManager.getInstance(context).addSongToPlaylistSafe(
                                    selectedPlaylistId,
                                    song.getId(),
                                    new PlaylistManager.AddSongCallback() {
                                        @Override
                                        public void onResult(boolean success, String message) {
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                            );
                        });
                AlertDialog dialog = builder.create();

                dialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_dialog_bg);

                int screenWidth = context.getResources().getDisplayMetrics().widthPixels;

                int dialogWidth = (int) (screenWidth * 0.90);

                // 3. Apply the width and set height to wrap content
                dialog.getWindow().setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

                dialog.show();
            }
        };
        // We use observeForever here just to get the data once for the dialog
        playlistsLiveData.observeForever(observer);
    }

    private void showSongDetailsDialog(Context context, Song song) {
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


    // 1. Add this method to handle the UI shift and animation
    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition == toPosition) return;

        // Remove the item from the old position and insert it at the new position
        Song movedItem = songList.remove(fromPosition);
        songList.add(toPosition, movedItem);

        // Notify the RecyclerView to animate the move
        notifyItemMoved(fromPosition, toPosition);
    }

    // 2. Add this getter so the Activity can read the new order after dragging
    public List<Song> getCurrentSongs() {
        return songList;
    }
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