package com.example.meusic.adapters;

import com.example.meusic.R;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meusic.database.entities.PlaylistEntity;
import com.example.meusic.managers.FavoriteManager;
import com.example.meusic.managers.PlaylistManager;
import com.example.meusic.models.SearchItem;
import com.example.meusic.models.Song;
import com.example.meusic.models.SongOptionItem;

import java.util.ArrayList;
import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<SearchItem> items;
    private SongOptionAdapter optionAdapter;
    private final List<SongOptionItem> songOptions = new ArrayList<>();

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
            Song song = (Song) item.data;
            // Bind your song data here (title, artist, etc.)
             ((SongViewHolder) holder).title.setText(item.title);
             ((SongViewHolder) holder).artist.setText(item.subtitle);
             ((SongViewHolder) holder).album.setText(item.subtitle);
            ((SongViewHolder) holder).optionsDots.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    InputMethodManager imm = (InputMethodManager) v.getContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);

                    // Hide keyboard using the RecyclerView's window token
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }

                    showOptionsDialog(song, v.getContext());
                }
            });
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
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

    private String formatDuration(long durationMs) {
        long totalSeconds = durationMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
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
         ImageView optionsDots;
        SongViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.song_title);
            artist = v.findViewById(R.id.song_artist);
            album = v.findViewById(R.id.album_name);
            optionsDots = v.findViewById(R.id.two_dots_option);
        }
    }
}