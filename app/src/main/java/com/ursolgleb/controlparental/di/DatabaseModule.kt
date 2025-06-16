package com.ursolgleb.controlparental.di

import android.content.Context
import android.os.Build
import android.os.UserManager
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ursolgleb.controlparental.data.apps.AppDatabase
import com.ursolgleb.controlparental.data.apps.dao.AppDao
import com.ursolgleb.controlparental.data.apps.dao.HorarioDao
import com.ursolgleb.controlparental.data.apps.dao.UsageEventDao
import com.ursolgleb.controlparental.data.apps.dao.UsageStatsDao
import com.ursolgleb.controlparental.data.apps.dao.DeviceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        var contextForDb = appContext
        val userManager = appContext.getSystemService(UserManager::class.java)
        if (userManager != null && !userManager.isUserUnlocked && !appContext.isDeviceProtectedStorage) {
            val deviceContext = appContext.createDeviceProtectedStorageContext()
            deviceContext.getDatabasePath("app_database.db").parentFile?.mkdirs()
            deviceContext.moveDatabaseFrom(appContext, "app_database.db")
            contextForDb = deviceContext
        }
        // Use a device protected context when the user is locked so the service can access
        // the database at boot time.

        // Objeto de migración
        val CLEAN_MIGRATION = object : Migration(34, 35) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(0)
                        if (name != "android_metadata" && name != "room_master_table") {
                            db.execSQL("DROP TABLE IF EXISTS `$name`")
                        }
                    }
                }
            }
        }


        // Al crear la base de datos
        return Room.databaseBuilder(
            contextForDb,
            AppDatabase::class.java,
            "app_database.db"
        )
            .addMigrations(CLEAN_MIGRATION) // Registrar la migración
            .fallbackToDestructiveMigration()
            .build()
    }

    @Singleton
    @Provides
    fun provideAppDao(db: AppDatabase): AppDao =
        db.appDao()

    @Singleton
    @Provides
    fun provideHorarioDao(db: AppDatabase): HorarioDao =
        db.horarioDao()


    @Singleton
    @Provides
    fun provideUsageEventDao(db: AppDatabase): UsageEventDao =
        db.usageEventDao()

    @Singleton
    @Provides
    fun provideUsageStatsDao(db: AppDatabase): UsageStatsDao =
        db.usageStatsDao()

    @Singleton
    @Provides
    fun provideDeviceDao(db: AppDatabase): DeviceDao =
        db.deviceDao()

}
