package com.knowlily.browser.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.knowlily.browser.model.BookmarkItem
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarksDao {

    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<BookmarkItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: BookmarkItem)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)")
    suspend fun exists(url: String): Boolean

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()
}
