package dev.kyralabs.pingogram.feature.profile.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.kyralabs.pingogram.core.navigation.PlayerRoute
import dev.kyralabs.pingogram.core.navigation.ProfileRoute
import dev.kyralabs.pingogram.core.ui.transition.AvatarTransformKey
import dev.kyralabs.pingogram.core.ui.transition.ProvideNavAnimatedVisibilityScope
import dev.kyralabs.pingogram.core.ui.transition.avatarTransformTarget
import dev.kyralabs.pingogram.feature.profile.player.PlayerRouteScreen
import dev.kyralabs.pingogram.feature.profile.ui.ProfileRoute as ProfileScreenRoute

/**
 * Adds a user's profile, [ProfileRoute], to the graph (`REQUIREMENTS.md` FR-P2), pushed above the screen it is opened
 * from. [onBack] leaves it; [onOpenChat] opens a chat (the private chat, a secret chat just started, a group or
 * channel a mention in the bio named), [onOpenProfile] another user's profile (a mention), [onOpenMedia] a photo, video
 * or GIF of the shared media in the media viewer, and [onOpenMessage] a file, song or voice message in the chat at its
 * message. [onOpenPlayer] opens the full player of the user's music. [onEditContact], where the app has a contact
 * editor, is what "Add to contacts" and "Edit contact" open for the user; without it, those rows are left out.
 * [onExportChat], where the app has the export page, is what "Export chat" opens for the private chat (Saved Messages
 * on one's own profile); without it, that row is left out. It grows out of the conversation's avatar that opened it,
 * and shrinks back into it (`docs/DESIGN_SYSTEM.md` §5, [avatarTransformTarget]).
 */
fun NavGraphBuilder.profileScreen(
    onBack: () -> Unit,
    onOpenChat: (chatId: Long) -> Unit,
    onOpenProfile: (userId: Long) -> Unit,
    onOpenMedia: (chatId: Long, messageId: Long) -> Unit,
    onOpenMessage: (chatId: Long, messageId: Long) -> Unit,
    onOpenPlayer: (userId: Long) -> Unit,
    onEditContact: ((userId: Long) -> Unit)? = null,
    onExportChat: ((chatId: Long) -> Unit)? = null,
) {
    composable<ProfileRoute> { entry ->
        val userId = entry.toRoute<ProfileRoute>().userId
        // It grows out of the conversation's avatar, and shrinks back into it.
        ProvideNavAnimatedVisibilityScope(this) {
            ProfileScreenRoute(
                onBack = onBack,
                onOpenChat = onOpenChat,
                onOpenProfile = onOpenProfile,
                onOpenMedia = onOpenMedia,
                onOpenMessage = onOpenMessage,
                onOpenPlayer = onOpenPlayer,
                modifier = Modifier.avatarTransformTarget(AvatarTransformKey.profile(userId)),
                onEditContact = onEditContact,
                onExportChat = onExportChat,
            )
        }
    }
}

/** Adds the full player of a profile's music, [PlayerRoute], above the profile or the screen the mini player is on. */
fun NavGraphBuilder.playerScreen(onBack: () -> Unit) {
    composable<PlayerRoute> { PlayerRouteScreen(onBack = onBack) }
}

/** Pushes the player of user [userId]'s music, unless it is shown already. */
fun NavHostController.navigateToPlayer(userId: Long) {
    navigate(PlayerRoute(userId)) { launchSingleTop = true }
}
