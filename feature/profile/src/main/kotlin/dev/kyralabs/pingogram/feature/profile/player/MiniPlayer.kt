package dev.kyralabs.pingogram.feature.profile.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kyralabs.pingogram.core.ui.R as UiR
import dev.kyralabs.pingogram.core.ui.preview.PingogramPreview
import dev.kyralabs.pingogram.core.ui.preview.PingogramPreviews
import dev.kyralabs.pingogram.core.ui.theme.sizes
import dev.kyralabs.pingogram.core.ui.theme.spacing
import dev.kyralabs.pingogram.feature.profile.R
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * The floating round player for the chats and profiles, in Material 3 Expressive: a wavy ring shows how far the track
 * has played, the cookie-shaped circle opens the full player when tapped. Dragging it down moves it with the finger and
 * reveals a close button; released far enough, the music stops, otherwise it springs back.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiniPlayerBubble(
    onOpenPlayer: (userId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MiniPlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val track = state.currentTrack ?: return
    val profileUserId = state.profileUserId ?: return
    val progress =
        if (state.durationMillis > 0L) {
            (state.positionMillis.toFloat() / state.durationMillis.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    MiniPlayerBubbleContent(
        isPlaying = state.isPlaying,
        progress = progress,
        label = track.title,
        onOpen = { onOpenPlayer(profileUserId) },
        onClose = viewModel::onClose,
        modifier = modifier,
    )
}

/**
 * The round bubble without a view model, so previews can show it. It moves anywhere with the finger. Dragged down to the
 * bottom of the screen, a red close cross appears on it; released there, the music stops.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MiniPlayerBubbleContent(
    isPlaying: Boolean,
    progress: Float,
    label: String,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val screenHeight = LocalWindowInfo.current.containerSize.height.toFloat()
    val closeZoneTop = screenHeight - with(density) { CLOSE_ZONE_DP.dp.toPx() }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var bottomInRoot by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    // The drag gesture keeps the first composition's lambdas, so it reads the latest values through these.
    val bottomNow = rememberUpdatedState(bottomInRoot)
    val closeZoneNow = rememberUpdatedState(closeZoneTop)
    val onCloseNow = rememberUpdatedState(onClose)
    val overCloseZone = dragging && bottomInRoot > closeZoneTop
    val reveal by animateFloatAsState(targetValue = if (overCloseZone) 1f else 0f, label = "bubbleClose")
    val colors = MaterialTheme.colorScheme
    Box(
        modifier =
            modifier
                .size(BUBBLE_DP.dp)
                .onGloballyPositioned { coordinates ->
                    bottomInRoot = coordinates.positionInRoot().y + coordinates.size.height
                }.offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDrag = { change, amount ->
                            change.consume()
                            offset += amount
                        },
                        onDragEnd = {
                            if (bottomNow.value > closeZoneNow.value) onCloseNow.value()
                            dragging = false
                        },
                        onDragCancel = { dragging = false },
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        CircularWavyProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = colors.primary,
            trackColor = colors.surfaceContainerHighest,
        )
        Surface(
            onClick = onOpen,
            modifier = Modifier.padding(RING_INSET_DP.dp).fillMaxSize(),
            shape = MaterialShapes.Cookie9Sided.toShape(),
            color = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
            shadowElevation = 4.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter =
                        painterResource(
                            if (isPlaying) UiR.drawable.ic_pause_filled else UiR.drawable.ic_play_arrow_filled,
                        ),
                    contentDescription = label.ifBlank { null },
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Surface(
            onClick = onClose,
            enabled = reveal >= 1f,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .size(CLOSE_DP.dp)
                    .graphicsLayer {
                        alpha = reveal
                        scaleX = 0.5f + 0.5f * reveal
                        scaleY = scaleX
                    },
            shape = MaterialShapes.Cookie6Sided.toShape(),
            color = colors.errorContainer,
            contentColor = colors.onErrorContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(UiR.drawable.ic_close),
                    contentDescription = stringResource(R.string.player_close),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

private const val BUBBLE_DP = 72
private const val RING_INSET_DP = 6
private const val CLOSE_ZONE_DP = 120
private const val CLOSE_DP = 48

/** The mini player's controls: play or pause, and the next track. */
@HiltViewModel
class MiniPlayerViewModel
    @Inject
    constructor(
        private val playback: MusicPlaybackController,
    ) : ViewModel() {
        /** The queue, the track that plays and whether it plays. */
        val state = playback.state

        // Not connected here: the mini player shows on the chats screen, and the service starts only when something plays
        // (playProfile and the full player connect on their own).

        fun onPlayPause() = playback.toggle()

        fun onNext() = playback.next()

        fun onClose() = playback.close()
    }

/**
 * The mini player above the navigation bar: what plays, play or pause, and the next track. Tapping it opens the full
 * player of the track's profile ([onOpenPlayer]); it shows nothing while the queue is empty.
 */
@Composable
fun MiniPlayer(
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
        onClose = viewModel::onClose,
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
    onClose: () -> Unit,
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
            IconButton(onClick = onClose, shapes = IconButtonDefaults.shapes()) {
                Icon(
                    painter = painterResource(UiR.drawable.ic_close),
                    contentDescription = stringResource(R.string.player_close),
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
            onClose = {},
        )
    }
}
