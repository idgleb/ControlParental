package com.ursolgleb.controlparental.UI.adapters

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.data.local.dao.AppDao
import com.ursolgleb.controlparental.data.local.entities.BlockedEntity
import com.ursolgleb.controlparental.databinding.ItemBlockedAppBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BlockedAppsAdapter(
    private var blockedApps: MutableList<BlockedEntity>,
    private val appDao: AppDao
) : RecyclerView.Adapter<BlockedAppsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemBlockedAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, appDao)
    }

    override fun getItemCount() = blockedApps.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(blockedApps[position])
    }


    // 🔥 ✅ Función para agregar una nueva app bloqueada a la lista y actualizar la UI
    fun addBlockedApp(newBlockedApp: BlockedEntity) {
        blockedApps.add(newBlockedApp)  // Agregar a la lista
        notifyItemInserted(blockedApps.size - 1)  // Notificar el cambio a RecyclerView
    }

    // 🔥 ✅ Función para actualizar toda la lista
    fun updateList(newList: List<BlockedEntity>) {
        blockedApps.clear()
        blockedApps.addAll(newList)
        notifyDataSetChanged()
    }



    class ViewHolder(private val binding: ItemBlockedAppBinding, private val appDao: AppDao) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(blockedApp: BlockedEntity) {

            // 🔥 Obtener más información de la app usando Room (Ejemplo)
            CoroutineScope(Dispatchers.IO).launch {
                val appInfo = appDao.getApp(blockedApp.packageName)
                withContext(Dispatchers.Main) {
                    if (appInfo != null) {
                        // Convertir AdaptiveIconDrawable a BitmapDrawable si es necesario
                        val drawable = getAppIcon(appInfo.packageName,binding.ivIconoApp.context)
                        val bitmap = convertDrawableToBitmap(drawable!!)
                        binding.ivIconoApp.setImageBitmap(bitmap)
                        binding.textViewPackageName.text = appInfo.appName
                    } else {
                        //binding.textViewAppName.text = "No encontrada"
                    }
                }
            }

        }

        fun getAppIcon(packageName: String, context: Context): Drawable? {
            return try {
                val packageManager = context.packageManager
                packageManager.getApplicationIcon(packageName)  // 🔥 Obtiene el ícono real de la app
            } catch (e: PackageManager.NameNotFoundException) {
                null  // En caso de error, devuelve `null`
            }
        }

        fun convertDrawableToBitmap(drawable: Drawable): Bitmap {
            return if (drawable is AdaptiveIconDrawable) {
                val bitmap = Bitmap.createBitmap(
                    drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            } else {
                (drawable as BitmapDrawable).bitmap
            }

    }

}}
