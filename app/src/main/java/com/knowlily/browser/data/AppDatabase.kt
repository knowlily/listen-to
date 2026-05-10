package com.knowlily.browser.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.knowlily.browser.model.BookmarkItem
import com.knowlily.browser.model.HistoryItem

@Database(
    entities = [HistoryItem::class, BookmarkItem::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarksDao(): BookmarksDao
}
