package com.ursolgleb.controlparental.data.log

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LogBlockedAppEntity::class], version = 1)
abstract class LogAppBlockerDatabase : RoomDatabase() {
    abstract fun logBlockedAppDao(): LogBlockedAppDao

    companion object {
        @Volatile
        private var INSTANCE: LogAppBlockerDatabase? = null

        fun getDatabase(context: Context): LogAppBlockerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LogAppBlockerDatabase::class.java,
                    "app_blocker.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
