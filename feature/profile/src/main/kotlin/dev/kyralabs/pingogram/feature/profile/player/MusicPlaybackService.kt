package dev.kyralabs.pingogram.feature.profile.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import dev.kyralabs.pingogram.core.domain.repository.FileStreamRepository
import dev.kyralabs.pingogram.core.ui.media.TdlibDataSource
import javax.inject.Inject
import javax.inject.Provider

/**
 * The service that owns the profile music's `ExoPlayer` ([MusicPlaybackController] controls it through a
 * `MediaController`): playback goes on when the profile closes, the app goes to the background or the screen turns off.
 * It streams TDLib files as they download ([TdlibDataSource]), holds audio focus while it plays, pauses when headphones
 * are unplugged and keeps the CPU awake while a stream downloads (`WAKE_LOCK`), as a player in the background must.
 *
 * The media session publishes what plays to the system's media controls, the lock screen and Android's media output
 * picker; its notification opens the app.
 */
@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {
    /**
     * Read only when a track starts streaming, never when the service is created: the service is bound from the app's
     * root as soon as the mini player shows, and at sign-in no account's graph exists yet to provide the repository.
     */
    @Inject lateinit var streams: Provider<FileStreamRepository>

    private var session: MediaSession? = null
    private var player: ExoPlayer? = null

    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val exo = newPlayer()
        player = exo
        val builder = MediaSession.Builder(this, exo)
        packageManager.getLaunchIntentForPackage(packageName)?.let { launch ->
            builder.setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    launch,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }
        session = builder.build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.let { mediaSession ->
            player?.release()
            mediaSession.release()
        }
        session = null
        player = null
        super.onDestroy()
    }

    /** The player, on the main looper, reading TDLib's files as they arrive. */
    @androidx.annotation.OptIn(UnstableApi::class)
    private fun newPlayer(): ExoPlayer =
        ExoPlayer
            .Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    DefaultDataSource.Factory(
                        this,
                        // Each stream opens its data source when it starts, so the active account is read then.
                        androidx.media3.datasource.DataSource.Factory { TdlibDataSource.Factory(streams.get()).createDataSource() },
                    ),
                ),
            ).setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            ).setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
}
