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
    }

}