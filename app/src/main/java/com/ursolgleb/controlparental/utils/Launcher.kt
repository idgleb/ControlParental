package com.ursolgleb.controlparental.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class Launcher {
    companion object {
        fun getDefaultLauncherPackageName(context: Context): String {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)

            val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            return resolveInfo?.activityInfo?.packageName.toString()
        }

        fun isRealLauncher(packageName: String, context: Context): Boolean {
            return true
            //return hasPermission(context, packageName, "android.permission.BIND_APPWIDGET")
               //     && hasPermission(context, packageName, "android.permission.ACCESS_SHORTCUTS")
        }
        fun hasPermission(context: Context, packageName: String, permission: String): Boolean {
            val pm = context.packageManager
            return pm.checkPermission(permission, packageName) == PackageManager.PERMISSION_GRANTED
        }
    }

}