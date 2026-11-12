package dev.kyralabs.pingogram.core.domain.repository

import dev.kyralabs.pingogram.core.common.result.Outcome
import dev.kyralabs.pingogram.core.domain.model.Birthdate
import dev.kyralabs.pingogram.core.domain.model.ProfileAudiosPage
import dev.kyralabs.pingogram.core.domain.model.ProfileGiftsPage
import dev.kyralabs.pingogram.core.domain.model.UserFullInfo
import dev.kyralabs.pingogram.core.domain.model.UserProfilePhotosPage
import kotlinx.coroutines.flow.Flow

/**
 * Other users' profiles (`REQUIREMENTS.md` FR-P2): what TDLib knows about a user beyond their `User` (bio, birthday,
 * block state, a bot's description, the current user's note about a contact) and their profile photos, and setting
 * that note; a secret chat with them starts through `SecretChatRepository`. The logged-in user's own profile, which
 * they edit, is `ProfileRepository`'s. Nothing is cached here: TDLib keeps the full info itself and reports its
 * changes.
 *
 * Requests that get no answer in time fail with `DomainError.Network`; a user TDLib does not know (or may not reach)
 * fails with `DomainError.NotFound`.
 */
interface UserProfileRepository {
    /**
     * The full info of user [userId] (TDLib asks the server the first time, answers from memory after), then the info
     * again after every change TDLib reports, while collected: a bio edited on another device, the user blocked or
     * unblocked here or elsewhere. A failed first request is the flow's first value; changes still follow it.
     */
    fun fullInfo(userId: Long): Flow<Outcome<UserFullInfo>>

    /**
     * Sets the note the current user keeps about contact [userId] to [note], plain text of at most
     * `ProfileLimits.NOTE_LENGTH_MAX` characters; an empty one removes it. Only the current user sees it, on all their
     * devices; TDLib reports the new full info with `updateUserFullInfo`, which [fullInfo] follows.
     */
    suspend fun setNote(
        userId: Long,
        note: String,
    ): Outcome<Unit>

    /**
     * Suggests [birthdate] to user [userId], who has none on their profile: Telegram sends them a message they can
     * accept, keeping it on their profile, or decline.
     */
    suspend fun suggestBirthdate(
        userId: Long,
        birthdate: Birthdate,
    ): Outcome<Unit>

    /** Up to [limit] (at most 100) of user [userId]'s profile photos, after the first [offset] of them. */
    suspend fun profilePhotos(
        userId: Long,
        offset: Int = 0,
        limit: Int = PHOTOS_PAGE_SIZE,
    ): Outcome<UserProfilePhotosPage>

    /**
     * Up to [limit] (at most 100) of the music user [userId] chose for their profile, after the first [offset] of them.
     * The first page feeds the profile's card, later ones the player's list as it is scrolled.
     */
    suspend fun profileAudios(
        userId: Long,
        offset: Int = 0,
        limit: Int = PROFILE_AUDIOS_PAGE_SIZE,
    ): Outcome<ProfileAudiosPage>

    /** Up to [pageSize] of the gifts user [userId] received, from [pageOffset] (empty for the first page). */
    suspend fun profileGifts(
        userId: Long,
        pageOffset: String = "",
        pageSize: Int = PROFILE_GIFTS_PAGE_SIZE,
    ): Outcome<ProfileGiftsPage>

    companion object {
        /** The gifts the profile shows at first. */
        const val PROFILE_GIFTS_PAGE_SIZE = 12

        /** The profile photos a page holds: enough for the carousel's first screens. */
        const val PHOTOS_PAGE_SIZE = 20

        /** The profile music a page holds: the profile's card shows the first few, the player lists the rest. */
        const val PROFILE_AUDIOS_PAGE_SIZE = 20
    }
}
