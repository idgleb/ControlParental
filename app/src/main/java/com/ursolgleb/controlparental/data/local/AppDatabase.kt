package com.ursolgleb.controlparental.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ursolgleb.controlparental.data.local.dao.AppDao
import com.ursolgleb.controlparental.data.local.dao.BlockedDao
import com.ursolgleb.controlparental.data.local.dao.UsageLimitDao
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.data.local.entities.BlockedEntity
import com.ursolgleb.controlparental.data.local.entities.UsageLimitEntity

@Database(
    entities = [AppEntity::class, BlockedEntity::class, UsageLimitEntity::class],
    version = 5,  // Incrementa el número (antes era 1)
    exportSchema = true  // Para que Room guarde el esquema de versiones previas
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun blockedDao(): BlockedDao
    abstract fun usageLimitDao(): UsageLimitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "parental_control_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
