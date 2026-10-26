package com.gathin.cleanyourtv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BgTop = Color(0xFF161A24)
private val BgBottom = Color(0xFF050608)

// Card sizes on the fixed 1280x720dp canvas.
private val BannerW = 256.dp
private val BannerH = 144.dp
private val IconSize = 96.dp

@Composable
fun HomeScreen(
    state: HomeState,
    onLaunch: (AppEntry) -> Unit,
    onTogglePin: (AppEntry) -> Unit,
    onOpenSettings: () -> Unit,
) {
    BackHandler(enabled = true) { /* a launcher has nowhere to go back to */ }

    // TVs report wildly different densities. Lay everything out on a fixed 1280x720dp canvas
    // so it looks the same on every screen (1920px wide => density 1.5).
    val metrics = LocalContext.current.resources.displayMetrics
    val canvas = remember(metrics.widthPixels) { Density(metrics.widthPixels / 1280f, 1f) }
    CompositionLocalProvider(LocalDensity provides canvas) {
        HomeContent(state, onLaunch, onTogglePin, onOpenSettings)
    }
}

@Composable
private fun HomeContent(
    state: HomeState,
    onLaunch: (AppEntry) -> Unit,
    onTogglePin: (AppEntry) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var focused by remember { mutableStateOf<AppEntry?>(null) }
    // Dynamic backdrop: glow follows the average colour of whichever app is focused.
    val glow by animateColorAsState(focused?.accent ?: Color(0xFF3A4A7A), tween(500), label = "glow")

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
            .background(
                Brush.radialGradient(
                    colors = listOf(glow.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(1400f, 200f),
                    radius = 1200f,
                )
            )
    ) {
        // Generous side/top padding keeps content inside the TV's overscan area.
        Column(Modifier.fillMaxSize().padding(start = 64.dp, end = 64.dp, top = 40.dp, bottom = 24.dp)) {
            TopBar()
            Hero(focused)
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                if (state.pinned.isNotEmpty()) {
                    item { Shelf("Favorites", state.pinned, BannerW, BannerH, true, { focused = it }, onLaunch, onTogglePin) }
                }
                if (state.recents.isNotEmpty()) {
                    item { Shelf("Recently used", state.recents, BannerW, BannerH, true, { focused = it }, onLaunch, onTogglePin) }
                }
                item { Shelf("All apps", state.all, IconSize, IconSize, false, { focused = it }, onLaunch, onTogglePin, onOpenSettings) }
            }
        }
    }
}

@Composable
private fun TopBar() {
    val now by produceState(Date()) {
        while (true) { value = Date(); delay(10_000) }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            SimpleDateFormat("h:mm", Locale.getDefault()).format(now),
            color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light,
        )
        Text(
            SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now),
            color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun Hero(app: AppEntry?) {
    Column(Modifier.padding(top = 16.dp, bottom = 4.dp).fillMaxWidth()) {
        Text(
            app?.label ?: "Home",
            color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
        )
        Text(
            if (app == null) "Select an app" else "OK to open  ·  hold OK to pin or unpin",
            color = Color.White.copy(alpha = 0.55f), fontSize = 14.sp,
        )
    }
}

@Composable
private fun Shelf(
    title: String,
    apps: List<AppEntry>,
    w: Dp,
    h: Dp,
    useBanner: Boolean,
    onFocus: (AppEntry) -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onTogglePin: (AppEntry) -> Unit,
    onOpenSettings: (() -> Unit)? = null,
) {
    Column {
        Text(title, color = Color.White.copy(alpha = 0.8f), fontSize = 17.sp, modifier = Modifier.padding(top = 6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            // Extra room so the focused (scaled-up) card is not clipped by the row.
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 12.dp),
        ) {
            items(apps, key = { it.pkg }) { app ->
                AppCard(app, w, h, useBanner, onFocus, { onLaunch(app) }, { onTogglePin(app) })
            }
            if (onOpenSettings != null) {
                item { SettingsCard(w, h, onOpenSettings) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCard(
    app: AppEntry,
    w: Dp,
    h: Dp,
    useBanner: Boolean,
    onFocus: (AppEntry) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.1f else 1f, tween(160), label = "scale")
    val shape = RoundedCornerShape(14.dp)

    Box(
        Modifier
            .size(w, h)
            .zIndex(if (isFocused) 1f else 0f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .border(if (isFocused) 3.dp else 0.dp, Color.White.copy(alpha = 0.9f), shape)
            .onFocusChanged { isFocused = it.isFocused; if (it.isFocused) onFocus(app) } // must sit before the focusable
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        if (useBanner && app.banner != null) {
            Image(app.banner, app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else if (useBanner) {
            // TV app without a banner: tinted card with the icon centred.
            Box(Modifier.fillMaxSize().background(app.accent.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                Image(app.icon, app.label, Modifier.size(72.dp))
            }
        } else {
            // Dock-style tile: the icon fills the whole card.
            Image(app.icon, app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsCard(w: Dp, h: Dp, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.1f else 1f, tween(160), label = "scale")
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .size(w, h)
            .zIndex(if (isFocused) 1f else 0f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(if (isFocused) 3.dp else 0.dp, Color.White.copy(alpha = 0.9f), shape)
            .onFocusChanged { isFocused = it.isFocused }
            .combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("Settings", color = Color.White, fontSize = 14.sp)
    }
}
