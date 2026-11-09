package com.example.calculator.data;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
@Entity(tableName = "history")
public class HistoryEntry {
 @PrimaryKey(autoGenerate = true) public long id;
 @NonNull public String expression = "";
 @NonNull public String result = "";
 public long createdAt;
}
