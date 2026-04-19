package com.example.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.data.local.notes.NoteDao
import com.example.myapplication.data.local.notes.NoteEntity
import com.example.myapplication.data.local.recentpdf.RecentPdfDao
import com.example.myapplication.data.local.recentpdf.RecentPdfEntity

@Database(
    entities = [
        NoteEntity::class,
        RecentPdfEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun recentPdfDao(): RecentPdfDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_application_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return getInstance(context)
        }
    }
}