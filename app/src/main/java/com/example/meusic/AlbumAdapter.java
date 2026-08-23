package com.example.meusic;

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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {

    private final List<Album> albums;
    private final OnAlbumClickListener listener;

    public interface OnAlbumClickListener {
        void onAlbumClick(Album album);
    }

    public AlbumAdapter(List<Album> albums, OnAlbumClickListener listener) {
        this.albums = albums;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        Album album = albums.get(position);
        holder.albumName.setText(album.getName());
        holder.albumArtist.setText(album.getArtist());

        // Load Album Art (Basic implementation)
        // Note: For a grid, it's highly recommended to use an image library like Glide or Coil
        // to prevent lag. This is a simple synchronous way for now.
        if (!album.getSongs().isEmpty()) {
            loadAlbumArt(holder.albumArt, album.getSongs().get(0));
        }

        holder.itemView.setOnClickListener(v -> listener.onAlbumClick(album));
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    private void loadAlbumArt(ImageView imageView, Song song) {
        // Run on a background thread to prevent UI lag in the grid
        new Thread(() -> {
            try {
                Uri trackUri = android.content.ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.getId());
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(imageView.getContext(), trackUri);
                byte[] art = retriever.getEmbeddedPicture();
                if (art != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                    imageView.post(() -> imageView.setImageBitmap(bitmap));
                }
                retriever.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ImageView albumArt;
        TextView albumName, albumArtist;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            albumArt = itemView.findViewById(R.id.album_art);
            albumName = itemView.findViewById(R.id.album_name);
            albumArtist = itemView.findViewById(R.id.album_artist);
        }
    }
}