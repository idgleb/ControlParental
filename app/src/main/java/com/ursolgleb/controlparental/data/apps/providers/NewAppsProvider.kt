package com.ursolgleb.controlparental.data.apps.providers

import android.content.Context
import android.content.pm.ApplicationInfo
import com.ursolgleb.controlparental.data.apps.dao.AppDao
import com.ursolgleb.controlparental.utils.AppsFun
import com.ursolgleb.controlparental.utils.Logger
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.reactivecircus.cache4k.Cache
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Singleton
class NewAppsProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDao: AppDao
) {

    private val cache = Cache.Builder()
        .expireAfterWrite(1.toDuration(DurationUnit.MINUTES))
        .maximumCacheSize(1)
        .build<String, List<ApplicationInfo>>()

    suspend fun getNuevasAppsEnSistema(): List<ApplicationInfo> = cache.get("nuevas") {
            computeNuevasAppsEnSistema()
    }

    private fun computeNuevasAppsEnSistema(): List<ApplicationInfo> = runBlocking {
        val installedApps = AppsFun.getAllAppsWithUIdeSistema(context)
        if (installedApps.isEmpty()) return@runBlocking emptyList()
        val paquetesEnBD = appDao.getAllApps().first().map { it.packageName }.toSet()
        val nuevosApps = installedApps.filter { it.packageName !in paquetesEnBD }
        Logger.info(
            context,
            "NewAppsProvider",
            "NUEVAS APPS: ${nuevosApps.joinToString { it.packageName }}"
        )
        nuevosApps
    }
}