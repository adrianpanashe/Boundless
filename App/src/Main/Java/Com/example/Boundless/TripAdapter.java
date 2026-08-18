package com.example.boundless;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    private List<Trip> tripList;

    public TripAdapter(List<Trip> tripList) {
        this.tripList = tripList;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // This links the adapter to your item_trip.xml file
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip currentTrip = tripList.get(position);

        // Populate the views with data from the Trip object
        holder.tripTitle.setText(currentTrip.getName());
        holder.tripSubtitle.setText(currentTrip.getSubtitle());

        if (currentTrip.getImagePath() != null && !currentTrip.getImagePath().isEmpty()) {
            Bitmap bitmap = ImageUtils.loadImageFromPath(currentTrip.getImagePath());
            holder.tripImage.setImageBitmap(bitmap);
        } else {
            holder.tripImage.setImageResource(currentTrip.getImageResourceId() != 0 ? currentTrip.getImageResourceId() : R.drawable.ic_launcher_background);
        }

        // Handle clicks on the entire card
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), TripDetailActivity.class);
            // Pass the trip data to the next screen
            intent.putExtra("TRIP_ID", currentTrip.getId());
            intent.putExtra("TRIP_NAME", currentTrip.getName());
            intent.putExtra("TRIP_DESC", currentTrip.getDescription());
            if (currentTrip.getImagePath() != null && !currentTrip.getImagePath().isEmpty()) {
                intent.putExtra("TRIP_IMAGE_PATH", currentTrip.getImagePath());
            } else {
                intent.putExtra("TRIP_IMAGE", currentTrip.getImageResourceId());
            }
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    // Holds the views so we don't have to keep finding them by ID
    public static class TripViewHolder extends RecyclerView.ViewHolder {
        ImageView tripImage;
        TextView tripTitle;
        TextView tripSubtitle;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tripImage = itemView.findViewById(R.id.img_trip_background);
            tripTitle = itemView.findViewById(R.id.tv_trip_title);
            tripSubtitle = itemView.findViewById(R.id.tv_trip_subtitle);
        }
    }
}