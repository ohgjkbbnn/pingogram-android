package dev.kyralabs.pingogram.core.domain.model

import kotlinx.collections.immutable.ImmutableList

/** One gift a user shows: a regular gift (its Star price) or a unique NFT gift (its title and number). */
data class ProfileGift(
    val id: String,
    val isUnique: Boolean,
    val title: String,
    val number: Int,
    val starCount: Long,
)

/** A page of the gifts a user received (td_api.tl `getReceivedGifts`), with the offset of the next one. */
data class ProfileGiftsPage(
    val gifts: ImmutableList<ProfileGift>,
    val totalCount: Int,
    val nextOffset: String,
)
