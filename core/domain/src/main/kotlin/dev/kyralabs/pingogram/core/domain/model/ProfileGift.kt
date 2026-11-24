package dev.kyralabs.pingogram.core.domain.model

import kotlinx.collections.immutable.ImmutableList

/**
 * A visual asset belonging to a collectible gift.
 *
 * The file itself is managed by TDLib. Domain keeps only the stable
 * identifier and dimensions, never a TDLib object.
 */
data class ProfileGiftAsset(
    val fileId: Int,
    val width: Int = 0,
    val height: Int = 0,
)

/**
 * One gift shown on a user's profile.
 *
 * A regular gift has a Star price.
 * A unique gift is a collectible/NFT with model, symbol and backdrop data.
 */
data class ProfileGift(
    val id: String,

    /**
     * True when this is a unique collectible gift.
     */
    val isUnique: Boolean,

    val title: String,
    val number: Int,
    val starCount: Long,

    /**
     * Human-readable collectible attributes.
     */
    val model: String = "",
    val symbol: String = "",
    val backdrop: String = "",

    /**
     * Actual TDLib files used to render the collectible.
     */
    val modelAsset: ProfileGiftAsset? = null,
    val symbolAsset: ProfileGiftAsset? = null,

    /**
     * Backdrop colors in packed RGB format.
     * The UI converts them to Compose Color/Brush.
     */
    val backdropColors: ImmutableList<Long> = kotlinx.collections.immutable.persistentListOf(),

    val date: Int = 0,
    val text: String = "",
)

/**
 * A page of gifts received by a user.
 */
data class ProfileGiftsPage(
    val gifts: ImmutableList<ProfileGift>,
    val totalCount: Int,
    val nextOffset: String,
)
