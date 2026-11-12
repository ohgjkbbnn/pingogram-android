package dev.kyralabs.pingogram.feature.profile.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kyralabs.pingogram.core.ui.text.bidiIsolated
import dev.kyralabs.pingogram.core.ui.text.errorMessage
import dev.kyralabs.pingogram.core.ui.text.isDebuggable
import dev.kyralabs.pingogram.core.ui.text.resolve
import dev.kyralabs.pingogram.feature.profile.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * The profile of the route's user: hoists its [ProfileViewModel] and handles its [ProfileEffect]s. Chats open through
 * [onOpenChat] (the private chat, a secret chat, a group a mention named), other profiles through [onOpenProfile]; the
 * shared media's photos, videos and GIFs open in the media viewer through [onOpenMedia], its files, songs and voice
 * messages in the chat at their message through [onOpenMessage], and its links in the browser. The app navigates with
 * these, as features do not know each other. [onEditContact], where given, opens the contact editor for the user;
 * [onExportChat], where given, the export page of the private chat (`docs/adr/0021-chat-export.md`).
 * Errors and what was done are snackbars; copied text goes to the clipboard (with a snackbar before Android 13, which
 * confirms copies itself), a phone number to the dialer (`ACTION_DIAL`, no permission), a username's link to the
 * system's share sheet. The dialogs, the note dialog and the report sheet show over the screen while the view model has
 * one.
 */
@Suppress("LongParameterList") // The app's navigation from the profile.
@Composable
internal fun ProfileRoute(
    onBack: (() -> Unit)?,
    onOpenChat: (chatId: Long) -> Unit,
    onOpenProfile: (userId: Long) -> Unit,
    onOpenMedia: (chatId: Long, messageId: Long) -> Unit,
    onOpenMessage: (chatId: Long, messageId: Long) -> Unit,
    onOpenPlayer: (userId: Long) -> Unit,
    modifier: Modifier = Modifier,
    onEditContact: ((userId: Long) -> Unit)? = null,
    onExportChat: ((chatId: Long) -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val media by viewModel.media.collectAsStateWithLifecycle()
    val reportSheet by viewModel.reportSheet.collectAsStateWithLifecycle()
    val noteEditor by viewModel.noteEditor.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    val currentOnOpenMedia by rememberUpdatedState(onOpenMedia)
    val currentOnOpenMessage by rememberUpdatedState(onOpenMessage)
    val mediaActions =
        remember(uriHandler) {
            SharedMediaActions(
                onOpenMedia = { chatId, messageId -> currentOnOpenMedia(chatId, messageId) },
                onOpenMessage = { chatId, messageId -> currentOnOpenMessage(chatId, messageId) },
                onOpenUrl = { url -> runCatching { uriHandler.openUri(url) } },
            )
        }
    ProfileEffects(viewModel.effects, snackbarHostState, onOpenChat, onOpenProfile)
    val userId = (state as? ProfileUiState.Content)?.header?.userId
    val exportChatId = (state as? ProfileUiState.Content)?.actions?.exportChatId
    ProfileScreen(
        state = state,
        media = media,
        onEvent = viewModel::onEvent,
        onMediaEvent = viewModel::onMediaEvent,
        onBack = onBack,
        mediaActions = mediaActions,
        modifier = modifier,
        onEditContact = if (onEditContact != null && userId != null) ({ onEditContact(userId) }) else null,
        onExportChat = if (onExportChat != null && exportChatId != null) ({ onExportChat(exportChatId) }) else null,
        onOpenPlayer = onOpenPlayer,
        snackbarHostState = snackbarHostState,
        showMusic = true,
        showGifts = true,
    )
    ProfileDialogs((state as? ProfileUiState.Content)?.dialog, viewModel::onEvent)
    reportSheet?.let { ReportProfileSheet(it, viewModel.reportComment, viewModel::onEvent) }
    noteEditor?.let { ContactNoteDialog(it, viewModel::onEvent) }
}

/**
 * Handles [effects]: chats and profiles open through [onOpenChat] and [onOpenProfile]; copied text goes to the
 * clipboard through [copyText], a phone number to the dialer through [dial], a link to the share sheet through
 * [share]; errors, blocking and reporting are snackbars, the newest replacing the one shown.
 */
@Suppress("LongParameterList") // The effects, the snackbars, the navigation, and the platform actions tests replace.
@Composable
internal fun ProfileEffects(
    effects: Flow<ProfileEffect>,
    snackbarHostState: SnackbarHostState,
    onOpenChat: (chatId: Long) -> Unit,
    onOpenProfile: (userId: Long) -> Unit,
    copyText: Context.(text: String) -> Unit = Context::copyPlainText,
    dial: Context.(phoneNumber: String) -> Boolean = Context::dial,
    share: Context.(text: String) -> Unit = Context::shareText,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val showDetails = isDebuggable()
    val currentOnOpenChat by rememberUpdatedState(onOpenChat)
    val currentOnOpenProfile by rememberUpdatedState(onOpenProfile)
    LaunchedEffect(effects) {
        var snackbar: Job? = null
        effects.collect { effect ->
            val message =
                when (effect) {
                    is ProfileEffect.OpenChat -> {
                        currentOnOpenChat(effect.chatId)
                        ""
                    }

                    is ProfileEffect.OpenProfile -> {
                        currentOnOpenProfile(effect.userId)
                        ""
                    }

                    is ProfileEffect.ShowError -> {
                        errorMessage(resources, effect.error, showDetails)
                    }

                    is ProfileEffect.CopyText -> {
                        context.copyText(effect.text)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "" else resources.copied(effect.what)
                    }

                    is ProfileEffect.Dial -> {
                        if (context.dial(effect.phoneNumber)) "" else resources.getString(R.string.profile_no_dialer)
                    }

                    is ProfileEffect.Share -> {
                        context.share(effect.text)
                        ""
                    }

                    is ProfileEffect.BlockChanged,
                    is ProfileEffect.BirthdaySuggested,
                    ProfileEffect.Reported,
                    ProfileEffect.ReportNeedsMessages,
                    -> {
                        resources.notice(effect)
                    }
                }
            if (message.isNotEmpty()) {
                // Cancelling showSnackbar dismisses its snackbar.
                snackbar?.cancel()
                snackbar = launch { snackbarHostState.showSnackbar(message) }
            }
        }
    }
}

/** What a snackbar says after blocking, suggesting a birthday or reporting; empty for the other effects. */
private fun Resources.notice(effect: ProfileEffect): String =
    when (effect) {
        is ProfileEffect.BlockChanged -> {
            val name = effect.name.resolve(this).bidiIsolated()
            getString(if (effect.blocked) R.string.profile_blocked else R.string.profile_unblocked, name).bidiIsolated()
        }

        is ProfileEffect.BirthdaySuggested -> {
            getString(R.string.profile_birthday_suggested, effect.name.bidiIsolated()).bidiIsolated()
        }

        ProfileEffect.Reported -> {
            getString(R.string.profile_reported)
        }

        ProfileEffect.ReportNeedsMessages -> {
            getString(R.string.profile_report_needs_messages)
        }

        else -> {
            ""
        }
    }

/** What a snackbar says after copying, before Android 13. */
private fun Resources.copied(what: CopiedText): String =
    getString(
        when (what) {
            CopiedText.Username -> R.string.profile_username_copied
            CopiedText.Phone -> R.string.profile_phone_copied
            CopiedText.Bio -> R.string.profile_bio_copied
            CopiedText.Id -> R.string.profile_id_copied
        },
    )

/** Puts [text] on the clipboard as plain text, labelled for the system's clipboard preview. */
internal fun Context.copyPlainText(text: String) {
    val clipboard = getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.profile_copy_label), text))
}

/** Opens the dialer with [phoneNumber] (`ACTION_DIAL`, which needs no permission); false if no app dials. */
internal fun Context.dial(phoneNumber: String): Boolean =
    try {
        startActivity(Intent(Intent.ACTION_DIAL, "tel:$phoneNumber".toUri()))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

/** Shares [text] through the system's share sheet. */
internal fun Context.shareText(text: String) {
    val send =
        Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(send, getString(R.string.profile_share_title)))
}
