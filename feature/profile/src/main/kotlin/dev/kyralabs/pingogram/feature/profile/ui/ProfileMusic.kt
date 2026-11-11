package dev.kyralabs.pingogram.feature.profile.ui

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.kyralabs.pingogram.core.common.result.Outcome
import dev.kyralabs.pingogram.core.domain.model.DownloadPriority
import dev.kyralabs.pingogram.core.domain.model.FileState
import dev.kyralabs.pingogram.core.domain.repository.FileRepository
import dev.kyralabs.pingogram.core.domain.repository.UserProfileRepository
import dev.kyralabs.pingogram.core.navigation.ProfileRoute
import dev.kyralabs.pingogram.core.ui.theme.spacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import dev.kyralabs.pingogram.core.ui.R as UiR

/** One track of the profile's music: the file to play, and what the card shows. */
data class ProfileMusicTrack(
    val fileId: Int,
    val title: String,
    val performer: String,
)

/**
 * The profile's music as the card shows it: the first [tracks] of the user's profile music, which one plays
 * ([playingIndex]), and whether it plays ([isPlaying]).
 */
data class ProfileMusicState(
    val tracks: ImmutableList<ProfileMusicTrack> = persistentListOf(),
    val playingIndex: Int? = null,
    val isPlaying: Boolean = false,
)

/**
 * The profile's music (`UserProfileRepository.profileAudios`): loads the first tracks of user [userId], and plays a
 * track on its own [ExoPlayer] once TDLib has downloaded it (`FileRepository`). Playback belongs to this view model,
 * so it stops when the profile closes; a background player comes later.
 */
@HiltViewModel
class ProfileMusicViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        @param:ApplicationContext private val context: Context,
        private val profileRepository: UserProfileRepository,
        private val fileRepository: FileRepository,
    ) : ViewModel() {
        private val userId = savedStateHandle.toRoute<ProfileRoute>().userId

        private val mutableState = MutableStateFlow(ProfileMusicState())

        /** The tracks, which one plays, and whether it plays. */
        val state: StateFlow<ProfileMusicState> = mutableState.asStateFlow()

        private var loading: Job? = null

        private val player: ExoPlayer =
            ExoPlayer.Builder(context).build().apply {
                addListener(
                    object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            mutableState.update { it.copy(isPlaying = isPlaying) }
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                mutableState.update { it.copy(isPlaying = false) }
                            }
                        }
                    },
                )
            }

        init {
            viewModelScope.launch {
                // The card shows the first few tracks; a failure leaves the card out.
                val page = profileRepository.profileAudios(userId, offset = 0, limit = FIRST_TRACKS)
                if (page is Outcome.Success) {
                    val tracks =
                        page.value.audios.map { audio ->
                            ProfileMusicTrack(
                                fileId = audio.file.id,
                                title = audio.title.ifBlank { audio.fileName },
                                performer = audio.performer,
                            )
                        }
                    mutableState.update { it.copy(tracks = tracks.toImmutableList()) }
                }
            }
        }

        /** Plays track [index], or pauses or resumes it when it already plays. */
        fun onPlayPause(index: Int) {
            if (mutableState.value.playingIndex == index) {
                if (player.isPlaying) player.pause() else player.play()
            } else {
                startTrack(index)
            }
        }

        private fun startTrack(index: Int) {
            val track = mutableState.value.tracks.getOrNull(index) ?: return
            loading?.cancel()
            player.stop()
            mutableState.update { it.copy(playingIndex = index, isPlaying = false) }
            loading =
                viewModelScope.launch {
                    fileRepository.download(track.fileId, DownloadPriority.Visible)
                    val path =
                        fileRepository
                            .observe(track.fileId)
                            .filterIsInstance<FileState.Downloaded>()
                            .first()
                            .path
                    player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
                    player.prepare()
                    player.play()
                }
        }

        override fun onCleared() {
            loading?.cancel()
            player.release()
            super.onCleared()
        }

        private companion object {
            /** The tracks the profile's card loads: the first few; the rest come with the player's list. */
            const val FIRST_TRACKS = 5
        }
    }

/**
 * The profile's music card, under the actions: the playing track (else the first) with its title and performer; a
 * tap plays it, or pauses it while it plays. Shows nothing when the user has no music on their profile.
 */
@Composable
internal fun ProfileMusicCard(
    modifier: Modifier = Modifier,
    viewModel: ProfileMusicViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val index = state.playingIndex ?: 0
    val track = state.tracks.getOrNull(index) ?: return
    val isPlayingThis = state.playingIndex == index && state.isPlaying
    Surface(
        onClick = { viewModel.onPlayPause(index) },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(UiR.drawable.ic_music_note),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.performer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter =
                    painterResource(
                        if (isPlayingThis) UiR.drawable.ic_pause_filled else UiR.drawable.ic_play_arrow_filled,
                    ),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
