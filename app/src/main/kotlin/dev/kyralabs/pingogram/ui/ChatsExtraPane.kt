package dev.kyralabs.pingogram.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dev.kyralabs.pingogram.core.navigation.ChatInfoRoute
import dev.kyralabs.pingogram.core.navigation.ProfileRoute
import dev.kyralabs.pingogram.core.ui.component.PaneAsWindow
import dev.kyralabs.pingogram.core.ui.theme.SharedAxisDistance
import dev.kyralabs.pingogram.core.ui.theme.predictiveBackPopEnter
import dev.kyralabs.pingogram.core.ui.theme.predictiveBackPopExit
import dev.kyralabs.pingogram.core.ui.theme.sharedAxisXEnter
import dev.kyralabs.pingogram.core.ui.theme.sharedAxisXExit
import dev.kyralabs.pingogram.core.ui.transition.LocalSharedTransitionScope
import dev.kyralabs.pingogram.feature.chat.navigation.chatExportScreen
import dev.kyralabs.pingogram.feature.chat.navigation.navigateToChatExport
import dev.kyralabs.pingogram.feature.profile.navigation.chatInfoScreens
import dev.kyralabs.pingogram.feature.profile.navigation.navigateToPlayer
import dev.kyralabs.pingogram.feature.profile.navigation.playerScreen
import dev.kyralabs.pingogram.feature.profile.navigation.popBackStackAfterGroupChange
import dev.kyralabs.pingogram.feature.profile.navigation.profileScreen

/**
 * The Chats tab's extra pane on extra-large windows (`docs/DESIGN_SYSTEM.md` §7): [content]'s profile or info page
 * beside the conversation, with the pages it opens above it in a `NavHost` of its own (another profile from a mention
 * or a member, the members, permissions and invite links of a group, the export page), sliding on the shared axis on
 * X and leaving with the predictive back gesture, as pushed pages do; as Settings' detail pane does. Back from the
 * first page, and its back button, close the pane ([ExtraPaneNavigation.onClose]).
 *
 * Laid out as the window it is ([PaneAsWindow]), so the pages show their one-pane layouts at the pane's width. Each
 * page gets its view models from a store of the pane's own ([rememberViewModelStoreOwner]), which survives rotation
 * and goes with the pane; the `NavHost`'s back stack is saved, so the pages survive process death too. A chat a page
 * opens goes to the detail pane, media to the viewer, the contact editor above the layout ([navigation]). The container
 * transform from the conversation's avatar is not run here: the pane comes in with the scaffold's own motion.
 */
@Composable
internal fun ChatsExtraPane(
    content: ChatsPaneContent,
    navigation: ExtraPaneNavigation,
    modifier: Modifier = Modifier,
) {
    val start: Any =
        when (content) {
            is ChatsPaneContent.Profile -> ProfileRoute(content.userId)
            is ChatsPaneContent.ChatInfo -> ChatInfoRoute(content.infoChatId)
            is ChatsPaneContent.Chat -> return
        }
    // Each page its own back stack and view models, gone with it.
    key(content.key) {
        PaneAsWindow(modifier) {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides rememberViewModelStoreOwner(),
                LocalSharedTransitionScope provides null,
            ) {
                ExtraPaneNavHost(start, navigation)
            }
        }
    }
}

/**
 * Where the extra pane's pages lead outside it ([ChatsExtraPane]): [onClose] closes the pane, [onOpenChat] and
 * [onOpenMessage] show a chat in the detail pane, [onOpenMedia] opens the media viewer, [onEditContact] the contact
 * editor, and [onLeft] returns to the list once the user left or deleted the group.
 */
internal class ExtraPaneNavigation(
    val onClose: () -> Unit,
    val onOpenChat: (chatId: Long) -> Unit,
    val onOpenMessage: (chatId: Long, messageId: Long) -> Unit,
    val onOpenMedia: (chatId: Long, messageId: Long) -> Unit,
    val onEditContact: (userId: Long) -> Unit,
    val onLeft: () -> Unit,
)

/** The extra pane's `NavHost`, starting at [start] ([ChatsExtraPane]). */
@Composable
private fun ExtraPaneNavHost(
    start: Any,
    navigation: ExtraPaneNavigation,
) {
    val navController = rememberNavController()
    val motionScheme = MaterialTheme.motionScheme
    val axisDistance = with(LocalDensity.current) { SharedAxisDistance.roundToPx() }
    val back = { navController.popOrElse(navigation.onClose) }
    val openProfile = { userId: Long -> navController.navigate(ProfileRoute(userId)) { launchSingleTop = true } }
    NavHost(
        navController = navController,
        startDestination = start,
        enterTransition = { sharedAxisXEnter(motionScheme, axisDistance, forward = true) },
        exitTransition = { sharedAxisXExit(motionScheme, axisDistance, forward = true) },
        popEnterTransition = { sharedAxisXEnter(motionScheme, axisDistance, forward = false) },
        popExitTransition = { sharedAxisXExit(motionScheme, axisDistance, forward = false) },
        predictivePopEnterTransition = { predictiveBackPopEnter() },
        predictivePopExitTransition = { predictiveBackPopExit(motionScheme) },
    ) {
        profileScreen(
            onBack = back,
            onOpenChat = navigation.onOpenChat,
            onOpenProfile = openProfile,
            onOpenMedia = navigation.onOpenMedia,
            onOpenMessage = navigation.onOpenMessage,
            onOpenPlayer = { userId -> navController.navigateToPlayer(userId) },
            onEditContact = navigation.onEditContact,
            onExportChat = { chatId -> navController.navigateToChatExport(chatId) },
        )
        playerScreen(onBack = back)
        chatInfoScreens(
            onOpen = { route -> navController.navigate(route) { launchSingleTop = true } },
            onBack = back,
            onLeft = navigation.onLeft,
            onOpenChat = navigation.onOpenChat,
            onOpenProfile = openProfile,
            onOpenMedia = navigation.onOpenMedia,
            onOpenMessage = navigation.onOpenMessage,
            onSaved = { navController.popBackStackAfterGroupChange() },
        )
        // "Export chat" of the pane's profile or info page, in the pane too.
        chatExportScreen(onBack = back)
    }
}

/** Pops the page shown, or [orElse] for the first. */
private fun NavController.popOrElse(orElse: () -> Unit) {
    if (previousBackStackEntry != null) popBackStack() else orElse()
}
