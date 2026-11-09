package com.example.calculator.data;
import androidx.room.*;
import java.util.List;
@Dao public interface HistoryDao {
 @Query("SELECT * FROM history ORDER BY id DESC LIMIT 200") List<HistoryEntry> recent();
 @Insert void insert(HistoryEntry entry);
 @Query("DELETE FROM history") void clear();
 @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY id DESC LIMIT 200)") void trim();
 @Transaction default void record(HistoryEntry entry) { insert(entry); trim(); }
}
