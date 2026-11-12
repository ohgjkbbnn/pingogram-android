package dev.kyralabs.pingogram.feature.profile.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.kyralabs.pingogram.core.common.result.Outcome
import dev.kyralabs.pingogram.core.domain.repository.FileStreamRepository
import dev.kyralabs.pingogram.core.domain.repository.UserProfileRepository
import dev.kyralabs.pingogram.core.ui.media.TdlibDataSource
import kotlinx.collections.immutable.persistentListOf
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
 * The app's one music player, for the profile's music. It lives in the app's process, not in a system service: the
 * card, the mini player and the full player all read [state] and call the functions below. The player is created when
 * the user first plays a profile's music, so no account is read before a track plays. Playback goes on when the profile
 * closes, as long as the app runs.
 */
@OptIn(UnstableApi::class)
@Singleton
class MusicPlaybackController
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val profiles: Provider<UserProfileRepository>,
        private val streams: Provider<FileStreamRepository>,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        private val mutableState = MutableStateFlow(MusicPlaybackState())

        /** The queue, the track that plays, its progress and the player's modes. */
        val state: StateFlow<MusicPlaybackState> = mutableState.asStateFlow()

        private var player: ExoPlayer? = null
        private var loading: Job? = null

        init {
            scope.launch {
                while (isActive) {
                    delay(POSITION_UPDATE_MILLIS)
                    player?.let(::refresh)
                }
            }
        }

        /** Kept for the screens that call it before playing; the player is created on the first play. */
        fun ensureConnected() {
            // Nothing to connect: the player is created when a profile's music starts.
        }

        /**
         * Plays the music of [userId], from its first track, or resumes it when it is already the queue. Loads every
         * page of the profile's music first, so the full player lists all of it.
         */
        fun playProfile(userId: Long) {
            val current = mutableState.value
            if (current.profileUserId == userId && current.tracks.isNotEmpty()) {
                if (!current.isLoading) resumeOrRestart()
                return
            }
            if (current.profileUserId == userId && current.isLoading) return
            loading?.cancel()
            loading =
                scope.launch {
                    mutableState.update {
                        it.copy(
                            tracks = persistentListOf(),
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
                    val exo = player ?: createPlayer()
                    exo.setMediaItems(tracks.map(::mediaItemOf), 0, 0L)
                    exo.prepare()
                    exo.play()
                }
        }

        /** Stops the music and clears the queue: the mini player hides until the next play. */
        fun close() {
            loading?.cancel()
            player?.stop()
            player?.clearMediaItems()
            mutableState.update { MusicPlaybackState(isConnected = it.isConnected) }
        }

        /** Plays or pauses the queue. */
        fun toggle() {
            val exo = player ?: return
            if (exo.isPlaying) exo.pause() else resumeOrRestart()
        }

        /** Moves to the next track, wrapping around when the player repeats. */
        fun next() {
            player?.seekToNextMediaItem()
        }

        /** Goes back to the start of the track, or to the previous one when it just started. */
        fun previous() {
            val exo = player ?: return
            if (exo.currentPosition > RESTART_THRESHOLD_MILLIS) {
                exo.seekTo(0)
            } else {
                exo.seekToPreviousMediaItem()
            }
        }

        fun seekTo(positionMillis: Long) {
            player?.seekTo(positionMillis)
        }

        /** Plays track [index] of the queue. */
        fun select(index: Int) {
            val exo = player ?: return
            exo.seekToDefaultPosition(index)
            exo.play()
        }

        fun toggleShuffle() {
            val exo = player ?: return
            exo.shuffleModeEnabled = !exo.shuffleModeEnabled
        }

        /** Cycles repeat from off to all, all to one, and one to off. */
        fun cycleRepeat() {
            val exo = player ?: return
            exo.repeatMode =
                when (exo.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
        }

        private fun resumeOrRestart() {
            val exo = player ?: return
            if (exo.playbackState == Player.STATE_ENDED) exo.seekToDefaultPosition()
            exo.play()
        }

        private fun createPlayer(): ExoPlayer {
            val exo =
                ExoPlayer
                    .Builder(context)
                    .setMediaSourceFactory(
                        DefaultMediaSourceFactory(
                            DefaultDataSource.Factory(context, TdlibDataSource.Factory(streams.get())),
                        ),
                    ).setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .setUsage(C.USAGE_MEDIA)
                            .build(),
                        true,
                    ).setHandleAudioBecomingNoisy(true)
                    .build()
            exo.addListener(listenerFor(exo))
            player = exo
            mutableState.update { it.copy(isConnected = true) }
            return exo
        }

        private fun listenerFor(exo: ExoPlayer) =
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    mutableState.update { it.copy(isPlaying = isPlaying) }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        mutableState.update { it.copy(isPlaying = false) }
                    }
                }

                override fun onMediaItemTransition(
                    mediaItem: MediaItem?,
                    reason: Int,
                ) {
                    mutableState.update { it.copy(currentIndex = exo.currentMediaItemIndex, positionMillis = 0L) }
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    mutableState.update { it.copy(repeatMode = repeatMode) }
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    mutableState.update { it.copy(shuffle = shuffleModeEnabled) }
                }

                override fun onPlayerError(error: PlaybackException) {
                    mutableState.update { it.copy(isPlaying = false, error = true) }
                }
            }

        private fun refresh(exo: ExoPlayer) {
            mutableState.update {
                it.copy(
                    positionMillis = exo.currentPosition,
                    durationMillis = exo.duration.takeIf { duration -> duration != C.TIME_UNSET } ?: 0L,
                    isPlaying = exo.isPlaying,
                )
            }
        }

        /** Every track of the profile's music, page by page; a failed page ends the queue with what came before. */
        private suspend fun loadTracks(userId: Long): List<MusicTrack> {
            val repository = profiles.get()
            val all = mutableListOf<MusicTrack>()
            var offset = 0
            while (true) {
                val result = repository.profileAudios(userId, offset = offset, limit = PAGE_SIZE)
                if (result !is Outcome.Success) return all
                val page = result.value
                all += page.audios.map { audio ->
                    MusicTrack(
                        fileId = audio.file.id,
                        title = audio.title.ifBlank { audio.fileName },
                        performer = audio.performer,
                    )
                }
                offset += page.audios.size
                if (page.audios.isEmpty() || offset >= page.totalCount) return all
            }
        }

        private fun mediaItemOf(track: MusicTrack): MediaItem =
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

        private companion object {
            const val POSITION_UPDATE_MILLIS = 500L
            const val RESTART_THRESHOLD_MILLIS = 3_000L
            const val PAGE_SIZE = 100
        }
    }
