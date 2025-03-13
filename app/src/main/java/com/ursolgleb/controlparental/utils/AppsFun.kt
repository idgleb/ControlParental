package com.ursolgleb.controlparental.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class AppsFun {
    companion object {

        // Obtener todas las apps con UI
        fun getAllAppsWithUIdeSistema(context: Context): List<ApplicationInfo> {
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            return installedApps.filter { app -> siTieneUI(context, app.packageName) }
        }

        fun siTieneUI(context: Context, packageName: String): Boolean {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null)
            intent.setPackage(packageName)
            return pm.queryIntentActivities(intent, 0).isNotEmpty()
        }

        // Obtener el ícono de una aplicación
        fun getAppIcon(context: Context, packageName: String): Drawable? {
            return try {
                val packageManager = context.packageManager
                packageManager.getApplicationIcon(packageName)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e("AppDataRepository", "Ícono no encontrado para $packageName")
                null
            } catch (e: Exception) {
                Log.e("AppDataRepository", "Error al obtener ícono de $packageName: ${e.message}")
                null
            }
        }

        fun getApplicationInfo(context: Context, packageName: String): ApplicationInfo? {
            return try {
                val pm = context.packageManager
                pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                null // Si no se encuentra la app, retorna null
            }
        }

        // Obtener clasificación de edad de una app
        suspend fun getAppAgeRatingScraper(packageName: String): String {
            val url = "https://play.google.com/store/apps/details?id=$packageName"
            return withContext(Dispatchers.IO) {
                if (!Fun.isUrlExists(url)) {
                    Log.w("AppDataRepository", "La app no existe en Google Play")
                    return@withContext ""
                }
                try {
                    val doc: Document = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                        .timeout(5000)
                        .get()
                    doc.select("span[itemprop=contentRating]").first()?.text() ?: ""
                } catch (e: Exception) {
                    Log.e("AppDataRepository", "Error al obtener la clasificación", e)
                    ""
                }
            }
        }

    }

}