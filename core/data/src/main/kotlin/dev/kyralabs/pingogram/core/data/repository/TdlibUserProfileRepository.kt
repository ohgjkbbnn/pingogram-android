package dev.kyralabs.pingogram.core.data.repository

import dev.kyralabs.pingogram.core.common.result.Outcome
import dev.kyralabs.pingogram.core.common.result.map
import dev.kyralabs.pingogram.core.data.di.AccountScoped
import dev.kyralabs.pingogram.core.data.mapper.toAudio
import dev.kyralabs.pingogram.core.data.mapper.toBirthdate
import dev.kyralabs.pingogram.core.data.mapper.toFormattedText
import dev.kyralabs.pingogram.core.data.mapper.toPhoto
import dev.kyralabs.pingogram.core.data.mapper.toTdBirthdate
import dev.kyralabs.pingogram.core.domain.model.Birthdate
import dev.kyralabs.pingogram.core.domain.model.FormattedText
import dev.kyralabs.pingogram.core.domain.model.ProfileAudiosPage
import dev.kyralabs.pingogram.core.domain.model.UserFullInfo
import dev.kyralabs.pingogram.core.domain.model.UserProfilePhoto
import dev.kyralabs.pingogram.core.domain.model.UserProfilePhotosPage
import dev.kyralabs.pingogram.core.domain.repository.UserProfileRepository
import dev.kyralabs.pingogram.core.tdlib.TdlibClient
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

/**
 * [UserProfileRepository] on TDLib: `getUserFullInfo` and its `updateUserFullInfo`s, and `getUserProfilePhotos`.
 * Nothing is cached, so it is no `TdlibUpdateCache`: TDLib keeps the full info of every user it was asked for and
 * reports each change with `updateUserFullInfo`, which [fullInfo] follows while collected.
 *
 * - **Full info**: the updates are followed before the request goes out, so a change in between is not missed. TDLib
 *   sends an update before the answer it causes, so the answer is never older than an update before it.
 * - **Photos**: `getUserProfilePhotos` pages by offset; TDLib announces their files with the answer.
 * - **Birthday suggestions**: `suggestUserBirthdate`, which sends the user a `messageSuggestBirthdate`.
 * - **Note**: `setUserNote` with plain text (the note's formatting, which other apps may set, is not kept once edited
 *   here); TDLib then sends the new full info.
 *
 * Nothing about a user (bio, birthday, photos, note) is logged.
 */
@AccountScoped
class TdlibUserProfileRepository
    @Inject
    constructor(
        private val client: TdlibClient,
    ) : UserProfileRepository {
        override fun fullInfo(userId: Long): Flow<Outcome<UserFullInfo>> =
            channelFlow {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    client.updates
                        .filterIsInstance<TdApi.UpdateUserFullInfo>()
                        .filter { it.userId == userId && it.userFullInfo != null }
                        .collect { send(Outcome.Success(it.userFullInfo.toUserFullInfo())) }
                }
                send(client.request(TdApi.GetUserFullInfo(userId)).map { it.toUserFullInfo() })
            }

        override suspend fun setNote(
            userId: Long,
            note: String,
        ): Outcome<Unit> {
            val text = TdApi.FormattedText(note.trim(), emptyArray())
            return client.request(TdApi.SetUserNote(userId, text)).map {}
        }

        override suspend fun suggestBirthdate(
            userId: Long,
            birthdate: Birthdate,
        ): Outcome<Unit> = client.request(TdApi.SuggestUserBirthdate(userId, birthdate.toTdBirthdate())).map {}

        override suspend fun profilePhotos(
            userId: Long,
            offset: Int,
            limit: Int,
        ): Outcome<UserProfilePhotosPage> =
            client.request(TdApi.GetUserProfilePhotos(userId, offset, limit)).map { found ->
                UserProfilePhotosPage(
                    photos =
                        found.photos
                            .filterNotNull()
                            .map { UserProfilePhoto(it.id, it.toPhoto(), it.addedDate.toLong()) }
                            .toImmutableList(),
                    totalCount = found.totalCount.coerceAtLeast(0),
                )
            }

        override suspend fun profileAudios(
            userId: Long,
            offset: Int,
            limit: Int,
        ): Outcome<ProfileAudiosPage> =
            client.request(TdApi.GetUserProfileAudios(userId, offset, limit)).map { found ->
                ProfileAudiosPage(
                    audios = found.audios.filterNotNull().map { it.toAudio() }.toImmutableList(),
                    totalCount = found.totalCount.coerceAtLeast(0),
                )
            }
    }

/** The full info as the profile shows it; a year of 0 means the user shares only the day and month. */
private fun TdApi.UserFullInfo.toUserFullInfo(): UserFullInfo =
    UserFullInfo(
        bio = bio?.toFormattedText() ?: FormattedText.Empty,
        birthdate = birthdate?.toBirthdate(),
        isBlocked = blockList is TdApi.BlockListMain,
        botDescription = botInfo?.description?.takeIf { it.isNotBlank() },
        groupsInCommon = groupInCommonCount.coerceAtLeast(0),
        canBeCalled = canBeCalled,
        supportsVideoCalls = supportsVideoCalls,
        personalChatId = personalChatId,
        note = note?.toFormattedText() ?: FormattedText.Empty,
    )
