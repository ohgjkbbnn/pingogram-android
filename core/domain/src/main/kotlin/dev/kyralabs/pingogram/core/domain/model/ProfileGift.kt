package dev.kyralabs.pingogram.core.domain.model

import kotlinx.collections.immutable.ImmutableList

/**
 * One gift a user shows: a regular gift (its Star price) or a unique NFT gift (its title, number, model, pattern and
 * backdrop). [date] is the Unix time it was sent, [text] the note sent with it.
 */
data class ProfileGift(
    val id: String,
    val isUnique: Boolean,
    val title: String,
    val number: Int,
    val starCount: Long,
    val model: String = "",
    val symbol: String = "",
    val backdrop: String = "",
    val date: Int = 0,
    val text: String = "",
)

/** A page of the gifts a user received (td_api.tl `getReceivedGifts`), with the offset of the next one. */
data class ProfileGiftsPage(
    val gifts: ImmutableList<ProfileGift>,
    val totalCount: Int,
    val nextOffset: String,
)
