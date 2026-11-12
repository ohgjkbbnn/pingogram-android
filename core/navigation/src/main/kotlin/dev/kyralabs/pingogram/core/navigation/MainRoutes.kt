package dev.kyralabs.pingogram.core.navigation

import kotlinx.serialization.Serializable

/**
 * The graph of the signed-in app (`docs/ARCHITECTURE.md`, Navigation): the three top-level destinations
 * reached from the navigation bar or rail. [ChatsRoute] is its start destination.
 */
@Serializable
data object MainGraph

/** The chat list, `:feature:chats`. */
@Serializable
data object ChatsRoute

/** The contacts list (`REQUIREMENTS.md` FR-P1), `:feature:contacts`. */
@Serializable
data object ContactsRoute

/**
 * Adding a contact by phone number or username (`REQUIREMENTS.md` FR-P1), `:feature:contacts`, pushed above the
 * Contacts tab or the New chat screen.
 */
@Serializable
data object NewContactRoute

/**
 * Adding Telegram user [userId] to the contacts, or editing or deleting them as one (`REQUIREMENTS.md` FR-P1),
 * `:feature:contacts`, pushed above the screen it is opened from: the contacts list, or the user's profile.
 */
@Serializable
data class EditContactRoute(
    val userId: Long,
)

/** Settings, `:feature:settings`. */
@Serializable
data object SettingsRoute

/**
 * Editing the user's own profile (`REQUIREMENTS.md` FR-S1, Account): photo, name, username and bio,
 * `:feature:settings`, pushed above the Settings tab.
 */
@Serializable
data object EditProfileRoute

/**
 * Changing the account's phone number (`REQUIREMENTS.md` FR-S1): what it does, the new number, the code Telegram sends
 * to it, pushed above [EditProfileRoute].
 */
@Serializable
data object ChangePhoneNumberRoute

/** The user's chat folders (`REQUIREMENTS.md` FR-L3), their order and the folders Telegram suggests. */
@Serializable
data object ChatFoldersRoute

/** Editing chat folder [folderId], or a new folder with null, pushed above [ChatFoldersRoute]. */
@Serializable
data class ChatFolderEditRoute(
    val folderId: Int? = null,
)

/**
 * The user's sticker sets (`REQUIREMENTS.md` FR-C5), or their custom emoji sets ([emoji]): their order, archiving and
 * removing them.
 */
@Serializable
data class StickersRoute(
    val emoji: Boolean = false,
)

/**
 * The archived sticker sets ([archived]) or the trending ones, of custom emoji where [emoji], pushed above
 * [StickersRoute].
 */
@Serializable
data class StickerSetListRoute(
    val archived: Boolean,
    val emoji: Boolean = false,
)

/**
 * The sheet of sticker set [setId], or of the set named [name] (`t.me/addstickers/<name>`) when the id is 0,
 * `:feature:settings`: a dialog destination above the screen it was opened from.
 */
@Serializable
data class StickerSetRoute(
    val setId: Long = 0,
    val name: String? = null,
)

/**
 * The sheet of a shared chat folder's link [inviteLink] (`t.me/addlist/…`), `:feature:settings`: a dialog destination
 * above the screen the link was opened from.
 */
@Serializable
data class ChatFolderInviteRoute(
    val inviteLink: String,
)

/**
 * Appearance settings (`REQUIREMENTS.md` FR-S2): light or dark, AMOLED black, the color theme and contrast,
 * `:feature:settings`, pushed above the Settings tab.
 */
@Serializable
data object AppearanceRoute

/**
 * The color theme (`REQUIREMENTS.md` FR-S2, `docs/adr/0016-color-themes.md`): the wallpaper's colors or one of the
 * generated themes, `:feature:settings`, pushed above Appearance.
 */
@Serializable
data object ColorThemeRoute

/**
 * How conversations look (`REQUIREMENTS.md` FR-S1, Chat settings): message text size, bubble corners and wallpaper,
 * `:feature:settings`, pushed above Appearance.
 */
@Serializable
data object ChatAppearanceRoute

/**
 * The app's language (`REQUIREMENTS.md` FR-S1, §6.4): the system's or one of the languages Pingogram ships,
 * `:feature:settings`, pushed above the Settings tab.
 */
@Serializable
data object LanguageRoute

/**
 * Data and storage settings (`REQUIREMENTS.md` FR-S1): automatic download, how much space Pingogram takes and clearing
 * its cache, `:feature:settings`, pushed above the Settings tab.
 */
@Serializable
data object DataStorageRoute

/**
 * Power saving (Settings → Power saving): when it is on, following Battery Saver or always, and what it turns off,
 * `:feature:settings`, pushed above the Settings tab.
 */
@Serializable
data object PowerSavingRoute

/**
 * What downloads by itself on one kind of network, pushed above [DataStorageRoute]. [network] is the name of an
 * `AutoDownloadNetwork` of `:core:domain`, which this module does not depend on.
 */
@Serializable
data class AutoDownloadRoute(
    val network: String,
)

/** How much data Pingogram sent and received on each network, pushed above [DataStorageRoute]. */
@Serializable
data object NetworkUsageRoute

/** The proxies Pingogram connects through (`docs/adr/0027-proxies.md`), pushed above [DataStorageRoute]. */
@Serializable
data object ProxiesRoute

/** Adding a proxy, or editing proxy [proxyId] (TDLib's id), pushed above [ProxiesRoute]. */
@Serializable
data class EditProxyRoute(
    val proxyId: Int? = null,
)

/**
 * The sheet a proxy link (`tg://proxy`, `t.me/socks`) opens, which asks before connecting through the proxy: a dialog
 * destination of the settings pages. [kind] is the name of a `ProxyKind` of `:core:domain`; the fields another kind
 * does not use are empty. `toString()` leaves out the address and the credentials.
 */
@Serializable
data class ProxyLinkRoute(
    val server: String,
    val port: Int,
    val kind: String,
    val username: String = "",
    val password: String = "",
    val secret: String = "",
    val httpOnly: Boolean = false,
) {
    override fun toString() = "ProxyLinkRoute(kind=$kind, …)"
}

/**
 * About Pingogram (`REQUIREMENTS.md` FR-S1): version, source code, license and the open-source licenses,
 * `:feature:settings`, pushed above the Settings tab.
 */
@Serializable
data object AboutRoute

/**
 * The open-source licenses (`REQUIREMENTS.md` FR-S1, About): the libraries, fonts and icons Pingogram is built from,
 * `:feature:settings`, pushed above [AboutRoute].
 */
@Serializable
data object OpenSourceLicensesRoute

/**
 * One library of [OpenSourceLicensesRoute] with the full texts of its licenses, pushed above it. [libraryId] is the
 * id of an `OpenSourceLibrary` of `:core:domain`, which this module does not depend on.
 */
@Serializable
data class OpenSourceLibraryRoute(
    val libraryId: String,
)

/**
 * Privacy and security (`REQUIREMENTS.md` FR-S1): the privacy settings, blocked users, active sessions and 2-step
 * verification, `:feature:settings`, pushed above the Settings tab.
 */
@Serializable
data object PrivacySecurityRoute

/**
 * One privacy setting, who can see or do something, pushed above [PrivacySecurityRoute]. [setting] is the name of a
 * `PrivacySetting` of `:core:domain`, which this module does not depend on.
 */
@Serializable
data class PrivacySettingRoute(
    val setting: String,
)

/** The users and chats the user blocked, pushed above [PrivacySecurityRoute]. */
@Serializable
data object BlockedUsersRoute

/** The sessions of the account (devices logged in to it), pushed above [PrivacySecurityRoute]. */
@Serializable
data object ActiveSessionsRoute

/** The websites logged in with the account, pushed above [PrivacySecurityRoute]. */
@Serializable
data object ConnectedWebsitesRoute

/** The account's passkeys (`docs/adr/0037-passkeys.md`), pushed above [PrivacySecurityRoute]. */
@Serializable
data object PasskeysRoute

/**
 * The chats kept behind the passcode (`docs/adr/0029-locked-chats.md`), pushed above [PrivacySecurityRoute], above the
 * chat list from its "Locked chats" row, or from a locked chat's notification.
 */
@Serializable
data object LockedChatsRoute

/** Which channel the user's profile shows, pushed above [EditProfileRoute]. */
@Serializable
data object PersonalChannelRoute

/** The passcode lock's settings (`REQUIREMENTS.md` FR-S1), pushed above [PrivacySecurityRoute]. */
@Serializable
data object PasscodeLockRoute

/** Setting a new passcode, entered twice, pushed above [PasscodeLockRoute]. */
@Serializable
data object SetPasscodeRoute

/**
 * Deleting the account (Google Play's account deletion policy, `docs/PLAY_STORE.md`) and how long it may stay unused
 * before Telegram deletes it, pushed above [PrivacySecurityRoute].
 */
@Serializable
data object DeleteAccountRoute

/**
 * The login email (`REQUIREMENTS.md` FR-S1): Telegram's pattern of it and changing it with a code sent to the new
 * address, pushed above [PrivacySecurityRoute].
 */
@Serializable
data object LoginEmailRoute

/**
 * The self-destruct timer of the chats the user starts (`REQUIREMENTS.md` FR-S1), pushed above [PrivacySecurityRoute].
 */
@Serializable
data object AutoDeleteRoute

/** Who may start a chat with the user (`REQUIREMENTS.md` FR-S1), pushed above [PrivacySecurityRoute]. */
@Serializable
data object NewChatPrivacyRoute

/**
 * The users and groups a privacy [setting] (its `PrivacySetting` name) always allows ([always]) or never allows,
 * pushed above its [PrivacySettingRoute].
 */
@Serializable
data class PrivacyExceptionsRoute(
    val setting: String,
    val always: Boolean,
)

/** 2-step verification (`REQUIREMENTS.md` FR-S1): its state and its changes, pushed above [PrivacySecurityRoute]. */
@Serializable
data object TwoStepVerificationRoute

/**
 * One of 2-step verification's flows (setting, changing or removing the password, the recovery email, a forgotten
 * password), pushed above [TwoStepVerificationRoute]; [flow] is the flow's name.
 */
@Serializable
data class TwoStepVerificationFlowRoute(
    val flow: String,
)

/**
 * Notifications (`REQUIREMENTS.md` FR-S1): the defaults of private chats, groups and channels, "Keep connected" and
 * resetting them, `:feature:settings`, pushed above the Settings tab.
 */
@Serializable
data object NotificationSettingsRoute

/**
 * The chats of one kind with notification settings of their own, pushed above [NotificationSettingsRoute]. [scope] is
 * the name of a `NotificationScope` of `:core:domain`, which this module does not depend on.
 */
@Serializable
data class NotificationExceptionsRoute(
    val scope: String,
)

/** Whom the account hears of reactions and poll votes from, pushed above [NotificationSettingsRoute]. */
@Serializable
data object ReactionNotificationsRoute

/** The quick reaction and where a double tap adds it, pushed above [AppearanceRoute]. */
@Serializable
data object QuickReactionRoute

/** The chats the forward and share sheets show first, pushed above [AppearanceRoute]. */
@Serializable
data object ChatsShownFirstRoute

/** What swiping a chat row does toward each side, pushed above [AppearanceRoute]. */
@Serializable
data object SwipeActionsRoute

/**
 * Global search of chats, contacts and messages (`REQUIREMENTS.md` FR-L6), `:feature:chats`, opened from the search
 * button of the navigation bar or rail.
 */
@Serializable
data object SearchRoute

/** The archived chats (`REQUIREMENTS.md` FR-L9), `:feature:chats`, pushed above the Chats tab from its archive row. */
@Serializable
data object ArchiveRoute

/**
 * The archive's settings (`REQUIREMENTS.md` FR-L9): which chats Telegram archives and keeps archived, and how the
 * "Archived chats" row shows in the chat list, `:feature:chats`, pushed above the archive from its top bar, and
 * reachable from any screen that navigates to it (e.g. a settings page).
 */
@Serializable
data object ArchiveSettingsRoute

/**
 * Starting a chat (`REQUIREMENTS.md` FR-L7): new group, new channel and new contact, then the contacts,
 * `:feature:chats`, pushed above the Chats tab from its "New chat" button or its empty state.
 */
@Serializable
data object NewChatRoute

/**
 * Creating a group (`REQUIREMENTS.md` FR-P4), `:feature:chats`: a nested graph pushed above the New chat screen, whose
 * steps share one view model, [NewGroupMembersRoute] first, then [NewGroupDetailsRoute]. The new group's conversation
 * takes the place of the New chat screen and the flow.
 */
@Serializable
data object NewGroupRoute

/** The first step of [NewGroupRoute]: picking the people to add from the contacts. */
@Serializable
data object NewGroupMembersRoute

/** The last step of [NewGroupRoute]: the group's name, photo and auto-delete time, and creating it. */
@Serializable
data object NewGroupDetailsRoute

/**
 * Creating a channel (`REQUIREMENTS.md` FR-P4), `:feature:chats`: a nested graph pushed above the New chat screen,
 * whose steps share one view model: [NewChannelDetailsRoute], which creates the channel, then [NewChannelTypeRoute] and
 * [NewChannelMembersRoute] in its place. The new channel's conversation takes the place of the New chat screen and the
 * flow.
 */
@Serializable
data object NewChannelRoute

/** The first step of [NewChannelRoute]: the channel's name, description and photo, and creating it. */
@Serializable
data object NewChannelDetailsRoute

/** The second step of [NewChannelRoute]: public with a username, or private with an invite link. */
@Serializable
data object NewChannelTypeRoute

/** The last step of [NewChannelRoute]: adding subscribers from the contacts, which may be skipped. */
@Serializable
data object NewChannelMembersRoute

/**
 * The conversation of chat [chatId] (`REQUIREMENTS.md` FR-C1–C8), `:feature:chat`, pushed above the Chats tab from a
 * row of the chat list, the archive or search. With [messageId] (a message found by search) it opens at that message,
 * highlighted; without, at the newest messages or the first unread one. A forum without [topicId] shows its topics;
 * with it, the conversation of that topic (FR-C9). With [threadId], the thread of replies to that message of the chat
 * (a channel post's comments, in its discussion group).
 */
@Serializable
data class ChatRoute(
    val chatId: Long,
    val messageId: Long? = null,
    val topicId: Int? = null,
    val threadId: Long? = null,
)

/**
 * The full-screen media viewer (`REQUIREMENTS.md` FR-C10), `:feature:media`, opened on the photo, video or GIF
 * [messageId] of chat [chatId] from the conversation, and paging through the chat's media of that kind. It is pushed
 * above everything: no navigation bar or rail, and full screen on top of the list-detail layout of the Chats tab.
 */
@Serializable
data class MediaViewerRoute(
    val chatId: Long,
    val messageId: Long,
)

/**
 * The full-screen stories viewer, read only (`docs/ROADMAP.md` M6), `:feature:media`, opened on the active stories of
 * chat [chatId] from the rings above the chat list: at story [storyId], or without one at the first the user has not
 * seen. Swiping sideways moves to the other chats of the stories list. Like the media viewer, it is pushed above
 * everything, without the navigation bar or rail.
 */
@Serializable
data class StoryViewerRoute(
    val chatId: Long,
    val storyId: Int? = null,
)

/**
 * Posting a story (`docs/adr/0036-posting-stories.md`), `:feature:media`: the photos and videos picked with Android's
 * photo picker or taken with the camera app, reordered and sent on to the composer. Full screen, like the stories
 * viewer, without the navigation bar or rail.
 */
@Serializable
data object NewStoryRoute

/**
 * The forward sheet (`REQUIREMENTS.md` FR-C6) for messages [messageIds] of chat [fromChatId], `:feature:chat`: a
 * dialog destination above the screen it is opened from, which stays visible below the sheet. The media viewer opens
 * it for its current item; the conversation shows the same sheet in place, for its own messages.
 */
@Serializable
data class ForwardRoute(
    val fromChatId: Long,
    val messageIds: List<Long>,
)

/**
 * The share sheet (`REQUIREMENTS.md` FR-N2, direct share) for what another app shared, or a `t.me/share` link, kept in
 * memory under [shareId] (`ShareInbox` of `:feature:chat`): a dialog destination above the screen it is opened from,
 * which picks the chat or chats it goes to. After process death the share is gone and the sheet closes at once.
 */
@Serializable
data class ShareRoute(
    val shareId: Long,
)

/**
 * Exporting the history of chat [chatId] (`REQUIREMENTS.md` §5.9, `docs/adr/0021-chat-export.md`), `:feature:chat`: a
 * settings-style page pushed above the profile or the info page it is opened from, with the shared axis on X, where the
 * user picks the format, the media, the dates and the folder, then follows the export running in the background.
 */
@Serializable
data class ChatExportRoute(
    val chatId: Long,
)

/**
 * The scheduled messages of chat [chatId] (`REQUIREMENTS.md` FR-C5), `:feature:chat`: pushed above the conversation
 * whose "3 scheduled messages" bar opens it, with the shared axis on X, as bubbles under their send days, each to send
 * now, reschedule, edit, copy or delete.
 */
@Serializable
data class ScheduledMessagesRoute(
    val chatId: Long,
)

/**
 * The profile of user [userId] (`REQUIREMENTS.md` FR-P2), `:feature:profile`: photos, bio, usernames, phone number and
 * birthday, sending a message, a secret chat, notifications, blocking and reporting, and the media shared in the
 * private chat. Pushed above the screen it is opened from (the conversation's title, a sender's avatar or name, a
 * mention), with the shared axis on X.
 */
@Serializable
data class ProfileRoute(
    val userId: Long,
)

/**
 * The music of user [userId] (`REQUIREMENTS.md` FR-P2), `:feature:profile`: every track of the profile, the one
 * playing, play and pause, the previous and next track, seeking, shuffle and repeat. Pushed above the profile whose
 * card opens it, or above the screen the mini player is tapped on.
 */
@Serializable
data class PlayerRoute(
    val userId: Long,
)

/**
 * The info page of group or channel [chatId] (`REQUIREMENTS.md` FR-P3, FR-P4), `:feature:profile`: photo, title,
 * description and link, mute, the members, the shared media, administration for those whose rights allow it, and
 * leaving, deleting or clearing the history. Pushed above the conversation from its title, with the shared axis on X.
 */
@Serializable
data class ChatInfoRoute(
    val chatId: Long,
)

/**
 * The members of group or channel [chatId], pushed above its info page: [filter] is the name of a `MemberFilter` of
 * `:core:domain` (every member, administrators, restricted or removed users), which this module does not depend on.
 */
@Serializable
data class ChatMembersRoute(
    val chatId: Long,
    val filter: String,
)

/**
 * What member [userId] of group or channel [chatId] may do, pushed above the members: their administrator rights, or
 * their restrictions ([restrict]).
 */
@Serializable
data class MemberRightsRoute(
    val chatId: Long,
    val userId: Long,
    val restrict: Boolean,
)

/** Editing the title and description of group or channel [chatId], pushed above its info page. */
@Serializable
data class EditChatInfoRoute(
    val chatId: Long,
)

/** What members of group [chatId] may do by default, and its slow mode, pushed above its info page. */
@Serializable
data class ChatPermissionsRoute(
    val chatId: Long,
)

/**
 * The settings of group or channel [chatId] (reactions, saving content, signatures, topics, the discussion group,
 * upgrading), pushed above its info page.
 */
@Serializable
data class ChatSettingsRoute(
    val chatId: Long,
)

/** The requests to join group or channel [chatId], pushed above its info page. */
@Serializable
data class JoinRequestsRoute(
    val chatId: Long,
)

/** The recent actions of supergroup or channel [chatId], for its administrators, pushed above its info page. */
@Serializable
data class RecentActionsRoute(
    val chatId: Long,
)

/** The statistics of supergroup or channel [chatId], for its administrators, pushed above its info page. */
@Serializable
data class ChatStatisticsRoute(
    val chatId: Long,
)

/** The invite links of group or channel [chatId], pushed above its info page. */
@Serializable
data class InviteLinksRoute(
    val chatId: Long,
)

/** Creating an invite link of group or channel [chatId], or editing [link], pushed above its invite links. */
@Serializable
data class EditInviteLinkRoute(
    val chatId: Long,
    val link: String? = null,
)

/**
 * The join sheet of invite link [inviteLink] (`REQUIREMENTS.md` FR-P5), `:feature:profile`: a dialog destination above
 * the screen the link was opened from, as the forward sheet is. It shows the chat and joins only on "Join".
 */
@Serializable
data class ChatInviteRoute(
    val inviteLink: String,
) {
    override fun toString() = "ChatInviteRoute(…)"
}

/** The boosts of channel or supergroup [chatId], read only (`REQUIREMENTS.md` FR-P5), pushed above its info page. */
@Serializable
data class ChatBoostRoute(
    val chatId: Long,
)

/**
 * The QR code scanner (`REQUIREMENTS.md` FR-P5, `docs/adr/0012-camerax-and-zxing-qr-scanner.md`), `:feature:scanner`,
 * full screen above the screen it is opened from: the Contacts tab, the user's own QR code, or Active sessions'
 * "Link desktop device", whose [purpose] is the name of a `ScanPurpose` (a login code only), which this module does
 * not depend on.
 */
@Serializable
data class QrScannerRoute(
    val purpose: String,
)

/**
 * Logging in another device with QR code login link [link] (`tg://login?token=…`), `:feature:settings`: a settings page
 * that asks before confirming it, then shows the new session.
 */
@Serializable
data class QrLoginRoute(
    val link: String,
) {
    override fun toString() = "QrLoginRoute(…)"
}

/** The user's own QR code, to share their profile, `:feature:settings`: pushed above the screen it is opened from. */
@Serializable
data object MyQrCodeRoute

/**
 * "Send feedback" (`docs/TESTING_GUIDE.md`), `:feature:feedback`: the report of the build, the device, the last crash
 * and TDLib's errors, which the user shares themselves; pushed above Settings → About.
 */
@Serializable
data object FeedbackRoute

/**
 * Recent calls (`docs/ARCHITECTURE.md`, Calls), `:feature:calls`: the user's one-to-one calls, newest first, to call
 * back; pushed above the screen it is opened from (Settings home, the Contacts tab).
 */
@Serializable
data object RecentCallsRoute
