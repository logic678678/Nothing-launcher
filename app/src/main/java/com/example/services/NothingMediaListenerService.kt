package com.example.services

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MediaState(
    val title: String = "NOTHING PLAYING",
    val artist: String = "No Active Session",
    val isPlaying: Boolean = false,
    val packageName: String = ""
)

class NothingMediaListenerService : NotificationListenerService() {

    private var mediaSessionManager: MediaSessionManager? = null
    private val activeCallbacks = mutableMapOf<MediaController, MediaController.Callback>()

    override fun onCreate() {
        super.onCreate()
        instance = this
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterAllControllers()
        instance = null
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        registerMediaCallback()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        unregisterAllControllers()
    }

    private fun registerMediaCallback() {
        try {
            val manager = mediaSessionManager ?: return
            val component = ComponentName(this, NothingMediaListenerService::class.java)
            
            // Listen to active sessions
            manager.addOnActiveSessionsChangedListener({ controllers ->
                updateControllers(controllers)
            }, component)

            val currentControllers = manager.getActiveSessions(component)
            updateControllers(currentControllers)
        } catch (e: Exception) {
            Log.e("NothingMediaService", "Failed to register active sessions listener", e)
        }
    }

    private fun updateControllers(controllers: List<MediaController>?) {
        unregisterAllControllers()
        if (controllers.isNullOrEmpty()) {
            _mediaState.value = MediaState()
            activeController = null
            return
        }

        // Use the first active controller (usually Spotify, YouTube, etc.)
        val controller = controllers[0]
        activeController = controller

        val callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                updateMetadata(controller)
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                updatePlaybackState(controller)
            }
        }

        controller.registerCallback(callback)
        activeCallbacks[controller] = callback

        // Initial update
        updateMetadata(controller)
        updatePlaybackState(controller)
    }

    private fun unregisterAllControllers() {
        activeCallbacks.forEach { (controller, callback) ->
            try {
                controller.unregisterCallback(callback)
            } catch (ignored: Exception) {}
        }
        activeCallbacks.clear()
    }

    private fun updateMetadata(controller: MediaController) {
        val metadata = controller.metadata
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "UNKNOWN TRACK"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "UNKNOWN ARTIST"
        _mediaState.value = _mediaState.value.copy(
            title = title,
            artist = artist,
            packageName = controller.packageName
        )
    }

    private fun updatePlaybackState(controller: MediaController) {
        val state = controller.playbackState
        val isPlaying = state != null && state.state == PlaybackState.STATE_PLAYING
        _mediaState.value = _mediaState.value.copy(
            isPlaying = isPlaying
        )
    }

    companion object {
        var instance: NothingMediaListenerService? = null
            private set

        var activeController: MediaController? = null
            private set

        private val _mediaState = MutableStateFlow(MediaState())
        val mediaState = _mediaState.asStateFlow()

        fun isPermissionGranted(context: Context): Boolean {
            val pkgName = context.packageName
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            if (!flat.isNullOrEmpty()) {
                val names = flat.split(":")
                for (name in names) {
                    val cn = ComponentName.unflattenFromString(name)
                    if (cn != null && cn.packageName == pkgName) {
                        return true
                    }
                }
            }
            return false
        }

        fun togglePlay() {
            try {
                activeController?.let { controller ->
                    val state = controller.playbackState
                    if (state != null && state.state == PlaybackState.STATE_PLAYING) {
                        controller.transportControls.pause()
                    } else {
                        controller.transportControls.play()
                    }
                }
            } catch (e: Exception) {
                Log.e("NothingMediaService", "Failed to toggle play", e)
            }
        }

        fun next() {
            try {
                activeController?.transportControls?.skipToNext()
            } catch (e: Exception) {
                Log.e("NothingMediaService", "Failed to skip next", e)
            }
        }

        fun previous() {
            try {
                activeController?.transportControls?.skipToPrevious()
            } catch (e: Exception) {
                Log.e("NothingMediaService", "Failed to skip previous", e)
            }
        }
    }
}
