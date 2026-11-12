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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
 * The profile's gifts section: a title and the gifts in two columns of Material 3 tiles. An NFT gift is a tertiary
 * tile with a cookie badge and its number; a regular gift is a secondary tile with its Star price. Shows nothing when
 * the user has no gifts.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ProfileGiftsSection(
    modifier: Modifier = Modifier,
    viewModel: ProfileGiftsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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
                pair.forEach { gift -> GiftTile(gift, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GiftTile(
    gift: ProfileGift,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container = if (gift.isUnique) colors.tertiaryContainer else colors.secondaryContainer
    val onContainer = if (gift.isUnique) colors.onTertiaryContainer else colors.onSecondaryContainer
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        contentColor = onContainer,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
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
