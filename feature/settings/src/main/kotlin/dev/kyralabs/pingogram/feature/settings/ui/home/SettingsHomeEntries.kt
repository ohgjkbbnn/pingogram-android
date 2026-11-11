package dev.kyralabs.pingogram.feature.settings.ui.home

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.kyralabs.pingogram.feature.settings.R
import dev.kyralabs.pingogram.feature.settings.ui.OtherAccountUiModel
import dev.kyralabs.pingogram.feature.settings.ui.SettingsPage
import dev.kyralabs.pingogram.feature.settings.ui.SettingsPageSelection
import dev.kyralabs.pingogram.feature.settings.ui.SettingsSubPage
import kotlinx.collections.immutable.ImmutableList
import java.text.Normalizer
import java.util.Locale
import dev.kyralabs.pingogram.core.ui.R as UiR

// The one registry of Settings home's entries (docs/DESIGN_SYSTEM.md §13.9): its groups and
// what settings search finds.

/** Where an entry of Settings home leads: a settings page, or Recent calls, a page of another feature. */
internal sealed interface HomeDestination {
    data class Page(
        val page: SettingsPage,
    ) : HomeDestination

    data object RecentCalls : HomeDestination

    /** A page above a page of Settings home, opened with that page below it (settings search only). */
    data class SubPage(
        val subPage: SettingsSubPage,
    ) : HomeDestination
}

/** An entry: where it leads ([destination]), its [title], [summary] and [icon]. */
internal class HomeEntry(
    val destination: HomeDestination,
    @param:StringRes val title: Int,
    @param:StringRes val summary: Int,
    @param:DrawableRes val icon: Int,
)

/** A group of entries under its [title]. */
internal class HomeGroup(
    @param:StringRes val title: Int,
    val entries: List<HomeEntry>,
)

internal fun page(
    page: SettingsPage,
    @StringRes title: Int,
    @StringRes summary: Int,
    @DrawableRes icon: Int,
) = HomeEntry(HomeDestination.Page(page), title, summary, icon)

/**
 * Settings home's groups. Every page Settings home opens is in one of them:
 * "Make it yours", "Alerts and privacy", "More", and "App" for the rest. Owner decision to confirm (2026-11-07): the
 * grouping.
 */
internal val HomeGroups: List<HomeGroup> =
    listOf(
        HomeGroup(
            R.string.settings_home_make_it_yours,
            listOf(
                page(
                    SettingsPage.Appearance,
                    R.string.settings_appearance,
                    R.string.settings_appearance_summary,
                    UiR.drawable.ic_palette,
                ),
                page(
                    SettingsPage.ChatFolders,
                    R.string.settings_chat_folders,
                    R.string.settings_chat_folders_summary,
                    UiR.drawable.ic_folder,
                ),
                page(
                    SettingsPage.Stickers,
                    R.string.settings_home_stickers_emoji,
                    R.string.settings_stickers_summary,
                    UiR.drawable.ic_mood,
                ),
            ),
        ),
        HomeGroup(
            R.string.settings_home_alerts_privacy,
            listOf(
                page(
                    SettingsPage.Notifications,
                    R.string.settings_notifications,
                    R.string.settings_notifications_summary,
                    UiR.drawable.ic_notifications,
                ),
                page(
                    SettingsPage.PrivacySecurity,
                    R.string.settings_privacy_security,
                    R.string.settings_privacy_security_summary,
                    R.drawable.ic_shield,
                ),
                page(
                    SettingsPage.DataStorage,
                    R.string.settings_data_storage,
                    R.string.settings_data_storage_summary,
                    R.drawable.ic_storage,
                ),
            ),
        ),
        HomeGroup(
            R.string.settings_home_more,
            listOf(
                HomeEntry(
                    HomeDestination.RecentCalls,
                    R.string.settings_recent_calls,
                    R.string.settings_recent_calls_summary,
                    UiR.drawable.ic_history,
                ),
                page(
                    SettingsPage.ActiveSessions,
                    R.string.settings_home_devices,
                    R.string.settings_home_devices_summary,
                    R.drawable.ic_devices,
                ),
                page(
                    SettingsPage.Language,
                    R.string.settings_language,
                    R.string.settings_language_summary,
                    R.drawable.ic_language,
                ),
            ),
        ),
        HomeGroup(
            R.string.settings_home_app,
            listOf(
                page(
                    SettingsPage.PowerSaving,
                    R.string.settings_power_saving,
                    R.string.settings_power_saving_summary,
                    R.drawable.ic_energy_savings_leaf,
                ),
            ),
        ),
    )

/**
 * The entries of [entries] whose title or summary holds every word of [query], ignoring case and accents, in their
 * order; none for a blank query. [available] leaves out what this window cannot open (Recent calls without its page).
 */
internal fun searchEntries(
    entries: List<HomeEntry>,
    query: String,
    resources: Resources,
    locale: Locale,
    available: (HomeEntry) -> Boolean = { true },
): List<HomeEntry> {
    val words = normalized(query, locale).split(WHITESPACE).filter { it.isNotEmpty() }
    if (words.isEmpty()) return emptyList()
    return entries.filter { entry ->
        val text = resources.getString(entry.title) + " " + resources.getString(entry.summary)
        available(entry) && normalized(text, locale).let { normalizedText -> words.all { it in normalizedText } }
    }
}

private fun normalized(
    text: String,
    locale: Locale,
): String =
    Normalizer
        .normalize(text, Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .lowercase(locale)

private val COMBINING_MARKS = Regex("\\p{Mn}+")
private val WHITESPACE = Regex("\\s+")

/** What the account card's switch button offers: the device's other accounts, and "Add account" while [canAdd]. */
internal class AccountSwitcher(
    val accounts: ImmutableList<OtherAccountUiModel>,
    val canAdd: Boolean,
    val onSwitch: (slot: Int) -> Unit,
    val onAdd: () -> Unit,
)

/** What every row of the home shows besides its entry: the values beside rows, and the page open beside the list. */
internal class HomeRowState(
    val summaries: SettingsHomeSummaries,
    val selection: SettingsPageSelection?,
)
