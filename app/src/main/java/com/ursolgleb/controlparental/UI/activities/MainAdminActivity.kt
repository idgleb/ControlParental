package com.ursolgleb.controlparental.UI.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ursolgleb.controlparental.ControlParentalApp
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.UI.adapters.BlockedAppsAdapter
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.data.local.entities.BlockedEntity
import com.ursolgleb.controlparental.databinding.ActivityAdminMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class MainAdminActivity : AppCompatActivity() {
    lateinit var bindAdminMain: ActivityAdminMainBinding
    lateinit var adapter: BlockedAppsAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        bindAdminMain = ActivityAdminMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(bindAdminMain.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_admin)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initUI()
        initListeners()
        initBDApps()
    }

    fun getAppAgeRating(packageName: String): String {
        val url = "https://play.google.com/store/apps/details?id=$packageName"
        return try {
            val document = Jsoup.connect(url).get()
            val ageRatingElement = document.select("span[itemprop=contentRating] span").text()
            if (ageRatingElement.isNotEmpty()) {
                ageRatingElement
            } else {
                "x"
            }
        } catch (e: Exception) {
            "Error al obtener la clasificación"
        }
    }

    private fun initBDApps() {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        lifecycleScope.launch(Dispatchers.IO) {
            val appDao = ControlParentalApp.dbApps.appDao()
            for (app in apps) {
                var contentRating = getAppAgeRating(app.packageName)
                if (contentRating == "x" || contentRating.contains("Error")) {
                    contentRating = "?+"
                }
                val newApp = AppEntity(
                    packageName = app.packageName,
                    appName = app.loadLabel(pm).toString(),
                    appIcon = app.loadIcon(pm).toString(),
                    appCategory = app.category.toString(),
                    contentRating = contentRating,
                    appIsSystemApp = app.flags == 2
                )
                appDao.insertApp(newApp)

                Log.d(
                    "MainAdminActivity",
                    "App insertada: ${newApp.appName}, " +
                            "Package: ${newApp.packageName}, " +
                            "Rating: ${newApp.contentRating}, " +
                            "System App: ${newApp.appIsSystemApp}, " +
                            "Category: ${newApp.appCategory}, " +
                            "Icon: ${newApp.appIcon}"
                )

            }
        }

    }

    private fun initListeners() {
        bindAdminMain.ayudaBoton.setOnClickListener {
            intent = Intent(this, DesarolloActivity::class.java)
            startActivity(intent)
        }

        bindAdminMain.delitBlackedAppBoton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val blockedDao = ControlParentalApp.dbApps.blockedDao()
                blockedDao.deleteAllBlockedApps()
                withContext(Dispatchers.Main) {
                    adapter.updateList(emptyList())
                    Toast.makeText(
                        this@MainAdminActivity,
                        "Apps delited",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        bindAdminMain.aggregarAppsABlockedBoton.setOnClickListener {
            addAppBlockList("com.android.chrome")
            addAppBlockList("com.google.android.youtube")
        }


    }

    private fun addAppBlockList(packageName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val blockedDao = ControlParentalApp.dbApps.blockedDao()
            val appDao = ControlParentalApp.dbApps.appDao()

            val existingBlockedApp = blockedDao.getBlockedAppByPackageName(packageName)
            val app = appDao.getApp(packageName)

            if (existingBlockedApp == null) {
                val newBlockedApp = BlockedEntity(packageName = packageName)
                blockedDao.insertBlockedApp(newBlockedApp)

                withContext(Dispatchers.Main) {
                    adapter.addBlockedApp(newBlockedApp)
                    if (app != null) {
                        Toast.makeText(
                            this@MainAdminActivity,
                            app.appName,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    if (app != null) {
                        Toast.makeText(
                            this@MainAdminActivity,
                            "${app.appName} ya está bloqueado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun initUI() {
        initHeightDeSvInfo()

        adapter = BlockedAppsAdapter(
            emptyList<BlockedEntity>().toMutableList(),
            ControlParentalApp.dbApps.appDao()
        )
        bindAdminMain.rvAppsBloqueadas.layoutManager = LinearLayoutManager(this)
        bindAdminMain.rvAppsBloqueadas.adapter = adapter

        loadBlockedApps()

    }

    private fun loadBlockedApps() {
        lifecycleScope.launch(Dispatchers.IO) {
            val blockedApps = ControlParentalApp.dbApps.blockedDao().getAllBlockedApps()
            withContext(Dispatchers.Main) {
                adapter.updateList(blockedApps)
            }
        }
    }

    private fun initHeightDeSvInfo() {
        bindAdminMain.svInfo.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                bindAdminMain.svInfo.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val scrollViewHeight = bindAdminMain.svInfo.height
                // Calcula el 50% y lo aplica a vFondo
                val newHeight = (scrollViewHeight * 0.5).toInt()
                val params = bindAdminMain.vFondo.layoutParams
                params.height = newHeight
                bindAdminMain.vFondo.layoutParams = params
            }
        })
    }
}