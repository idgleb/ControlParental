package com.ursolgleb.controlparental.data.log

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LogBlockedAppEntity::class],
    version = 3,
    exportSchema = true
)

abstract class LogAppBlockerDatabase : RoomDatabase() {

    abstract fun logBlockedAppDao(): LogBlockedAppDao

}
