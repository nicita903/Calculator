package com.example.calculator.data;
import androidx.room.*;
@Database(entities = {HistoryEntry.class}, version = 1, exportSchema = false)
public abstract class HistoryDatabase extends RoomDatabase {
 public abstract HistoryDao history();
}
