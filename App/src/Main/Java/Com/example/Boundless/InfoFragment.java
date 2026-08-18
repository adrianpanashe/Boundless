package com.example.boundless;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class InfoFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_info, container, false);

        TextView tvAppDetails = view.findViewById(R.id.tv_app_details);

        tvAppDetails.setText("Boundless is an interactive travel companion designed to help you capture and revisit your most cherished memories. Track your journey across the globe, save hidden gems, and get notified when you're near your favorite spots.");

        return view;
    }
}
