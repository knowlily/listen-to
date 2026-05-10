package com.knowlily.browser.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.knowlily.browser.model.HistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoryItem)

    @Query("DELETE FROM history")
    suspend fun deleteAll()
}
