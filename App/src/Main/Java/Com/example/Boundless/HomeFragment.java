package com.example.boundless;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private TripAdapter adapter;
    private List<Trip> myTrips = new ArrayList<>();
    private AppDatabase db;
    private TripDao tripDao;
    private ExecutorService executorService;
    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        recyclerView = view.findViewById(R.id.recycler_view_trips);

        db = AppDatabase.getInstance(requireContext());
        tripDao = db.tripDao();
        executorService = Executors.newSingleThreadExecutor();

        adapter = new TripAdapter(myTrips);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTrips();
    }

    private void loadTrips() {
        executorService.execute(() -> {
            List<Trip> trips = tripDao.getAllTrips();
            if (trips.isEmpty()) {
                Trip t1 = new Trip(R.drawable.ic_launcher_background, "Croatia", "Summer 2017 - 14 days", "A beautiful trip along the Dalmatian coast.");
                Trip t2 = new Trip(R.drawable.ic_launcher_background, "Warsaw", "Spring 2017 - 3 days", "Exploring the historic old town and amazing museums.");
                Trip t3 = new Trip(R.drawable.ic_launcher_background, "Kappl", "Winter 2016 - 8 days", "The best skiing holiday in Kappl, located in the Paznaun Valley.");
                tripDao.insert(t1);
                tripDao.insert(t2);
                tripDao.insert(t3);
                trips = tripDao.getAllTrips();
            }

            final List<Trip> finalTrips = trips;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    myTrips.clear();
                    myTrips.addAll(finalTrips);
                    adapter.notifyDataSetChanged();
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
