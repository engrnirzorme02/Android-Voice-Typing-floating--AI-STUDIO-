package com.nirzor.voicebubble.service

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.nirzor.voicebubble.MainActivity
import com.nirzor.voicebubble.VoiceBubbleApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class VoiceBubbleTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onStartListening() {
        super.onStartListening()
        updateTileState(FloatingBubbleService.isServiceRunning.value)

        serviceScope.launch {
            FloatingBubbleService.isServiceRunning.collect { isRunning ->
                updateTileState(isRunning)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        serviceScope.coroutineContext[Job]?.cancelChildren()
    }

    override fun onClick() {
        super.onClick()
        val context = applicationContext
        val hasOverlay = Settings.canDrawOverlays(context)
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasOverlay || !hasMic) {
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_NAV_TARGET, MainActivity.TARGET_HOME)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, mainIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(mainIntent)
            }
            return
        }

        val isRunning = FloatingBubbleService.isServiceRunning.value
        if (isRunning) {
            FloatingBubbleService.stop(context)
            updateTileState(false)
        } else {
            val startMic = VoiceBubbleApp.instance.preferences.startListeningFromTile.value
            FloatingBubbleService.start(context, startListeningImmediately = startMic)
            updateTileState(true)
        }
    }

    private fun updateTileState(isActive: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
