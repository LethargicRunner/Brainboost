package com.lethargicrunner.brainboost

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.lethargicrunner.brainboost.audio.PlaybackService
import com.lethargicrunner.brainboost.ui.BrainboostScreen
import com.lethargicrunner.brainboost.ui.BrainboostTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* refusé : le son joue quand même, notification masquée */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BrainboostTheme {
                BrainboostScreen(
                    onPlay = { beat, carrier, duration ->
                        requestNotificationPermissionIfNeeded()
                        PlaybackService.start(this, beat, carrier, duration)
                    },
                    onStop = { PlaybackService.stop(this) }
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
