package dev.kyralabs.pingogram.feature.profile.player

import androidx.media3.common.Player
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** One track of a profile's music: the TDLib file to stream, and what the player and its notifications show. */
data class MusicTrack(
    val fileId: Int,
    val title: String,
    val performer: String,
)

/**
 * What the music player and the mini player show: the profile's [tracks], which one plays ([currentIndex]), whether it
 * plays, how far it got, and the repeat and shuffle modes. [profileUserId] is the profile the queue belongs to, so a
 * profile's card can tell whether the music it shows is the music that plays.
 */
data class MusicPlaybackState(
    val tracks: ImmutableList<MusicTrack> = persistentListOf(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val error: Boolean = false,
    val isConnected: Boolean = false,
    val positionMillis: Long = 0L,
    val durationMillis: Long = 0L,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffle: Boolean = false,
    val profileUserId: Long? = null,
) {
    /** The track that plays, or null while the queue is empty. */
    val currentTrack: MusicTrack?
        get() = tracks.getOrNull(currentIndex)
}
