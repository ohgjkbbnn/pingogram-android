package dev.kyralabs.pingogram.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.kyralabs.pingogram.core.common.result.Outcome
import dev.kyralabs.pingogram.core.domain.model.ProfileGift
import dev.kyralabs.pingogram.core.domain.repository.UserProfileRepository
import dev.kyralabs.pingogram.core.navigation.ProfileRoute
import dev.kyralabs.pingogram.core.ui.theme.spacing
import dev.kyralabs.pingogram.feature.profile.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/** The gifts a profile shows: the first page of the user's received gifts. */
data class ProfileGiftsState(
    val gifts: ImmutableList<ProfileGift> = persistentListOf(),
    val totalCount: Int = 0,
)

/** The profile's gifts (`UserProfileRepository.profileGifts`), loaded for the user the profile belongs to. */
@HiltViewModel
class ProfileGiftsViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val profileRepository: UserProfileRepository,
    ) : ViewModel() {
        private val userId = savedStateHandle.toRoute<ProfileRoute>().userId

        private val mutableState = MutableStateFlow(ProfileGiftsState())

        /** The first gifts and how many the user has in all. */
        val state: StateFlow<ProfileGiftsState> = mutableState.asStateFlow()

        init {
            viewModelScope.launch {
                // A failure leaves the section out.
                val result = profileRepository.profileGifts(userId, pageSize = SHOWN_GIFTS)
                if (result is Outcome.Success) {
                    mutableState.update {
                        it.copy(gifts = result.value.gifts, totalCount = result.value.totalCount)
                    }
                }
            }
        }

        private companion object {
            const val SHOWN_GIFTS = 12
        }
    }

/**
 * The profile's gifts section: a title and the gifts in two columns of Material 3 tiles. An NFT gift is a tertiary tile
 * with a cookie badge; a regular gift is a secondary tile with its Star price. A tap opens the gift's details in a
 * bottom sheet. Shows nothing when the user has no gifts.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileGiftsSection(
    modifier: Modifier = Modifier,
    viewModel: ProfileGiftsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<ProfileGift?>(null) }
    if (state.gifts.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.profile_section_gifts),
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = MaterialTheme.spacing.small),
        )
        state.gifts.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.spacing.small),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                pair.forEach { gift -> GiftTile(gift, Modifier.weight(1f)) { selected = gift } }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    selected?.let { gift ->
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            GiftDetails(gift)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GiftTile(
    gift: ProfileGift,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val container = if (gift.isUnique) colors.tertiaryContainer else colors.secondaryContainer
    val onContainer = if (gift.isUnique) colors.onTertiaryContainer else colors.onSecondaryContainer
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        contentColor = onContainer,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GiftBadge(gift, Modifier.size(56.dp))
            Spacer(Modifier.size(MaterialTheme.spacing.small))
            Text(
                text =
                    if (gift.isUnique) {
                        stringResource(R.string.gift_unique_title, gift.title, gift.number)
                    } else {
                        stringResource(R.string.gift_regular_title)
                    },
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    if (gift.isUnique) {
                        stringResource(R.string.gift_unique_label)
                    } else {
                        stringResource(R.string.gift_stars, gift.starCount)
                    },
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The gift's mark: a cookie for an NFT gift, a circle for a regular one. */
@Composable
private fun GiftBadge(
    gift: ProfileGift,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier =
            modifier
                .clip(if (gift.isUnique) MaterialShapes.Cookie9Sided.toShape() else CircleShape)
                .background(if (gift.isUnique) colors.tertiary else colors.secondary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (gift.isUnique) "\u25C6" else "\u2605",
            color = if (gift.isUnique) colors.onTertiary else colors.onSecondary,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

/** The details of a tapped gift: a large mark, its name, and a table of its facts, as Telegram shows them. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GiftDetails(gift: ProfileGift) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GiftBadge(gift, Modifier.size(120.dp))
        Spacer(Modifier.size(MaterialTheme.spacing.medium))
        Text(
            text =
                if (gift.isUnique) {
                    stringResource(R.string.gift_unique_title, gift.title, gift.number)
                } else {
                    stringResource(R.string.gift_regular_title)
                },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(MaterialTheme.spacing.medium))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                if (gift.isUnique) {
                    GiftDetailRow(stringResource(R.string.gift_detail_model), gift.model)
                    GiftDetailRow(stringResource(R.string.gift_detail_symbol), gift.symbol)
                    GiftDetailRow(stringResource(R.string.gift_detail_backdrop), gift.backdrop)
                    GiftDetailRow(stringResource(R.string.gift_detail_number), "#${gift.number}")
                } else {
                    GiftDetailRow(stringResource(R.string.gift_detail_price), stringResource(R.string.gift_stars, gift.starCount))
                }
                if (gift.date > 0) {
                    GiftDetailRow(
                        stringResource(R.string.gift_detail_date),
                        SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(gift.date * 1000L)),
                    )
                }
                if (gift.text.isNotBlank()) {
                    Text(
                        text = gift.text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = MaterialTheme.spacing.small),
                    )
                }
            }
        }
        Spacer(Modifier.size(MaterialTheme.spacing.medium))
    }
}

@Composable
private fun GiftDetailRow(
    label: String,
    value: String,
) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
