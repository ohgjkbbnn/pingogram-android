package dev.kyralabs.pingogram.core.domain.model

import kotlinx.collections.immutable.ImmutableList

/**
 * What a user's profile shows beyond their [User] (`REQUIREMENTS.md` FR-P2, `UserProfileRepository`), as TDLib's
 * `userFullInfo` has it: what the user wrote about themselves, their birthday, whether the current user blocked them,
 * and a bot's description. TDLib reads it from the server when first asked, then keeps it and reports its changes.
 *
 * `toString()` leaves the bio, the birthday, the description and the note out, so it can be logged.
 *
 * @property bio the user's bio with its links and mentions; empty without one, and always for bots.
 * @property birthdate the birthday the user shares with the current user; null if they share none.
 * @property isBlocked the user is on the current user's main block list (`BlockListRepository`).
 * @property botDescription what a bot says about itself, for its profile; null for users who are no bots.
 * @property groupsInCommon how many groups and channels the current user shares with the user.
 * @property canBeCalled the user's privacy settings let the current user call them (td_api.tl `can_be_called`).
 * @property supportsVideoCalls the user's app takes video calls (td_api.tl `supports_video_calls`).
 * @property personalChatId the channel the user shows on their profile (td_api.tl `personal_chat_id`); 0 for none.
 * @property note what the current user wrote about this contact for themselves (td_api.tl `userFullInfo.note`): only
 *   they see it, and it stays with their contacts on all their devices; empty without one.
 */
data class UserFullInfo(
    val bio: FormattedText,
    val birthdate: Birthdate?,
    val isBlocked: Boolean,
    val botDescription: String?,
    val groupsInCommon: Int,
    val canBeCalled: Boolean = false,
    val supportsVideoCalls: Boolean = false,
    val personalChatId: Long = 0,
    val note: FormattedText = FormattedText.Empty,
) {
    override fun toString() = "UserFullInfo(isBlocked=$isBlocked, groupsInCommon=$groupsInCommon, …)"
}

/**
 * A birthday: [day] of [month] (1–12), and the [year] if the user shares it.
 *
 * `toString()` leaves it out.
 */
data class Birthdate(
    val day: Int,
    val month: Int,
    val year: Int?,
) {
    override fun toString() = "Birthdate(…)"
}

/**
 * One of a user's profile photos, the current one first (`UserProfileRepository.profilePhotos`): its [photo] in the
 * sizes Telegram keeps (160 and 640 px, and more for newer photos) with a minithumbnail; [addedDate] when it was set
 * (Unix time, seconds). Animated profile photos show their still picture.
 */
data class UserProfilePhoto(
    val id: Long,
    val photo: Photo,
    val addedDate: Long,
)

/**
 * A page of a user's profile photos, newest first; [totalCount] is how many the user has, as far as the current user
 * may see them.
 */
data class UserProfilePhotosPage(
    val photos: ImmutableList<UserProfilePhoto>,
    val totalCount: Int,
)

/**
 * The music a user chose for their profile (td_api.tl `getUserProfileAudios`): [audios] after the first offset, and
 * how many there are in all ([totalCount]) so the list knows when to stop asking for more.
 */
data class ProfileAudiosPage(
    val audios: ImmutableList<Audio>,
    val totalCount: Int,
)

/** Who has a public username (`SearchRepository.publicChat`): a user (or bot), or a group or channel. */
sealed interface UsernameOwner {
    /** User [userId], whose private chat is [chatId]. */
    data class User(
        val userId: Long,
        val chatId: Long,
    ) : UsernameOwner

    /** The group or channel [chatId]. */
    data class Chat(
        val chatId: Long,
    ) : UsernameOwner
}
