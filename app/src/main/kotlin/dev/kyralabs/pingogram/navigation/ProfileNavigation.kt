package dev.kyralabs.pingogram.navigation

import androidx.compose.runtime.State
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import dev.kyralabs.pingogram.core.navigation.ChatInfoRoute
import dev.kyralabs.pingogram.core.navigation.ChatRoute
import dev.kyralabs.pingogram.core.navigation.ChatsRoute
import dev.kyralabs.pingogram.core.navigation.ProfileRoute
import dev.kyralabs.pingogram.feature.chat.navigation.navigateToChatExport
import dev.kyralabs.pingogram.feature.contacts.navigation.navigateToEditContact
import dev.kyralabs.pingogram.feature.media.navigation.navigateToMediaViewer
import dev.kyralabs.pingogram.feature.profile.navigation.chatInfoScreens
import dev.kyralabs.pingogram.feature.profile.navigation.navigateToPlayer
import dev.kyralabs.pingogram.feature.profile.navigation.playerScreen
import dev.kyralabs.pingogram.feature.profile.navigation.popBackStackAfterGroupChange
import dev.kyralabs.pingogram.feature.profile.navigation.profileScreen

/**
 * A user's profile (`REQUIREMENTS.md` FR-P2), pushed above the screen it is opened from. Its "Message" returns to the
 * conversation below it when that is the user's chat; otherwise the chat (also a secret chat just started, a group a
 * mention named, a file at its message) opens in the Chats tab's detail pane on expanded and wider windows
 * ([besideList], [navigateToChat]) and is pushed above the profile on others, so back returns to the profile. Another
 * profile and a photo, video or GIF in the media viewer are pushed above it. "Add to contacts" and "Edit contact" push
 * the contact editor (`EditContactRoute`, `feature:contacts`) above it, "Export chat" the export page
 * (`ChatExportRoute`, `feature:chat`).
 */
internal fun NavGraphBuilder.profile(
    navController: NavHostController,
    besideList: State<Boolean>,
) {
    profileScreen(
        onBack = { navController.popBackStack() },
        onOpenChat = { chatId -> navController.openChatFromProfile(chatId, besideList = besideList.value) },
        onOpenProfile = { userId -> navController.navigateToProfile(userId) },
        onOpenMedia = { chatId, messageId -> navController.navigateToMediaViewer(chatId, messageId) },
        onOpenMessage = { chatId, messageId ->
            navController.navigateToChat(chatId, messageId, besideList = besideList.value)
        },
        onOpenPlayer = { userId -> navController.navigateToPlayer(userId) },
        onEditContact = { userId -> navController.navigateToEditContact(userId) },
        onExportChat = { chatId -> navController.navigateToChatExport(chatId) },
    )
    playerScreen(onBack = { navController.popBackStack() })
}

/** Pushes the profile of user [userId], unless it is shown already. */
internal fun NavHostController.navigateToProfile(userId: Long) {
    navigate(ProfileRoute(userId)) { launchSingleTop = true }
}

/**
 * Opens chat [chatId] from a profile or an info page: back to the conversation below it if it is that chat (the page
 * was opened from its title), else [besideList] in the Chats tab's detail pane, or pushed above the page.
 */
private fun NavHostController.openChatFromProfile(
    chatId: Long,
    besideList: Boolean,
) {
    val below = previousBackStackEntry
    val isBelow = below?.destination?.hasRoute<ChatRoute>() == true && below.toRoute<ChatRoute>().chatId == chatId
    if (isBelow) popBackStack() else navigateToChat(chatId, besideList = besideList)
}

/**
 * The info page of a group or channel and the pages it opens (`REQUIREMENTS.md` FR-P3, FR-P4), each pushed above the
 * one before. A profile it names is pushed above it; a chat opens as from a profile ([besideList]), and so does the
 * shared media. Once the user left or deleted the chat, the back stack returns to the chat list, as the conversation
 * below is gone.
 */
internal fun NavGraphBuilder.chatInfo(
    navController: NavHostController,
    besideList: State<Boolean>,
) {
    chatInfoScreens(
        onOpen = { route -> navController.navigate(route) { launchSingleTop = true } },
        onBack = { navController.popBackStack() },
        onLeft = { navController.popBackStackToChats() },
        onOpenChat = { chatId -> navController.openChatFromProfile(chatId, besideList = besideList.value) },
        onOpenProfile = { userId -> navController.navigateToProfile(userId) },
        onOpenMedia = { chatId, messageId -> navController.navigateToMediaViewer(chatId, messageId) },
        onOpenMessage = { chatId, messageId ->
            navController.navigateToChat(chatId, messageId, besideList = besideList.value)
        },
        onSaved = { navController.popBackStackAfterGroupChange() },
    )
}

/** Pushes the info page of group or channel [chatId], unless it is shown already. */
internal fun NavHostController.navigateToChatInfo(chatId: Long) {
    navigate(ChatInfoRoute(chatId)) { launchSingleTop = true }
}

/** Back to the chat list below everything pushed above it; one step back where it is not on the back stack. */
private fun NavHostController.popBackStackToChats() {
    if (!popBackStack<ChatsRoute>(inclusive = false)) popBackStack()
}
