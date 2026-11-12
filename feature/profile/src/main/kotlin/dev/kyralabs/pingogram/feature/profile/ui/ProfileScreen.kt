package dev.kyralabs.pingogram.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculateThreePaneScaffoldValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import dev.kyralabs.pingogram.core.common.result.DomainError
import dev.kyralabs.pingogram.core.ui.component.DelayedLoadingIndicator
import dev.kyralabs.pingogram.core.ui.component.EmptyState
import dev.kyralabs.pingogram.core.ui.component.paneContainer
import dev.kyralabs.pingogram.core.ui.component.plusBottom
import dev.kyralabs.pingogram.core.ui.component.topOnly
import dev.kyralabs.pingogram.core.ui.component.windowMargin
import dev.kyralabs.pingogram.core.ui.component.withTop
import dev.kyralabs.pingogram.core.ui.preview.PingogramPreview
import dev.kyralabs.pingogram.core.ui.preview.PingogramPreviews
import dev.kyralabs.pingogram.core.ui.preview.PingogramVariantPreviews
import dev.kyralabs.pingogram.core.ui.text.errorText
import dev.kyralabs.pingogram.core.ui.theme.sizes
import dev.kyralabs.pingogram.core.ui.theme.spacing
import dev.kyralabs.pingogram.feature.profile.R
import dev.kyralabs.pingogram.feature.profile.ui.preview.ProfilePreviewData
import dev.kyralabs.pingogram.core.ui.R as UiR

/**
 * A user's profile (`REQUIREMENTS.md` FR-P2; `docs/DESIGN_SYSTEM.md` §6 "Profile header", §7):
 *
 * - **Compact and medium windows**: one page under the large flexible top bar ([ProfileTopBar], the avatar and name
 *   collapsing as it scrolls): the actions ([ProfileActionsRow]), the photos ([ProfilePhotosCarousel]), the information
 *   ([ProfileInfoGroup]), what else can be done ([ProfileMoreGroup]), then the shared media's tabs and items
 *   ([sharedMediaItems]), all in one [LazyVerticalGrid], so the page scrolls as one and only what shows is composed.
 * - **Expanded and wider**: Material's supporting pane canonical layout ([ProfilePanes], `SupportingPaneScaffold`
 *   of `material3-adaptive`): the profile as the main pane, the shared media as the supporting pane beside it, each in
 *   its own rounded container on the window's `surfaceContainer` (`paneContainer`, as the chats' and settings' panes).
 *   A profile as a third pane beside the conversation on extra-large windows (§7, M4) is not built: the profile is
 *   pushed above it.
 *
 * The page and its transparent bar are on the profile's page color, its rows segmented list items (`profileRowColors`),
 * as settings pages are. The content is inset by the window margin and as wide as a column of content gets
 * (`sizes.contentMaxWidth`), centered; the photo grid fills the width, three cells and more across. While the user is
 * looked up a delayed loading indicator shows; a user that cannot be found or read shows why, with "Try again".
 *
 * [onBack] is the top bar's back button; null hides it. [onEditContact], where the app has a contact editor, is what
 * "Add to contacts" or "Edit contact" opens; without it the row is left out. [onExportChat], where the app has the
 * export page, is what "Export chat" opens; without it that row is left out too. [onOpenPlayer] opens the full player
 * of the profile's music.
 */
@Suppress("LongParameterList") // The states, where their events go, and the app's navigation.
@Composable
internal fun ProfileScreen(
    state: ProfileUiState,
    media: SharedMediaUiState,
    onEvent: (ProfileEvent) -> Unit,
    onMediaEvent: (SharedMediaEvent) -> Unit,
    onBack: (() -> Unit)?,
    mediaActions: SharedMediaActions,
    modifier: Modifier = Modifier,
    onEditContact: (() -> Unit)? = null,
    onExportChat: (() -> Unit)? = null,
    onOpenPlayer: (userId: Long) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    twoPanes: Boolean = rememberTwoPanes(),
    gridState: LazyGridState = rememberLazyGridState(),
    showMusic: Boolean = false,
    showGifts: Boolean = false,
) {
    val page =
        ProfilePageParts(
            state,
            media,
            onEvent,
            onMediaEvent,
            mediaActions,
            onEditContact,
            onExportChat,
            onOpenPlayer,
            showMusic,
            showGifts,
        )
    if (twoPanes) {
        ProfilePanes(page, onBack, snackbarHostState, modifier, gridState)
    } else {
        ProfilePage(page, onBack, snackbarHostState, modifier, gridState)
    }
}

/** What the page shows and where its events go, handed down as one. */
internal class ProfilePageParts(
    val state: ProfileUiState,
    val media: SharedMediaUiState,
    val onEvent: (ProfileEvent) -> Unit,
    val onMediaEvent: (SharedMediaEvent) -> Unit,
    val mediaActions: SharedMediaActions,
    val onEditContact: (() -> Unit)?,
    val onExportChat: (() -> Unit)? = null,
    val onOpenPlayer: (userId: Long) -> Unit = {},
    val showMusic: Boolean = false,
    val showGifts: Boolean = false,
)

/** Whether the window has room for the supporting pane beside the profile: expanded and wider. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun rememberTwoPanes(): Boolean =
    calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2()).maxHorizontalPartitions > 1

/**
 * The profile as a page: its top bar collapsing with the grid's scroll (`exitUntilCollapsedScrollBehavior`), the
 * snackbars, and the body ([ProfileBody]), with the shared media below the rest where [withMedia].
 */
@Composable
internal fun ProfilePage(
    page: ProfilePageParts,
    onBack: (() -> Unit)?,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier,
    gridState: LazyGridState,
    withMedia: Boolean = true,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = profilePageColor(),
        topBar = { ProfileTopBar((page.state as? ProfileUiState.Content)?.header, scrollBehavior, onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        ProfileBody(page, innerPadding, gridState, withMedia)
    }
}

/**
 * Below the top bar: the loading indicator, the error, or the profile's grid. Its own composable, so the frames of a
 * collapsing top bar, which measure the `Scaffold`'s content anew, stop here (`docs/PERFORMANCE.md` §4).
 */
@Composable
private fun ProfileBody(
    page: ProfilePageParts,
    innerPadding: PaddingValues,
    gridState: LazyGridState,
    withMedia: Boolean,
) {
    val modifier = Modifier.fillMaxSize().consumeWindowInsets(innerPadding)
    when (val state = page.state) {
        ProfileUiState.Loading -> {
            val description = stringResource(R.string.profile_loading)
            Box(modifier.padding(innerPadding), contentAlignment = Alignment.Center) {
                DelayedLoadingIndicator(Modifier.semantics { contentDescription = description })
            }
        }

        is ProfileUiState.Error -> {
            ProfileError(state.error, onRetry = { page.onEvent(ProfileEvent.Retry) }, modifier.padding(innerPadding))
        }

        is ProfileUiState.Content -> {
            ProfileGrid(state, page, innerPadding, gridState, withMedia, modifier)
        }
    }
}

/**
 * The profile's lazy grid: full-width items for the actions, photos and rows, inset by the window margin and capped at
 * the content's width, then, [withMedia], the shared media. The grid has as many columns as cells of
 * `sizes.sharedMediaCell` fit; its images are decoded at the cells' real width.
 */
@Suppress("LongParameterList")
@Composable
private fun ProfileGrid(
    state: ProfileUiState.Content,
    page: ProfilePageParts,
    innerPadding: PaddingValues,
    gridState: LazyGridState,
    withMedia: Boolean,
    modifier: Modifier,
) {
    val margin = windowMargin()
    val sizes = MaterialTheme.sizes
    val spacing = MaterialTheme.spacing
    if (withMedia) SharedMediaLoadMoreEffect(gridState, page.media, page.onMediaEvent)
    BoxWithConstraints(modifier) {
        val inset = max(margin, (maxWidth - sizes.contentMaxWidth) / 2)
        val columns = (maxWidth / sizes.sharedMediaCell).toInt().coerceAtLeast(1)
        val cellSizePx = with(LocalDensity.current) { (maxWidth / columns).roundToPx() }
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            // Below the bar, so what scrolls never shows through the transparent bar, read in the layout phase.
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(remember(innerPadding) { innerPadding.topOnly() })
                    .testTag(PROFILE_GRID_TEST_TAG),
            state = gridState,
            contentPadding =
                remember(
                    innerPadding,
                    spacing,
                ) { innerPadding.withTop(0.dp).plusBottom(spacing.extraLarge) },
        ) {
            profileItems(state, page, inset)
            if (withMedia && page.media.chatId != null) {
                fullWidth(key = MEDIA_HEADER, inset = inset, top = spacing.extraLarge) {
                    ProfileSectionHeader(stringResource(R.string.profile_section_media))
                }
                sharedMediaItems(page.media, page.onMediaEvent, page.mediaActions, cellSizePx, inset)
            }
        }
    }
}

/** The actions, photos and rows of the profile, each a full-width item inset by [inset]. */
private fun LazyGridScope.profileItems(
    state: ProfileUiState.Content,
    page: ProfilePageParts,
    inset: Dp,
) {
    fullWidth(key = ACTIONS, inset = inset) {
        ProfileActionsRow(state.actions, page.onEvent, Modifier.padding(top = MaterialTheme.spacing.small))
    }
    if (page.showMusic) {
        item(key = MUSIC, span = { GridItemSpan(maxLineSpan) }, contentType = MUSIC) {
            ProfileMusicCard(
                modifier = Modifier.padding(start = inset, end = inset, top = MaterialTheme.spacing.small),
                onOpenPlayer = { page.onOpenPlayer(state.header.userId) },
            )
        }
    }
    if (state.photos.totalCount > 1) {
        item(key = PHOTOS, span = { GridItemSpan(maxLineSpan) }, contentType = PHOTOS) {
            ProfilePhotosCarousel(
                photos = state.photos,
                onEvent = page.onEvent,
                margin = inset,
                modifier = Modifier.padding(top = MaterialTheme.spacing.extraLarge),
            )
        }
    }
    if (!state.info.isEmpty) {
        fullWidth(key = INFO, inset = inset) {
            ProfileInfoGroup(state.info, page.onEvent, Modifier.padding(top = MaterialTheme.spacing.extraLarge))
        }
    }
    val actions = state.actions
    val hasMore =
        (actions.contact != null && page.onEditContact != null) ||
            page.onExportChat != null ||
            actions.isBlocked != null ||
            actions.canReport
    if (hasMore) {
        fullWidth(key = MORE, inset = inset) {
            ProfileMoreGroup(
                actions = actions,
                onEvent = page.onEvent,
                modifier = Modifier.padding(top = MaterialTheme.spacing.extraLarge),
                onEditContact = page.onEditContact,
                onExportChat = page.onExportChat,
            )
        }
    }
    if (page.showGifts) {
        item(key = GIFTS, span = { GridItemSpan(maxLineSpan) }, contentType = GIFTS) {
            ProfileGiftsSection(
                modifier = Modifier.padding(start = inset, end = inset, top = MaterialTheme.spacing.extraLarge),
            )
        }
    }
}

/** A full-width item of the grid, inset by [inset] at its sides and [top] above it. */
private fun LazyGridScope.fullWidth(
    key: String,
    inset: Dp,
    top: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }, contentType = key) {
        Box(Modifier.fillMaxWidth().padding(start = inset, end = inset, top = top)) { content() }
    }
}

/** Why the profile cannot show: an [EmptyState] on `Cookie4Sided` in `errorContainer`, with "Try again". */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileError(
    error: DomainError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    EmptyState(
        illustration = MaterialShapes.Cookie4Sided,
        icon = UiR.drawable.ic_error,
        title = stringResource(R.string.profile_error_title),
        body = if (error == DomainError.NotFound) stringResource(R.string.profile_not_found_body) else errorText(error),
        modifier = modifier,
        containerColor = colors.errorContainer,
        iconColor = colors.onErrorContainer,
        action = {
            Button(onClick = onRetry, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.profile_retry), style = MaterialTheme.typography.labelLargeEmphasized)
            }
        },
    )
}

/** The test tag of the profile's grid, for tests that scroll it. */
internal const val PROFILE_GRID_TEST_TAG = "profile_grid"

private const val ACTIONS = "actions"
private const val PHOTOS = "photos"
private const val INFO = "info"
private const val MORE = "more"
private const val MEDIA_HEADER = "media-header"
private const val MUSIC = "music"
private const val GIFTS = "gifts"
