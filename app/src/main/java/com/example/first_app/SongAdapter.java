package com.example.first_app;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    // Helper method to convert milliseconds to "MM:SS"
    private String formatDuration(long durationMs) {
        long seconds = (durationMs / 1000) % 60;
        long minutes = (durationMs / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView title, artist, duration, album;
        View divider;
        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.song_title);
            artist = itemView.findViewById(R.id.song_artist);
            album = itemView.findViewById(R.id.album_name);
            duration = itemView.findViewById(R.id.song_duration);
            divider = itemView.findViewById(R.id.song_divider);

        }
    }
}