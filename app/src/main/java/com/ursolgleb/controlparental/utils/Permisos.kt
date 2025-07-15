package com.ursolgleb.controlparental.utils

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.app.Activity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class Permisos {
    companion object{

        fun hasUsageStatsPermission(context: Context): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }

        fun requestUsageStatsPermission(context: Context) {
            val intent = Intent(
                Settings.ACTION_USAGE_ACCESS_SETTINGS,
                "package:${context.packageName}".toUri()
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // ✅ necesario fuera de Activity
            context.startActivity(intent)
        }
        
        fun hasLocationPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        fun requestLocationPermission(context: Context) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:" + context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        fun requestLocationPermission(
            fragment: androidx.fragment.app.Fragment,
            onResult: (Boolean) -> Unit
        ) {
            val launcher = getLocationPermissionLauncher(fragment, onResult)
            launcher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        fun hasBackgroundLocationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                // En versiones anteriores no existe el permiso, así que lo consideramos concedido
                true
            }
        }

        fun requestBackgroundLocationPermission(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:" + context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }

        fun requestBackgroundLocationPermission(
            fragment: androidx.fragment.app.Fragment,
            onResult: (Boolean) -> Unit
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val launcher = fragment.registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    onResult(granted)
                }
                launcher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                // En versiones anteriores, se considera concedido
                onResult(true)
            }
        }

        fun hasSystemAlertWindowPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }

        fun requestSystemAlertWindowPermission(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }

        fun getLocationPermissionLauncher(
            caller: ActivityResultCaller,
            onResult: (Boolean) -> Unit
        ): ActivityResultLauncher<Array<String>> {
            return caller.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val allGranted = permissions.all { it.value }
                onResult(allGranted)
            }
        }

    }
}