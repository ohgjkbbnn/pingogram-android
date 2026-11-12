package dev.kyralabs.pingogram.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kyralabs.pingogram.core.common.result.Outcome
import dev.kyralabs.pingogram.core.domain.repository.UserProfileRepository
import dev.kyralabs.pingogram.core.navigation.ProfileRoute
import dev.kyralabs.pingogram.core.ui.theme.sizes
import dev.kyralabs.pingogram.core.ui.theme.spacing
import dev.kyralabs.pingogram.feature.profile.R
import dev.kyralabs.pingogram.feature.profile.player.MusicPlaybackController
import dev.kyralabs.pingogram.feature.profile.player.MusicTrack
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import dev.kyralabs.pingogram.core.ui.R as UiR

/**
 * The profile's music as the card shows it: the first [tracks] of the user's profile music, the track that plays
 * ([nowPlaying]) when it is this profile's queue, and whether it plays.
 */
data class ProfileMusicState(
    val tracks: ImmutableList<MusicTrack> = persistentListOf(),
    val nowPlaying: MusicTrack? = null,
    val isPlaying: Boolean = false,
)

/**
 * The profile's music card: it loads the first tracks of user [userId] for the card, and plays the profile's whole
 * queue through [MusicPlaybackController], so playback goes on when the profile closes. The player's service owns the
 * playback; this view model only mirrors it for the card.
 */
@HiltViewModel
class ProfileMusicViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val profileRepository: UserProfileRepository,
        private val playback: MusicPlaybackController,
    ) : ViewModel() {
        private val userId = savedStateHandle.toRoute<ProfileRoute>().userId

        private val preview = MutableStateFlow(ProfileMusicState())

        /** The card's first tracks, the track that plays when it is this profile's, and whether it plays. */
        val state: StateFlow<ProfileMusicState> =
            combine(preview, playback.state) { card, player ->
                val current = player.currentTrack?.takeIf { player.profileUserId == userId }
                card.copy(nowPlaying = current, isPlaying = current != null && player.isPlaying)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ProfileMusicState())

        init {
            playback.ensureConnected()
            viewModelScope.launch {
                // The card shows the first few tracks; a failure leaves the card out.
                val page = profileRepository.profileAudios(userId, offset = 0, limit = FIRST_TRACKS)
                if (page is Outcome.Success) {
                    val tracks =
                        page.value.audios.map { audio ->
                            MusicTrack(
                                fileId = audio.file.id,
                                title = audio.title.ifBlank { audio.fileName },
                                performer = audio.performer,
                            )
                        }
                    preview.update { it.copy(tracks = tracks.toImmutableList()) }
                }
            }
        }

        /** Plays the profile's music, or pauses or resumes it when it already plays. */
        fun onPlayPause() {
            val player = playback.state.value
            if (player.profileUserId == userId && player.currentTrack != null) {
                playback.toggle()
            } else {
                playback.playProfile(userId)
            }
        }

        private companion object {
            /** The tracks the profile's card loads: the first few; the player lists the rest. */
            const val FIRST_TRACKS = 5

            /** How long the card keeps its state after it leaves the composition. */
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }

/**
 * The profile's music card, under the actions: the playing track (else the first) with its title and performer. A tap
 * opens the full player ([onOpenPlayer]); the button plays, or pauses it while it plays. Shows nothing when the user
 * has no music on their profile.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ProfileMusicCard(
    modifier: Modifier = Modifier,
    onOpenPlayer: () -> Unit = {},
    viewModel: ProfileMusicViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state.nowPlaying
    val preview = state.tracks.firstOrNull()
    val title = current?.title ?: preview?.title ?: return
    val performer = current?.performer ?: preview?.performer.orEmpty()
    val isPlayingThis = current != null && state.isPlaying
    Surface(
        onClick = onOpenPlayer,
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
                        .size(MaterialTheme.sizes.avatarMedium)
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
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = MaterialTheme.spacing.medium),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = performer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledIconButton(
                onClick = viewModel::onPlayPause,
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(
                    painter =
                        painterResource(
                            if (isPlayingThis) UiR.drawable.ic_pause_filled else UiR.drawable.ic_play_arrow_filled,
                        ),
                    contentDescription =
                        stringResource(if (isPlayingThis) R.string.player_pause else R.string.player_play),
                )
            }
        }
    }
}
