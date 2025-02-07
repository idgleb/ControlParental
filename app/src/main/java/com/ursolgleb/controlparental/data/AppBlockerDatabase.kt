package com.ursolgleb.controlparental.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BlockedAppEntity::class], version = 1)
abstract class AppBlockerDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao

    companion object {
        @Volatile
        private var INSTANCE: AppBlockerDatabase? = null

        fun getDatabase(context: Context): AppBlockerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppBlockerDatabase::class.java,
                    "app_blocker.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
