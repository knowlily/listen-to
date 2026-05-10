package com.knowlily.browser.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.knowlily.browser.data.AppDatabase
import com.knowlily.browser.data.BookmarksDao
import com.knowlily.browser.data.HistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "knowlily_browser.db")
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    migrateFromSharedPreferences(context, db)
                }
            })
            .build()
    }

    @Provides
    fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideBookmarksDao(db: AppDatabase): BookmarksDao = db.bookmarksDao()

    private fun migrateFromSharedPreferences(context: Context, db: SupportSQLiteDatabase) {
        val historyPrefs = context.getSharedPreferences("browser_history", Context.MODE_PRIVATE)
        for ((key, value) in historyPrefs.all) {
            if (value is String) {
                val ts = key.toLongOrNull() ?: continue
                db.execSQL(
                    "INSERT OR IGNORE INTO history (url, timestamp) VALUES (?, ?)",
                    arrayOf(value, ts)
                )
            }
        }
        val bookmarkPrefs = context.getSharedPreferences("browser_bookmarks", Context.MODE_PRIVATE)
        for ((key, value) in bookmarkPrefs.all) {
            if (value is String) {
                val ts = key.substringBefore("-").toLongOrNull() ?: System.currentTimeMillis()
                db.execSQL(
                    "INSERT OR IGNORE INTO bookmarks (url, timestamp) VALUES (?, ?)",
                    arrayOf(value, ts)
                )
            }
        }
    }
}
