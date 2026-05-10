package com.knowlily.browser.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long
)
