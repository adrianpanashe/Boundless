package com.example.boundless;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TripDao {
    @Insert
    long insert(Trip trip);

    @Update
    void update(Trip trip);

    @Delete
    void delete(Trip trip);

    @Query("SELECT * FROM trips ORDER BY id DESC")
    List<Trip> getAllTrips();

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    Trip getTripById(int id);
}
