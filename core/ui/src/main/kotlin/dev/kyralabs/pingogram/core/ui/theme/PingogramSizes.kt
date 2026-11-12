package dev.kyralabs.pingogram.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Layout dimensions beyond the spacing scale (`docs/DESIGN_SYSTEM.md` §7). Feature modules use these instead of
 * `dp` literals (and of fractions of the window), through [MaterialTheme.sizes].
 *
 * @property contentMaxWidth the widest a single column of content gets (forms, onboarding, a list on its own),
 *   centered on medium and wider windows so lines and fields stay comfortable; the width of an M3 dialog.
 * @property illustrationMaxSize the largest side of a hero illustration or a QR code frame.
 * @property headlineBadge the side of a `MaterialShapes` badge with an icon above a headline, e.g. on every login step
 *   (`docs/DESIGN_SYSTEM.md` §3.3); its icon takes half of it.
 * @property avatarSmall an `Avatar` in a one- or two-line list item: contacts, members, search results (the M3 list
 *   item's leading avatar).
 * @property avatarRow an `Avatar` in a chat list row, next to a title and a one-line preview (`docs/DESIGN_SYSTEM.md`
 *   §7: between Material's 40 dp list avatar and [avatarMedium], so photos stay recognizable in a dense list).
 * @property avatarMedium an `Avatar` on the account's card at the top of Settings.
 * @property avatarLarge an `Avatar` heading a screen on its own: the profile photo in Edit profile.
 * @property avatarProfile the `Avatar` heading a user's profile, under its large flexible top bar, the largest avatar
 *   of the app, as the profile is about that person.
 * @property profilePhotoItem the side of a photo in the profile's carousel of profile photos (square, as Telegram
 *   keeps them).
 * @property sharedMediaCell the smallest cell of a photo or video in the grid of a profile's shared media: three
 *   across a phone, more on wider panes.
 * @property avatarExtraSmall an `Avatar` beside the last of a group of messages in a group chat, next to its bubble.
 * @property avatarRing the width of a ring around an `Avatar`, and of the gap that sets it off from the avatar, e.g.
 *   the ring in `primary` around the account's avatar on its card in Settings.
 * @property avatarBadge the diameter of a small round badge over an `Avatar`'s bottom end, e.g. the edit badge on the
 *   account's avatar in Settings, holding an icon of [badgeIcon].
 * @property inlineIcon an icon within a line of title text, e.g. the badges and mute icon after a chat's name.
 * @property inlineIconSmall an icon next to label text, e.g. read receipts beside a time, or a chat's pin.
 * @property badgeIcon an icon inside a `Badge`, e.g. the `@` of an unread mention; the size of a badge's digits.
 * @property inlineProgressIndicator an indeterminate progress indicator within a line of `labelLarge` text, e.g. in
 *   the chat list's connection pill.
 * @property textBlockPadding how far the rounded background of inline code, a code block or a hidden spoiler reaches
 *   past its text at either end, so the glyphs keep clear of its corners.
 * @property bubbleMaxWidthFraction the widest a message bubble gets, as a fraction of the conversation's width, so
 *   incoming and outgoing bubbles stay apart.
 * @property bubbleMaxWidth the widest a message bubble gets on wide windows, where [bubbleMaxWidthFraction] would make
 *   lines too long to read comfortably; the width of [contentMaxWidth].
 * @property reactionBadge the smallest a reaction's round badge on a bubble's corner gets: its diameter with one emoji.
 *   Its touch target is 48 dp nonetheless, reaching past it without taking room.
 * @property reactionBadgeRing the ring in the conversation's background color around a reaction badge, which sets it
 *   off from the bubble it overlaps.
 * @property reactionTile the width of a reaction with its count in the reactions picker's carousel of a message's
 *   reactions, at 100% font scale.
 * @property swipeReplyDistance how far a bubble is swiped toward the end to reply to it: released past it, the reply
 *   starts.
 * @property nextChatPullDistance how far a read channel is pulled up past its newest post to open the next unread
 *   channel: released past it, that channel opens.
 * @property messageMenuBlur the blur radius of the conversation behind a message lifted for its actions (API 31+).
 * @property messageMenuMinWindowHeight the shortest window that lifts a long-pressed message with its menu beside it;
 *   shorter ones (phones in landscape) show its actions in a bottom sheet. Material's lower bound of medium heights.
 * @property mediaMaxWidth the widest a photo, a video or an album gets in a message, and the width of a poll: on a
 *   phone more than [bubbleMaxWidthFraction] leaves, so media fill a bubble as wide as text may make it.
 * @property mediaMaxHeight the tallest a photo or a video gets in a message.
 * @property mediaMinSize the shortest side of a photo or a video in a message: panoramas and tall screenshots are
 *   cropped to it rather than shown as slivers.
 * @property mediaAlbumGap the gap between the photos and videos of an album.
 * @property playBadge the round badge on a video's or a GIF's thumbnail that says it plays.
 * @property sticker the square a sticker fits in, keeping its aspect ratio.
 * @property largeEmoji the square an emoji shown on its own fits in: an animated emoji, a die.
 * @property videoNote the diameter of a round video message.
 * @property contentIcon the circle behind the icon of a document, a location, a game or a story in a message.
 * @property waveformMinWidth the width of the waveform of a short voice message; longer ones get wider, up to
 *   [waveformMaxWidth] for a minute and more.
 * @property waveformMaxWidth the width of the waveform of a voice message of a minute or more.
 * @property waveformHeight the height of a voice message's waveform: its loudest bars.
 * @property waveformBarWidth the width of one bar of a voice message's waveform.
 * @property waveformBarGap the gap between two bars of a voice message's waveform.
 * @property attachmentPreview the square of a picked photo or video in the preview before it is sent.
 * @property fabSmall the height of a small extended floating action button and of the FAB it collapses to (Material 3
 *   Expressive's, 56 dp; material3 has no public token for it): the room a list leaves at its end,
 *   so its last row can scroll clear of one.
 * @property floatingNavigationBar the height of the floating navigation bar's pill and of the search button beside it,
 *   (`docs/DESIGN_SYSTEM.md` §6.1): the floating toolbar's 64 dp less its 8 dp of padding.
 * @property themeChip the diameter of a color theme's chip in Settings → Appearance → Theme, as large as a
 *   FAB, so the three colors of each chip are told apart.
 * @property denseTopAppBar the height of the small top app bar over the dense lists of the Chats and Contacts tabs and
 *   the archive (`docs/DESIGN_SYSTEM.md` §7, Density): Material's compact top app bar, 8 dp lower than its default.
 * @property pinPadMaxWidth the widest the passcode lock's keypad gets, so its keys stay within a thumb's reach.
 * @property passcodeDot the diameter of a dot that stands for an entered digit of a passcode.
 * @property composerField the height of the conversation composer's text field at one line, a filled tonal pill
 *   (`docs/DESIGN_SYSTEM.md` §6), as tall as the round send button beside it, and of the recording
 *   bar in its place.
 * @property recordingDot the dot that shows a voice message is being recorded, as loud as the microphone.
 * @property recordingCancelDistance how far the finger slides toward the start, holding the microphone, to cancel.
 * @property recordingLockDistance how far it slides up to lock the recording, which then goes on hands-free.
 * @property inputPanelHeight the height of the emoji and sticker panel above the navigation bar until the keyboard has
 *   shown once; then the panel takes the keyboard's height, so switching between them moves nothing.
 * @property panelEmoji the smallest cell of an emoji in the panel's grid: a 48 dp touch target.
 * @property panelSticker the smallest cell of a sticker in the panel's grid: five on a phone.
 * @property panelStickerCover the picture of a sticker set on its button in the panel's row of sets.
 * @property leadingIconContainer the tonal circle behind a settings row's leading icon, as in Android 16's Settings:
 *   the size of Material's leading avatar in a list item, so rows keep their heights.
 * @property keyImageOutline the outline around a secret chat's key image, which sets its square off from its frame
 *   whatever the colors of its edge pixels.
 * @property pinnedIndicator the width of the pinned messages bar's segments, one per pinned message, that show which
 *   one the bar shows.
 * @property pollBar the thickness of the bar under a poll's answer that shows its share of the votes, as Material's
 *   linear progress indicator.
 * @property sliderInactiveTrackInset how far from the thumb's centre a Material 3 Expressive slider's inactive track
 *   starts: half its 4 dp handle and the 6 dp gap beside it, where the media viewer's seek bar draws what is buffered.
 * @property chartHeight the plot of a statistics chart, without its labels.
 * @property chartLine the width of a statistics chart's lines.
 * @property experimentalNavigationBar the experimental look's floating navigation bar (`docs/DESIGN_SYSTEM.md` §13): a
 *   taller pill than [floatingNavigationBar], as the owner's design draws it.
 * @property experimentalFab the experimental look's "New chat" button beside that bar, a rounded square as tall.
 * @property experimentalBottomFade the experimental look's fade at the end of a tab's list, under the bar.
 * @property headerAction the experimental look's tonal circular action buttons in a tab's large header.
 * @property experimentalAccountAvatar the account's avatar on the experimental look's account card in Settings.
 * @property playerArtwork the square of the artwork in the profile music player.
 * @property playerPlayButton the diameter of the player's play and pause button, larger than the controls beside it.
 * @property miniPlayer the height of the mini player above the navigation bar.
 */
@Immutable
data class PingogramSizes(
    val contentMaxWidth: Dp = 560.dp,
    val illustrationMaxSize: Dp = 280.dp,
    val headlineBadge: Dp = 64.dp,
    val avatarSmall: Dp = 40.dp,
    val avatarRow: Dp = 48.dp,
    val avatarMedium: Dp = 56.dp,
    val avatarLarge: Dp = 96.dp,
    val avatarProfile: Dp = 120.dp,
    val profilePhotoItem: Dp = 120.dp,
    val sharedMediaCell: Dp = 112.dp,
    val avatarExtraSmall: Dp = 32.dp,
    val avatarRing: Dp = 2.dp,
    val avatarBadge: Dp = 24.dp,
    /** An avatar in a line of text: who ticked a checklist's task. */
    val avatarInline: Dp = 24.dp,
    val inlineIcon: Dp = 18.dp,
    val inlineIconSmall: Dp = 16.dp,
    val badgeIcon: Dp = 12.dp,
    val inlineProgressIndicator: Dp = 20.dp,
    val textBlockPadding: Dp = 2.dp,
    val bubbleMaxWidthFraction: Float = 0.8f,
    val bubbleMaxWidth: Dp = 560.dp,
    val reactionBadge: Dp = 28.dp,
    val reactionBadgeRing: Dp = 2.dp,
    val reactionTile: Dp = 64.dp,
    val swipeReplyDistance: Dp = 64.dp,
    val nextChatPullDistance: Dp = 96.dp,
    val messageMenuBlur: Dp = 16.dp,
    val messageMenuMinWindowHeight: Dp = 480.dp,
    val mediaMaxWidth: Dp = 320.dp,
    val mediaMaxHeight: Dp = 360.dp,
    val mediaMinSize: Dp = 120.dp,
    val mediaAlbumGap: Dp = 2.dp,
    val playBadge: Dp = 48.dp,
    val sticker: Dp = 160.dp,
    val largeEmoji: Dp = 112.dp,
    val videoNote: Dp = 200.dp,
    val contentIcon: Dp = 40.dp,
    val waveformMinWidth: Dp = 96.dp,
    val waveformMaxWidth: Dp = 192.dp,
    val waveformHeight: Dp = 24.dp,
    val waveformBarWidth: Dp = 3.dp,
    val waveformBarGap: Dp = 2.dp,
    val attachmentPreview: Dp = 120.dp,
    val fabSmall: Dp = 56.dp,
    val floatingNavigationBar: Dp = 56.dp,
    val denseTopAppBar: Dp = 56.dp,
    val themeChip: Dp = 56.dp,
    val pinPadMaxWidth: Dp = 360.dp,
    val passcodeDot: Dp = 14.dp,
    val composerField: Dp = 56.dp,
    val recordingDot: Dp = 10.dp,
    val recordingCancelDistance: Dp = 112.dp,
    val recordingLockDistance: Dp = 72.dp,
    val inputPanelHeight: Dp = 280.dp,
    val panelEmoji: Dp = 48.dp,
    val panelSticker: Dp = 72.dp,
    val panelStickerCover: Dp = 28.dp,
    val leadingIconContainer: Dp = 40.dp,
    val keyImageOutline: Dp = 2.dp,
    val pinnedIndicator: Dp = 3.dp,
    val pollBar: Dp = 4.dp,
    val sliderInactiveTrackInset: Dp = 8.dp,
    val chartHeight: Dp = 192.dp,
    val chartLine: Dp = 2.dp,
    val experimentalNavigationBar: Dp = 64.dp,
    val experimentalFab: Dp = 64.dp,
    val experimentalBottomFade: Dp = 132.dp,
    val experimentalAccountAvatar: Dp = 76.dp,
    val headerAction: Dp = 48.dp,
    val playerArtwork: Dp = 240.dp,
    val playerPlayButton: Dp = 72.dp,
    val miniPlayer: Dp = 64.dp,
)

val LocalPingogramSizes = staticCompositionLocalOf { PingogramSizes() }

/** The [PingogramSizes] provided by `PingogramTheme`. */
val MaterialTheme.sizes: PingogramSizes
    @Composable
    @ReadOnlyComposable
    get() = LocalPingogramSizes.current
