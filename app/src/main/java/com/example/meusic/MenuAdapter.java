package com.example.meusic;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meusic.R;

import java.util.List;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {

    private List<MenuItem> menuItems;
    private OnItemClickListener clickListener;

    // 2. Define the Interface
    public interface OnItemClickListener {
        void onItemClick(MenuItem item, int position);
    }
    public MenuAdapter(List<MenuItem> menuItems, OnItemClickListener listener) {
        this.menuItems = menuItems;
        this.clickListener = listener;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuItem item = menuItems.get(position);

        // Set Icon and tint it red (matching your image)
        holder.icon.setImageResource(item.getIconResId());
        holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.brand_red)); // Define this color in colors.xml

        // Set Texts
        holder.title.setText(item.getTitle());

        // Handle Subtitle (Hide it if it's empty, like for Toolbox/Settings)
        if (item.getSubtitle().isEmpty()) {
            holder.subtitle.setVisibility(View.GONE);
        } else {
            holder.subtitle.setVisibility(View.VISIBLE);
            holder.subtitle.setText(item.getSubtitle());
        }

        // Handle Divider (Hide it for the last item or specific items)
        // In your image, there is no divider between Toolbox and Settings.
//        if (position == menuItems.size() - 1 || position == menuItems.size() - 2) {
//            holder.divider.setVisibility(View.GONE);
//        } else {
//            holder.divider.setVisibility(View.VISIBLE);
//        }
        if (position == menuItems.size() - 1) {
            holder.divider.setVisibility(View.GONE);

        }
//        holder.itemView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                view.setBackgroundColor(R.color.secondaryBackground);
//            }
//        });
        // 4. SET THE CLICK LISTENER HERE
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                int pos = holder.getAdapterPosition();
                // Check if position is valid (prevents crashes if item was deleted)
                if (pos != RecyclerView.NO_POSITION) {

                    clickListener.onItemClick(menuItems.get(pos), pos);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    public void updateSongCount(int count) {
        // Assuming "All songs" is the first item (index 0) in your list
        if (!menuItems.isEmpty()) {
            menuItems.get(0).setSubtitle(count + " songs");
            notifyItemChanged(0); // Refresh only the first item
        }
    }

    public void updateFavouritesCount(int count) {
        if (!menuItems.isEmpty()) {
            menuItems.get(2).setSubtitle(count + " songs");
            notifyItemChanged(2); // Refresh only the first item
        }
    }
    public void updatePlaylistsCount(int count) {
        if (!menuItems.isEmpty()) {
            menuItems.get(1).setSubtitle(count + " playlists");
            notifyItemChanged(1); // Refresh only the first item
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title, subtitle;
        View divider;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.item_icon);
            title = itemView.findViewById(R.id.item_title);
            subtitle = itemView.findViewById(R.id.item_subtitle);
            divider = itemView.findViewById(R.id.item_divider);
        }
    }
}
