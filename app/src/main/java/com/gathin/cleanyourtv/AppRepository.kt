package com.gathin.cleanyourtv

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppEntry(
    val pkg: String,
    val label: String,
    val component: ComponentName,
    val icon: ImageBitmap,
    val banner: ImageBitmap?,
    val accent: Color,
)

data class HomeState(
    val all: List<AppEntry> = emptyList(),
    val pinned: List<AppEntry> = emptyList(),
    val recents: List<AppEntry> = emptyList(),
)

/** Reads installed apps and keeps pins / recent-launch times in SharedPreferences. */
class AppRepository(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    fun load(): HomeState {
        val pm = ctx.packageManager
        val seen = HashSet<String>()
        val entries = ArrayList<AppEntry>()

        // Leanback first so TV apps keep their banner; plain launcher apps fill the rest.
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (ri in pm.queryIntentActivities(query, 0)) {
                val ai = ri.activityInfo
                if (ai.packageName == ctx.packageName || !seen.add(ai.packageName)) continue
                val icon = ri.loadIcon(pm).toBitmap(256, 256, Bitmap.Config.ARGB_8888)
                val banner = ai.loadBanner(pm)?.toBitmap(320, 180, Bitmap.Config.ARGB_8888)
                entries += AppEntry(
                    pkg = ai.packageName,
                    label = ri.loadLabel(pm).toString(),
                    component = ComponentName(ai.packageName, ai.name),
                    icon = icon.asImageBitmap(),
                    banner = banner?.asImageBitmap(),
                    accent = averageColor(banner ?: icon),
                )
            }
        }
        entries.sortBy { it.label.lowercase() }

        val byPkg = entries.associateBy { it.pkg }
        val pinned = pinnedList().mapNotNull { byPkg[it] }
        val recents = entries
            .filter { prefs.getLong("last:${it.pkg}", 0L) > 0L }
            .sortedByDescending { prefs.getLong("last:${it.pkg}", 0L) }
            .take(10)
        return HomeState(all = entries, pinned = pinned, recents = recents)
    }

    fun recordLaunch(pkg: String) {
        prefs.edit().putLong("last:$pkg", System.currentTimeMillis()).apply()
    }

    fun togglePin(pkg: String) {
        val list = pinnedList().toMutableList()
        if (!list.remove(pkg)) list.add(pkg)
        prefs.edit().putString("pinned", list.joinToString("|")).apply()
    }

    private fun pinnedList(): List<String> =
        prefs.getString("pinned", "")!!.split("|").filter { it.isNotBlank() }

    private fun averageColor(bmp: Bitmap): Color {
        val px = Bitmap.createScaledBitmap(bmp, 1, 1, true).getPixel(0, 0)
        return Color(px).copy(alpha = 1f)
    }
}
