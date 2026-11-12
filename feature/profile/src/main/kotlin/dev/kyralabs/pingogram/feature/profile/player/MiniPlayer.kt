package dev.kyralabs.pingogram.feature.profile.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kyralabs.pingogram.core.ui.preview.PingogramPreview
import dev.kyralabs.pingogram.core.ui.preview.PingogramPreviews
import dev.kyralabs.pingogram.core.ui.theme.sizes
import dev.kyralabs.pingogram.core.ui.theme.spacing
import dev.kyralabs.pingogram.feature.profile.R
import javax.inject.Inject
import dev.kyralabs.pingogram.core.ui.R as UiR

/** The mini player's controls: play or pause, and the next track. */
@HiltViewModel
class MiniPlayerViewModel
    @Inject
    constructor(
        private val playback: MusicPlaybackController,
    ) : ViewModel() {
        /** The queue, the track that plays and whether it plays. */
        val state = playback.state

        init {
            playback.ensureConnected()
        }

        fun onPlayPause() = playback.toggle()

        fun onNext() = playback.next()
    }

/**
 * The mini player above the navigation bar: what plays, play or pause, and the next track. Tapping it opens the full
 * player of the track's profile ([onOpenPlayer]); it shows nothing while the queue is empty.
 */
@Composable
internal fun MiniPlayer(
    onOpenPlayer: (userId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MiniPlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val track = state.currentTrack ?: return
    val profileUserId = state.profileUserId ?: return
    MiniPlayerContent(
        track = track,
        isPlaying = state.isPlaying,
        profileUserId = profileUserId,
        onOpenPlayer = onOpenPlayer,
        onPlayPause = viewModel::onPlayPause,
        onNext = viewModel::onNext,
        modifier = modifier,
    )
}

/** The mini player's row, without a view model, so previews can show it. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MiniPlayerContent(
    track: MusicTrack,
    isPlaying: Boolean,
    profileUserId: Long,
    onOpenPlayer: (userId: Long) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = { onOpenPlayer(profileUserId) },
        modifier =
            modifier
                .widthIn(max = MaterialTheme.sizes.contentMaxWidth)
                .fillMaxWidth()
                .height(MaterialTheme.sizes.miniPlayer),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = MaterialTheme.spacing.small,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(MaterialTheme.sizes.avatarRow)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(UiR.drawable.ic_music_note),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = MaterialTheme.spacing.medium)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.performer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onPlayPause, shapes = IconButtonDefaults.shapes()) {
                Icon(
                    painter =
                        painterResource(
                            if (isPlaying) UiR.drawable.ic_pause_filled else UiR.drawable.ic_play_arrow_filled,
                        ),
                    contentDescription =
                        stringResource(if (isPlaying) R.string.player_pause else R.string.player_play),
                )
            }
            IconButton(onClick = onNext, shapes = IconButtonDefaults.shapes()) {
                Icon(
                    painter = painterResource(UiR.drawable.ic_skip_next_filled),
                    contentDescription = stringResource(R.string.player_next),
                )
            }
        }
    }
}

@PingogramPreviews
@Composable
private fun MiniPlayerPreview() {
    PingogramPreview {
        MiniPlayerContent(
            track = MusicTrack(fileId = 1, title = "Midnight City", performer = "M83"),
            isPlaying = true,
            profileUserId = 1,
            onOpenPlayer = {},
            onPlayPause = {},
            onNext = {},
        )
    }
}
