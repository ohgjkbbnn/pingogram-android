package dev.kyralabs.pingogram.feature.profile.player

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kyralabs.pingogram.core.navigation.PlayerRoute
import dev.kyralabs.pingogram.core.ui.component.DelayedLoadingIndicator
import dev.kyralabs.pingogram.core.ui.component.EmptyState
import dev.kyralabs.pingogram.core.ui.component.windowMargin
import dev.kyralabs.pingogram.core.ui.preview.PingogramPreview
import dev.kyralabs.pingogram.core.ui.preview.PingogramPreviews
import dev.kyralabs.pingogram.core.ui.preview.PingogramVariantPreviews
import dev.kyralabs.pingogram.core.ui.theme.sizes
import dev.kyralabs.pingogram.core.ui.theme.spacing
import dev.kyralabs.pingogram.feature.profile.R
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject
import dev.kyralabs.pingogram.core.ui.R as UiR

/** What the player's controls ask of its view model. */
internal sealed interface PlayerEvent {
    data object PlayPause : PlayerEvent

    data object Next : PlayerEvent

    data object Previous : PlayerEvent

    data object Shuffle : PlayerEvent

    data object Repeat : PlayerEvent

    data object Retry : PlayerEvent

    data class Seek(
        val positionMillis: Long,
    ) : PlayerEvent

    data class Select(
        val index: Int,
    ) : PlayerEvent
}

/**
 * The full player of the profile's music: its artwork, the track and its performer, a seek bar, play and pause, the
 * previous and next track, shuffle and repeat, and the profile's whole queue. It opens from the profile's music card or
 * the mini player and shows the queue of [PlayerRoute]'s user, starting it when it is not the one that plays.
 */
@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val playback: MusicPlaybackController,
    ) : ViewModel() {
        private val userId = savedStateHandle.toRoute<PlayerRoute>().userId

        /** The queue, the track that plays, its progress and the player's modes. */
        val state = playback.state

        init {
            playback.ensureConnected()
            if (playback.state.value.profileUserId != userId || playback.state.value.tracks.isEmpty()) {
                playback.playProfile(userId)
            }
        }

        internal fun onEvent(event: PlayerEvent) {
            when (event) {
                PlayerEvent.PlayPause -> playback.toggle()
                PlayerEvent.Next -> playback.next()
                PlayerEvent.Previous -> playback.previous()
                PlayerEvent.Shuffle -> playback.toggleShuffle()
                PlayerEvent.Repeat -> playback.cycleRepeat()
                PlayerEvent.Retry -> playback.playProfile(userId)
                is PlayerEvent.Seek -> playback.seekTo(event.positionMillis)
                is PlayerEvent.Select -> playback.select(event.index)
            }
        }
    }

/** The player of the route's user, hoisted from [PlayerViewModel]. */
@Composable
internal fun PlayerRouteScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PlayerScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * The player's screen: a loading indicator until the queue arrives, an empty or error state when the profile has no
 * music or it could not be read, and [PlayerContent] otherwise.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PlayerScreen(
    state: MusicPlaybackState,
    onEvent: (PlayerEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.player_title),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            painter = painterResource(UiR.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.profile_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { innerPadding ->
        val loadingDescription = stringResource(R.string.player_loading)
        when {
            state.isLoading && state.tracks.isEmpty() -> {
                DelayedLoadingIndicator(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .semantics { contentDescription = loadingDescription },
                )
            }

            state.tracks.isEmpty() -> {
                EmptyState(
                    illustration = MaterialShapes.Cookie9Sided,
                    icon = UiR.drawable.ic_music_note,
                    title = stringResource(if (state.error) R.string.player_error else R.string.player_empty_title),
                    body = stringResource(if (state.error) R.string.player_error_body else R.string.player_empty_body),
                    modifier = Modifier.padding(innerPadding),
                    action =
                        if (state.error) {
                            {
                                Button(
                                    onClick = { onEvent(PlayerEvent.Retry) },
                                    shapes = ButtonDefaults.shapes(),
                                ) {
                                    Text(
                                        text = stringResource(R.string.profile_retry),
                                        style = MaterialTheme.typography.labelLargeEmphasized,
                                    )
                                }
                            }
                        } else {
                            null
                        },
                )
            }

            else -> {
                PlayerContent(state, onEvent, Modifier.padding(innerPadding))
            }
        }
    }
}

/** The player's artwork, now playing, seek bar, controls and queue. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayerContent(
    state: MusicPlaybackState,
    onEvent: (PlayerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = state.currentTrack ?: return
    val duration = state.durationMillis.coerceAtLeast(1L)
    var dragged by remember { mutableStateOf<Long?>(null) }
    val shown = (dragged ?: state.positionMillis).coerceIn(0L, duration)
    val maxWidth = MaterialTheme.sizes.contentMaxWidth
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                horizontal = windowMargin(),
                vertical = MaterialTheme.spacing.large,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
    ) {
        item { PlayerArtwork() }
        item {
            Column(
                modifier = Modifier.widthIn(max = maxWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.performer,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        item {
            Column(modifier = Modifier.widthIn(max = maxWidth)) {
                Slider(
                    value = shown.toFloat(),
                    onValueChange = { dragged = it.toLong() },
                    onValueChangeFinished = {
                        dragged?.let { onEvent(PlayerEvent.Seek(it)) }
                        dragged = null
                    },
                    valueRange = 0f..duration.toFloat(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = formatTime(shown), style = MaterialTheme.typography.labelSmall)
                    Text(text = formatTime(duration), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item { PlayerControls(state, onEvent, Modifier.widthIn(max = maxWidth)) }
        item {
            Text(
                text = stringResource(R.string.player_queue),
                modifier = Modifier.widthIn(max = maxWidth).semantics { heading() },
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
        }
        itemsIndexed(items = state.tracks, key = { _, item -> item.fileId }) { index, item ->
            PlayerQueueRow(
                track = item,
                current = index == state.currentIndex,
                isPlaying = state.isPlaying,
                onClick = { onEvent(PlayerEvent.Select(index)) },
                modifier = Modifier.widthIn(max = maxWidth),
            )
        }
    }
}

/** The square of artwork above the track: the profile's music has no cover, so it shows a note on a tonal square. */
@Composable
private fun PlayerArtwork() {
    Box(
        modifier =
            Modifier
                .size(MaterialTheme.sizes.playerArtwork)
                .clip(MaterialTheme.shapes.extraLargeIncreased)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(UiR.drawable.ic_music_note),
            contentDescription = null,
            modifier = Modifier.size(MaterialTheme.sizes.avatarLarge),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Shuffle, previous, play or pause, next and repeat. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayerControls(
    state: MusicPlaybackState,
    onEvent: (PlayerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val repeatLabel =
        stringResource(
            when (state.repeatMode) {
                Player.REPEAT_MODE_ALL -> R.string.player_repeat_all
                Player.REPEAT_MODE_ONE -> R.string.player_repeat_one
                else -> R.string.player_repeat_off
            },
        )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconToggleButton(
            checked = state.shuffle,
            onCheckedChange = { onEvent(PlayerEvent.Shuffle) },
            shapes = IconButtonDefaults.toggleableShapes(),
        ) {
            Icon(
                painter = painterResource(UiR.drawable.ic_shuffle_filled),
                contentDescription = stringResource(R.string.player_shuffle),
            )
        }
        FilledIconButton(
            onClick = { onEvent(PlayerEvent.Previous) },
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.size(MaterialTheme.sizes.fabSmall),
        ) {
            Icon(
                painter = painterResource(UiR.drawable.ic_skip_previous_filled),
                contentDescription = stringResource(R.string.player_previous),
            )
        }
        FilledIconButton(
            onClick = { onEvent(PlayerEvent.PlayPause) },
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.size(MaterialTheme.sizes.playerPlayButton),
        ) {
            Icon(
                painter =
                    painterResource(
                        if (state.isPlaying) UiR.drawable.ic_pause_filled else UiR.drawable.ic_play_arrow_filled,
                    ),
                contentDescription =
                    stringResource(if (state.isPlaying) R.string.player_pause else R.string.player_play),
            )
        }
        FilledIconButton(
            onClick = { onEvent(PlayerEvent.Next) },
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.size(MaterialTheme.sizes.fabSmall),
        ) {
            Icon(
                painter = painterResource(UiR.drawable.ic_skip_next_filled),
                contentDescription = stringResource(R.string.player_next),
            )
        }
        IconToggleButton(
            checked = state.repeatMode != Player.REPEAT_MODE_OFF,
            onCheckedChange = { onEvent(PlayerEvent.Repeat) },
            shapes = IconButtonDefaults.toggleableShapes(),
        ) {
            Icon(
                painter =
                    painterResource(
                        if (state.repeatMode == Player.REPEAT_MODE_ONE) {
                            UiR.drawable.ic_repeat_one_filled
                        } else {
                            UiR.drawable.ic_repeat_filled
                        },
                    ),
                contentDescription = repeatLabel,
            )
        }
    }
}

/** One track in the queue; tapping it plays it. */
@Composable
private fun PlayerQueueRow(
    track: MusicTrack,
    current: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color =
            if (current) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(MaterialTheme.sizes.avatarRow)
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            if (current) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(UiR.drawable.ic_music_note),
                    contentDescription = null,
                    tint =
                        if (current) {
                            MaterialTheme.colorScheme.onSecondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = MaterialTheme.spacing.medium)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
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
            if (current) {
                Icon(
                    painter =
                        painterResource(
                            if (isPlaying) UiR.drawable.ic_pause_filled else UiR.drawable.ic_play_arrow_filled,
                        ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** A position as a player shows it: `m:ss`, or `h:mm:ss` for an hour and more. */
private fun formatTime(millis: Long): String = DateUtils.formatElapsedTime(millis / 1_000)

@PingogramPreviews
@PingogramVariantPreviews
@Composable
private fun PlayerScreenPreview() {
    PingogramPreview {
        PlayerScreen(
            state =
                MusicPlaybackState(
                    tracks = PreviewTracks,
                    currentIndex = 0,
                    isPlaying = true,
                    positionMillis = 74_000,
                    durationMillis = 244_000,
                    profileUserId = 1,
                ),
            onEvent = {},
            onBack = {},
        )
    }
}

@PingogramPreviews
@Composable
private fun PlayerScreenLoadingPreview() {
    PingogramPreview {
        PlayerScreen(
            state = MusicPlaybackState(isLoading = true, profileUserId = 1),
            onEvent = {},
            onBack = {},
        )
    }
}

@PingogramPreviews
@Composable
private fun PlayerScreenEmptyPreview() {
    PingogramPreview {
        PlayerScreen(
            state = MusicPlaybackState(profileUserId = 1),
            onEvent = {},
            onBack = {},
        )
    }
}

private val PreviewTracks =
    persistentListOf(
        MusicTrack(fileId = 1, title = "Midnight City", performer = "M83"),
        MusicTrack(fileId = 2, title = "Intro", performer = "The xx"),
        MusicTrack(fileId = 3, title = "Strobe", performer = "deadmau5"),
    )
