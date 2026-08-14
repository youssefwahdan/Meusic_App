package com.example.first_app;

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

public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<SearchItem> items;
    private final OnSearchItemClickListener listener;

    public interface OnSearchItemClickListener {
        void onItemClick(SearchItem item);
    }

    public SearchAdapter(List<SearchItem> items, OnSearchItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == SearchItem.TYPE_ARTIST) {
            return new ArtistViewHolder(inflater.inflate(R.layout.item_search_artist, parent, false));
        } else if (viewType == SearchItem.TYPE_ALBUM) {
            return new AlbumViewHolder(inflater.inflate(R.layout.item_search_album, parent, false));
        } else {
            // Reuse your existing item_song layout
            return new SongViewHolder(inflater.inflate(R.layout.item_song, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SearchItem item = items.get(position);

        if (holder instanceof ArtistViewHolder) {
            ((ArtistViewHolder) holder).name.setText(item.title);
            ((ArtistViewHolder) holder).songs.setText(item.subtitle);
        } else if (holder instanceof AlbumViewHolder) {

            ((AlbumViewHolder) holder).name.setText(item.title);
            ((AlbumViewHolder) holder).artist.setText(item.subtitle);
            if (!item.getSongs().isEmpty()) {
                loadAlbumArt(((AlbumViewHolder) holder).albumArt, item.getSongs().get(0));
            }
        } else if (holder instanceof SongViewHolder) {
            // Bind your song data here (title, artist, etc.)
             ((SongViewHolder) holder).title.setText(item.title);
             ((SongViewHolder) holder).artist.setText(item.subtitle);
             ((SongViewHolder) holder).album.setText(item.subtitle);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
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

    @Override
    public int getItemCount() { return items.size(); }

    // ViewHolders
    static class ArtistViewHolder extends RecyclerView.ViewHolder {
        TextView name, songs;
        ArtistViewHolder(View v) { super(v); name = v.findViewById(R.id.search_artist_name); songs = v.findViewById(R.id.search_artist_songs); }
    }
    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ImageView albumArt;
        TextView name, artist;
        AlbumViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.search_album_name);
            artist = v.findViewById(R.id.search_album_artist);
            albumArt = v.findViewById(R.id.search_album_art);
        }
    }
    static class SongViewHolder extends RecyclerView.ViewHolder {
         TextView title, artist, album;
        SongViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.song_title);
            artist = v.findViewById(R.id.song_artist);
            album = v.findViewById(R.id.album_name);
        }
    }
}