package com.gathin.cleanyourtv

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val repo by lazy { AppRepository(applicationContext) }
    private var state by mutableStateOf(HomeState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeScreen(
                state = state,
                onLaunch = ::launch,
                onTogglePin = { app ->
                    repo.togglePin(app.pkg)
                    refresh()
                },
                onOpenSettings = {
                    startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refresh() // picks up installs/uninstalls and updated recents
    }

    private fun refresh() {
        lifecycleScope.launch {
            state = withContext(Dispatchers.Default) { repo.load() }
        }
    }

    private fun launch(app: AppEntry) {
        repo.recordLaunch(app.pkg)
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        )
    }
}
