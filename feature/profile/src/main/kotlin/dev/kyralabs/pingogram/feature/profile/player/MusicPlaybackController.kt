package dev.kyralabs.pingogram.feature.profile.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.kyralabs.pingogram.core.common.result.Outcome
import dev.kyralabs.pingogram.core.domain.model.Audio
import dev.kyralabs.pingogram.core.domain.repository.UserProfileRepository
import dev.kyralabs.pingogram.core.ui.media.TdlibDataSource
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * The app's one music player, for the profile's music: it asks [MusicPlaybackService] for a [MediaController], loads
 * every track of a profile through [UserProfileRepository], and hands the queue to the service's player. The service
 * owns the `ExoPlayer`, so playback goes on when the profile closes; [state] mirrors the player for the card, the full
 * player and the mini player.
 *
 * The service streams TDLib files as they download (`TdlibDataSource`), and the player keeps audio focus and pauses
 * when headphones are unplugged, as the service builds it. The repositories are looked up for every queue, so a queue
 * loaded after an account switch belongs to the new account.
 */
@Singleton
class MusicPlaybackController
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val profiles: Provider<UserProfileRepository>,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        private val mutableState = MutableStateFlow(MusicPlaybackState())

        /** The queue, the track that plays, its progress and the player's modes. */
        val state: StateFlow<MusicPlaybackState> = mutableState.asStateFlow()

        private var controller: MediaController? = null
        private var controllerFuture: ListenableFuture<MediaController>? = null
        private var pending: ((MediaController) -> Unit)? = null
        private var loading: Job? = null

        init {
            scope.launch {
                while (isActive) {
                    delay(POSITION_UPDATE_MILLIS)
                    controller?.let(::refresh)
                }
            }
        }

        /** Connects to the playback service, so the player exists before the first tap. */
        fun ensureConnected() {
            if (controller != null || controllerFuture != null) return
            val token = SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
            val future =
                MediaController
                    .Builder(context, token)
                    .setListener(ControllerListener())
                    .buildAsync()
            controllerFuture = future
            future.addListener(
                {
                    if (controllerFuture !== future) return@addListener
                    runCatching { future.get() }
                        .onSuccess { connected ->
                            controller = connected
                            mutableState.update { it.copy(isConnected = true) }
                            pending?.let { it(connected) }
                            pending = null
                            refresh(connected)
                        }.onFailure {
                            controllerFuture = null
                            mutableState.update { it.copy(isConnected = false) }
                        }
                },
                ContextCompat.getMainExecutor(context),
            )
        }

        /**
         * Plays the music of [userId], from its first track, or resumes it when it is already the queue. Loads every
         * page of the profile's music first, so the full player lists all of it.
         */
        fun playProfile(userId: Long) {
            val current = mutableState.value
            if (current.profileUserId == userId && current.tracks.isNotEmpty()) {
                if (!current.isLoading) withController { it.resumeOrRestart() }
                return
            }
            if (current.profileUserId == userId && current.isLoading) return
            ensureConnected()
            loading?.cancel()
            loading =
                scope.launch {
                    mutableState.update {
                        it.copy(
                            tracks = emptyList<MusicTrack>().toImmutableList(),
                            currentIndex = 0,
                            isPlaying = false,
                            isLoading = true,
                            error = false,
                            positionMillis = 0L,
                            durationMillis = 0L,
                            profileUserId = userId,
                        )
                    }
                    val tracks = loadTracks(userId)
                    if (tracks.isEmpty()) {
                        mutableState.update { it.copy(isLoading = false, error = true) }
                        return@launch
                    }
                    mutableState.update { it.copy(tracks = tracks.toImmutableList(), isLoading = false) }
                    withController { connected ->
                        connected.setMediaItems(
                            tracks.map { track ->
                                MediaItem
                                    .Builder()
                                    .setMediaId(track.fileId.toString())
                                    .setUri(TdlibDataSource.uriOf(track.fileId))
                                    .setMediaMetadata(
                                        MediaMetadata
                                            .Builder()
                                            .setTitle(track.title)
                                            .setArtist(track.performer)
                                            .build(),
                                    ).build()
                            },
                            0,
                            0L,
                        )
                        connected.prepare()
                        connected.play()
                    }
                }
        }

        /** Plays or pauses the current track. */
        fun toggle() = withController { it.resumeOrPause() }

        /** Moves to the next track, wrapping around when the player repeats. */
        fun next() = withController { it.seekToNextMediaItem() }

        /** Goes back to the start of the track, or to the previous one when it just started. */
        fun previous() =
            withController { connected ->
                if (connected.currentPosition > RESTART_THRESHOLD_MILLIS) {
                    connected.seekTo(0)
                } else {
                    connected.seekToPreviousMediaItem()
                }
            }

        /** Seeks to [positionMillis] of the current track. */
        fun seekTo(positionMillis: Long) = withController { connected -> connected.seekTo(positionMillis) }

        /** Plays track [index] of the queue. */
        fun select(index: Int) =
            withController { connected ->
                connected.seekToDefaultPosition(index)
                connected.play()
            }

        /** Turns shuffling on or off. */
        fun toggleShuffle() =
            withController { connected -> connected.shuffleModeEnabled = !connected.shuffleModeEnabled }

        /** Cycles repeat from off to all, all to one, and one to off. */
        fun cycleRepeat() =
            withController { connected ->
                connected.repeatMode =
                    when (connected.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
            }

        private fun withController(action: (MediaController) -> Unit) {
            val connected = controller
            if (connected != null) {
                action(connected)
            } else {
                pending = action
                ensureConnected()
            }
        }

        private suspend fun loadTracks(userId: Long): List<MusicTrack> {
            val tracks = mutableListOf<MusicTrack>()
            val repository = profiles.get()
            var offset = 0
            var total = Int.MAX_VALUE
            while (tracks.size < MAX_TRACKS && offset < total) {
                when (val page = repository.profileAudios(userId, offset = offset, limit = PAGE_SIZE)) {
                    is Outcome.Success -> {
                        val audios = page.value.audios
                        if (audios.isEmpty()) break
                        tracks += audios.map { it.toTrack() }
                        offset += audios.size
                        total = page.value.totalCount
                    }

                    is Outcome.Failure -> {
                        break
                    }
                }
            }
            return tracks
        }

        private fun refresh(source: MediaController) {
            val duration = source.duration.takeIf { it > 0 } ?: 0L
            mutableState.update {
                it.copy(
                    currentIndex = source.currentMediaItemIndex.coerceAtLeast(0),
                    isPlaying = source.isPlaying,
                    isConnected = true,
                    positionMillis = source.currentPosition.coerceAtLeast(0),
                    durationMillis = duration,
                    repeatMode = source.repeatMode,
                    shuffle = source.shuffleModeEnabled,
                )
            }
        }

        private inner class ControllerListener : MediaController.Listener {
            override fun onDisconnected(controller: MediaController) {
                this@MusicPlaybackController.controller = null
                controllerFuture = null
                mutableState.update { it.copy(isConnected = false, isPlaying = false) }
            }
        }

        private companion object {
            /** How often the player's progress reaches the UI. */
            const val POSITION_UPDATE_MILLIS = 500L

            /** Pressing previous later than this restarts the track instead of going to the previous one. */
            const val RESTART_THRESHOLD_MILLIS = 3_000L

            /** The profile's music a page holds. */
            const val PAGE_SIZE = 50

            /** The most tracks one profile's queue holds, so a wrong total count cannot load forever. */
            const val MAX_TRACKS = 500
        }
    }

/** The player's play and pause, restarting a queue that ended. */
private fun MediaController.resumeOrPause() {
    if (isPlaying) {
        pause()
    } else {
        resumeOrRestart()
    }
}

/** Starts or resumes playback, from the start when the queue has ended. */
private fun MediaController.resumeOrRestart() {
    if (playbackState == Player.STATE_ENDED) seekTo(0)
    play()
}

/** What a track's card and player show, from TDLib's audio. */
private fun Audio.toTrack(): MusicTrack =
    MusicTrack(
        fileId = file.id,
        title = title.ifBlank { fileName },
        performer = performer,
    )
