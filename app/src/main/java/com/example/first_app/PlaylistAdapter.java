package com.example.first_app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private final List<PlaylistEntity> playlists;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(PlaylistEntity playlist);
    }

    private final OnPlaylistClickListener listener;

    private Context appContext;

    public PlaylistAdapter(List<PlaylistEntity> playlists, OnPlaylistClickListener listener, Context context) {
        this.playlists = playlists;
        this.listener = listener;
        this.appContext = context;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        PlaylistEntity playlist = playlists.get(position);
        holder.playlistName.setText(playlist.name);

        // Handle Item Click (Open the playlist)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaylistClick(playlist);
            }
        });


        loadFirstSongAlbumArt(holder.playlistCover, playlist.playlistId);

        // Handle Delete Click
        holder.deleteBtn.setOnClickListener(v -> {
            showDeleteConfirmation(v.getContext(), playlist, holder.getAdapterPosition());
        });
    }

    private void loadFirstSongAlbumArt(ImageView imageView, int playlistId) {
        // Set a default icon first
        // because of the recycler view behavior
        imageView.setImageResource(R.drawable.ic_menu);

        // Run on background thread to avoid UI lag
        new Thread(() -> {
            try {
                // 1. Get the first song ID from the playlist
                Long firstSongId = PlaylistManager.getInstance(appContext).getFirstSongIdInPlaylist(playlistId);

                if (firstSongId != null) {
                        Uri trackUri = android.content.ContentUris.withAppendedId(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, firstSongId);

                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        retriever.setDataSource(appContext, trackUri);
                        byte[] art = retriever.getEmbeddedPicture();

                        if (art != null) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                            // Post to main thread to update ImageView
                            imageView.post(() -> imageView.setImageBitmap(bitmap));
                        }
                        retriever.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    private void showDeleteConfirmation(Context context, PlaylistEntity playlist, int position) {
        new ConfirmationDialog.Builder(context).setMessage("Are you sure you want to delete '" + playlist.name + "'?").setCallback(new ConfirmationDialog.Callback() {
            @Override
            public void onConfirm() {
                PlaylistManager.getInstance(context).deletePlaylist(playlist.playlistId);
                Toast.makeText(context, "Playlist deleted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {

            }
        }).build().show();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        TextView playlistName;
        ImageView deleteBtn, playlistCover;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            playlistName = itemView.findViewById(R.id.playlist_name);
            deleteBtn = itemView.findViewById(R.id.delete_btn);
            playlistCover = itemView.findViewById(R.id.playlist_icon);
        }
    }
}