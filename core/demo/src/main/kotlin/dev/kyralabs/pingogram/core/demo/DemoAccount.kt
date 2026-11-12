package dev.kyralabs.pingogram.core.demo

import dev.kyralabs.pingogram.core.tdlib.TdResult
import org.drinkless.tdlib.TdApi
import java.util.Locale

/**
 * The demo's account and people (`docs/adr/0034-demo-mode-for-store-review.md`): the user's profile (name, bio,
 * username, birthday, photo, personal channel), contacts (added, edited, imported, removed), blocking, privacy rules,
 * sessions, websites and passkeys (sample ones, never real devices or password managers; adding a passkey needs a
 * Telegram account), a sample story of Ada's, and the countries the phone
 * number fields offer. It keeps this state itself; every change goes into the journal like the others.
 *
 * One branch per function, each starting with guard clauses for people that are not the demo's: hence the
 * suppressions.
 */
@Suppress("CyclomaticComplexMethod", "LongMethod", "TooManyFunctions", "ReturnCount", "LargeClass")
internal class DemoAccount : DemoArea {
    private val fullInfo = mutableMapOf<Long, TdApi.UserFullInfo>()
    private val privacy = mutableMapOf<String, TdApi.UserPrivacySettingRules>()
    private val sessions = mutableListOf<TdApi.Session>()
    private val websites = mutableListOf<TdApi.ConnectedWebsite>()
    private val passkeys = mutableListOf<TdApi.Passkey>()
    private val profilePhotos = mutableListOf<TdApi.ChatPhoto>()
    private var readDate = TdApi.ReadDatePrivacySettings(true)
    private var inactiveSessionDays = INACTIVE_SESSION_DAYS
    private var newChats = TdApi.NewChatPrivacySettings(true, 0)
    private var storyRead = false
    private var storyReaction: TdApi.ReactionType? = null
    private var started = false
    private var nextId = 1L

    override fun answer(
        world: DemoWorld,
        function: TdApi.Function<*>,
    ): DemoAnswer? {
        if (!started) start(world)
        return when (function) {
            is TdApi.GetUserFullInfo -> {
                info(world, function.userId)?.let(::ok) ?: notFound()
            }

            is TdApi.SetName -> {
                setName(world, function.firstName, function.lastName)
            }

            is TdApi.SetBio -> {
                changeMine(world) { bio = TdApi.FormattedText(function.bio, emptyArray()) }
            }

            is TdApi.SetBirthdate -> {
                changeMine(world) { birthdate = function.birthdate }
            }

            is TdApi.SetUserNote -> {
                setNote(world, function.userId, function.note)
            }

            is TdApi.SetPersonalChat -> {
                changeMine(world) { personalChatId = function.chatId }
            }

            is TdApi.GetSuitablePersonalChats -> {
                ok(TdApi.Chats(1, longArrayOf(DemoData.CHANNEL_CHAT)))
            }

            is TdApi.SetUsername -> {
                setUsername(world, function.username)
            }

            is TdApi.SetProfilePhoto -> {
                setPhoto(world, function.photo)
            }

            is TdApi.DeleteProfilePhoto -> {
                deletePhoto(world, function.profilePhotoId)
            }

            is TdApi.GetUserProfilePhotos -> {
                photos(function.userId, function.offset, function.limit)
            }

            is TdApi.GetUserProfileAudios -> {
                ok(TdApi.Audios().apply { totalCount = 0; audios = emptyArray() })
            }

            is TdApi.GetReceivedGifts -> {
                ok(TdApi.ReceivedGifts().apply { totalCount = 0; gifts = emptyArray(); nextOffset = "" })
            }

            is TdApi.GetUserLink -> {
                ok(userLink(world))
            }

            is TdApi.AddContact -> {
                addContact(world, function)
            }

            is TdApi.ImportContacts -> {
                importContacts(world, function.contacts.toList(), add = true)
            }

            is TdApi.ChangeImportedContacts -> {
                importContacts(world, function.contacts.toList(), add = false)
            }

            is TdApi.ClearImportedContacts -> {
                ok(TdApi.Ok())
            }

            is TdApi.RemoveContacts -> {
                removeContacts(world, function.userIds.toList())
            }

            is TdApi.SearchUserByPhoneNumber -> {
                userByPhone(world, function.phoneNumber)?.let(::ok) ?: notFound()
            }

            is TdApi.GetApplicationDownloadLink -> {
                ok(TdApi.HttpUrl(DOWNLOAD_LINK))
            }

            is TdApi.GetBlockedMessageSenders -> {
                blocked(world, function.offset, function.limit)
            }

            is TdApi.SetMessageSenderBlockList -> {
                block(world, function.senderId, function.blockList)
            }

            is TdApi.GetUserPrivacySettingRules -> {
                ok(rules(function.setting))
            }

            is TdApi.SetUserPrivacySettingRules -> {
                setRules(function.setting, function.rules)
            }

            is TdApi.GetReadDatePrivacySettings -> {
                ok(readDate)
            }

            is TdApi.SetReadDatePrivacySettings -> {
                okChanged(emptyList()).also { readDate = function.settings }
            }

            is TdApi.GetNewChatPrivacySettings -> {
                ok(newChats)
            }

            is TdApi.SetNewChatPrivacySettings -> {
                okChanged(emptyList()).also { newChats = function.settings }
            }

            is TdApi.GetActiveSessions -> {
                ok(TdApi.Sessions(sessions.toTypedArray(), inactiveSessionDays))
            }

            is TdApi.SetInactiveSessionTtl -> {
                okChanged(emptyList()).also { inactiveSessionDays = function.inactiveSessionTtlDays }
            }

            is TdApi.TerminateSession -> {
                terminate(function.sessionId)
            }

            is TdApi.TerminateAllOtherSessions -> {
                okChanged(emptyList()).also { sessions.removeAll { !it.isCurrent } }
            }

            is TdApi.ConfirmSession -> {
                okChanged(emptyList()).also {
                    sessions.find { it.id == function.sessionId }?.isUnconfirmed =
                        false
                }
            }

            is TdApi.ConfirmQrCodeAuthentication -> {
                linkDevice(world, function.link)
            }

            is TdApi.GetConnectedWebsites -> {
                ok(TdApi.ConnectedWebsites(websites.toTypedArray()))
            }

            is TdApi.DisconnectWebsite -> {
                okChanged(emptyList()).also { websites.removeAll { it.id == function.websiteId } }
            }

            is TdApi.DisconnectAllWebsites -> {
                okChanged(emptyList()).also { websites.clear() }
            }

            is TdApi.GetLoginPasskeys -> {
                ok(TdApi.Passkeys(passkeys.toTypedArray()))
            }

            is TdApi.RemoveLoginPasskey -> {
                okChanged(emptyList()).also { passkeys.removeAll { it.id == function.passkeyId } }
            }

            is TdApi.GetChatActiveStories -> {
                ok(chatStories(world, function.chatId))
            }

            is TdApi.GetStory -> {
                story(world, function.storyPosterChatId, function.storyId)
            }

            is TdApi.OpenStory -> {
                openStory(world, function.storyPosterChatId, function.storyId)
            }

            is TdApi.CloseStory -> {
                ok(TdApi.Ok())
            }

            is TdApi.GetStoryAvailableReactions -> {
                ok(storyReactions())
            }

            is TdApi.SetStoryReaction -> {
                reactToStory(world, function)
            }

            is TdApi.GetCountries -> {
                ok(TdApi.Countries(COUNTRIES.map(::country).toTypedArray()))
            }

            is TdApi.GetCountryCode -> {
                ok(TdApi.Text(countryCode()))
            }

            is TdApi.GetPhoneNumberInfo -> {
                ok(phoneNumberInfo(function.phoneNumberPrefix))
            }

            else -> {
                null
            }
        }
    }

    /** The sample sessions, websites, passkey and Ada's story, made once from the demo's start date. */
    private fun start(world: DemoWorld) {
        started = true
        val since = world.data.channel.date
        sessions +=
            session(true, "Android", TdApi.SessionDeviceTypeAndroid(), since, since)
        sessions +=
            session(
                false,
                "Android",
                TdApi.SessionDeviceTypeAndroid(),
                since + DAY,
                since + 2 * DAY,
            ).apply {
                deviceModel = "Demo tablet"
            }
        websites +=
            TdApi.ConnectedWebsite(
                nextId++,
                "example.org",
                DemoData.LOGIN_BOT,
                "Firefox",
                "Linux",
                since,
                since + DAY,
                "203.0.113.7",
                "Sample City",
            )
        passkeys += TdApi.Passkey("demo-passkey", "Sample password manager", since, since + DAY, 0)
    }

    /** A sample session: a made-up device at an address of the documentation range (RFC 5737), never a real one. */
    private fun session(
        current: Boolean,
        platform: String,
        type: TdApi.SessionDeviceType,
        logIn: Int,
        lastActive: Int,
    ) = TdApi.Session().apply {
        id = nextId++
        isCurrent = current
        canAcceptSecretChats = true
        canAcceptCalls = true
        deviceType = type
        applicationName = "Pingogram"
        applicationVersion = "demo"
        deviceModel = "This phone"
        this.platform = platform
        systemVersion = platform
        logInDate = logIn
        lastActiveDate = lastActive
        ipAddress = "192.0.2.${FIRST_ADDRESS + id}"
        location = "Sample City"
    }

    private fun info(
        world: DemoWorld,
        userId: Long,
    ): TdApi.UserFullInfo? {
        if (!world.users.containsKey(userId)) return null
        return fullInfo.getOrPut(userId) {
            world.data.userFullInfo(userId).apply {
                canBeCalled = userId != DemoData.ME && world.users[userId]?.type is TdApi.UserTypeRegular
                supportsVideoCalls = canBeCalled
                if (bio == null) bio = TdApi.FormattedText("", emptyArray())
            }
        }
    }

    private fun changeMine(
        world: DemoWorld,
        change: TdApi.UserFullInfo.() -> Unit,
    ): DemoAnswer {
        val mine = info(world, DemoData.ME) ?: return notFound()
        mine.change()
        return okChanged(listOf(TdApi.UpdateUserFullInfo(DemoData.ME, mine)))
    }

    private fun setName(
        world: DemoWorld,
        first: String,
        last: String,
    ): DemoAnswer {
        if (first.isBlank()) return badRequest("FIRSTNAME_INVALID")
        val me = world.users.getValue(DemoData.ME)
        me.firstName = first.trim()
        me.lastName = last.trim()
        val updates = mutableListOf<TdApi.Update>(TdApi.UpdateUser(me))
        world.chat(DemoData.ME)?.let { chat ->
            chat.title = "${me.firstName} ${me.lastName}".trim()
            updates += TdApi.UpdateChatTitle(chat.id, chat.title)
        }
        return okChanged(updates)
    }

    private fun setUsername(
        world: DemoWorld,
        username: String,
    ): DemoAnswer {
        if (username.isNotEmpty() && !USERNAME.matches(username)) return badRequest("USERNAME_INVALID")
        val me = world.users.getValue(DemoData.ME)
        val active = if (username.isEmpty()) emptyArray() else arrayOf(username)
        me.usernames = TdApi.Usernames(active, emptyArray(), username, emptyArray(), "")
        return okChanged(listOf(TdApi.UpdateUser(me)))
    }

    private fun setPhoto(
        world: DemoWorld,
        input: TdApi.InputChatPhoto?,
    ): DemoAnswer {
        val path =
            ((input as? TdApi.InputChatPhotoStatic)?.photo as? TdApi.InputFileLocal)?.path ?: return needsAccount()
        val file = world.sentFile(path)
        val id = nextId++
        profilePhotos.add(
            0,
            TdApi.ChatPhoto(
                id,
                world.now,
                null,
                arrayOf(TdApi.PhotoSize("x", file, PHOTO_SIZE, PHOTO_SIZE, intArrayOf())),
                null,
                null,
                null,
            ),
        )
        val me = world.users.getValue(DemoData.ME)
        me.profilePhoto = TdApi.ProfilePhoto(id, file, file, null, false, false)
        return okChanged(listOf(TdApi.UpdateUser(me)) + chatPhotoUpdate(world, me))
    }

    private fun deletePhoto(
        world: DemoWorld,
        id: Long,
    ): DemoAnswer {
        profilePhotos.removeAll { it.id == id }
        val me = world.users.getValue(DemoData.ME)
        me.profilePhoto =
            profilePhotos.firstOrNull()?.let { photo ->
                val file = photo.sizes.first().photo
                TdApi.ProfilePhoto(photo.id, file, file, null, false, false)
            }
        return okChanged(listOf(TdApi.UpdateUser(me)) + chatPhotoUpdate(world, me))
    }

    private fun chatPhotoUpdate(
        world: DemoWorld,
        me: TdApi.User,
    ): List<TdApi.Update> {
        val chat = world.chat(DemoData.ME) ?: return emptyList()
        chat.photo = me.profilePhoto?.let { TdApi.ChatPhotoInfo(it.small, it.big, null, false, false) }
        return listOf(TdApi.UpdateChatPhoto(chat.id, chat.photo))
    }

    private fun photos(
        userId: Long,
        offset: Int,
        limit: Int,
    ): DemoAnswer {
        val photos = if (userId == DemoData.ME) profilePhotos else emptyList()
        return ok(TdApi.ChatPhotos(photos.size, photos.drop(offset).take(limit).toTypedArray()))
    }

    private fun userLink(world: DemoWorld): TdApi.UserLink {
        val username =
            world.users
                .getValue(DemoData.ME)
                .usernames
                ?.editableUsername
                .orEmpty()
        return if (username.isNotEmpty()) {
            TdApi.UserLink(
                "https://t.me/$username",
                0,
            )
        } else {
            TdApi.UserLink(TEMPORARY_LINK, DAY)
        }
    }

    private fun addContact(
        world: DemoWorld,
        function: TdApi.AddContact,
    ): DemoAnswer {
        val user = world.users[function.userId] ?: return notFound()
        user.isContact = true
        user.isMutualContact = true
        val updates = mutableListOf<TdApi.Update>()
        function.contact?.let {
            if (it.firstName.isNotBlank()) user.firstName = it.firstName.trim()
            user.lastName = it.lastName.orEmpty().trim()
            val info = info(world, user.id)
            if (it.note != null && info != null) {
                info.note = it.note
                updates += TdApi.UpdateUserFullInfo(user.id, info)
            }
        }
        return okChanged(listOf(TdApi.UpdateUser(user)) + updates)
    }

    /** The note about one of the demo's people, which only the user sees: kept on the phone, like everything here. */
    private fun setNote(
        world: DemoWorld,
        userId: Long,
        note: TdApi.FormattedText?,
    ): DemoAnswer {
        val info = info(world, userId) ?: return notFound()
        info.note = note ?: TdApi.FormattedText("", emptyArray())
        return okChanged(listOf(TdApi.UpdateUserFullInfo(userId, info)))
    }

    /** Numbers of the demo's people match them; every other number is not on Telegram (user id 0). */
    private fun importContacts(
        world: DemoWorld,
        contacts: List<TdApi.ImportedContact>,
        add: Boolean,
    ): DemoAnswer {
        val updates = mutableListOf<TdApi.Update>()
        val ids =
            contacts.map { contact ->
                val user = userByPhone(world, contact.phoneNumber) ?: return@map 0L
                if (add && !user.isContact) {
                    user.isContact = true
                    if (contact.firstName.isNotBlank()) user.firstName = contact.firstName.trim()
                    user.lastName = contact.lastName.orEmpty().trim()
                    updates += TdApi.UpdateUser(user)
                }
                user.id
            }
        val result = TdApi.ImportedContacts(ids.toLongArray(), IntArray(ids.size))
        return if (updates.isEmpty()) ok(result) else changed(result, updates)
    }

    private fun removeContacts(
        world: DemoWorld,
        userIds: List<Long>,
    ): DemoAnswer {
        val removed = userIds.mapNotNull { world.users[it] }.filter { it.isContact }
        removed.forEach {
            it.isContact = false
            it.isMutualContact = false
        }
        return okChanged(removed.map { TdApi.UpdateUser(it) })
    }

    private fun userByPhone(
        world: DemoWorld,
        phone: String,
    ): TdApi.User? {
        val digits = phone.filter(Char::isDigit)
        if (digits.isEmpty()) return null
        return world.users.values.firstOrNull { it.phoneNumber.isNotEmpty() && it.phoneNumber == digits }
    }

    private fun blocked(
        world: DemoWorld,
        offset: Int,
        limit: Int,
    ): DemoAnswer {
        val ids = world.users.keys.filter { fullInfo[it]?.blockList is TdApi.BlockListMain }
        val page = ids.drop(offset).take(limit).map { TdApi.MessageSenderUser(it) }
        return ok(TdApi.MessageSenders(ids.size, page.toTypedArray()))
    }

    private fun block(
        world: DemoWorld,
        sender: TdApi.MessageSender,
        list: TdApi.BlockList?,
    ): DemoAnswer {
        val userId = (sender as? TdApi.MessageSenderUser)?.userId ?: return needsAccount()
        val info = info(world, userId) ?: return notFound()
        if (userId == DemoData.ME) return badRequest("USER_IS_SELF")
        info.blockList = list
        val updates = mutableListOf<TdApi.Update>(TdApi.UpdateUserFullInfo(userId, info))
        world.chat(userId)?.let { chat ->
            chat.blockList = list
            updates += TdApi.UpdateChatBlockList(chat.id, list)
        }
        return okChanged(updates)
    }

    /** Everyone may see what the user shows, except their phone number, which only contacts see. */
    private fun rules(setting: TdApi.UserPrivacySetting): TdApi.UserPrivacySettingRules =
        privacy.getOrPut(setting.javaClass.simpleName) {
            val rule: TdApi.UserPrivacySettingRule =
                when (setting) {
                    is TdApi.UserPrivacySettingShowPhoneNumber,
                    is TdApi.UserPrivacySettingAllowFindingByPhoneNumber,
                    -> {
                        TdApi.UserPrivacySettingRuleAllowContacts()
                    }

                    else -> {
                        TdApi.UserPrivacySettingRuleAllowAll()
                    }
                }
            TdApi.UserPrivacySettingRules(arrayOf(rule))
        }

    private fun setRules(
        setting: TdApi.UserPrivacySetting,
        rules: TdApi.UserPrivacySettingRules,
    ): DemoAnswer {
        privacy[setting.javaClass.simpleName] = rules
        return okChanged(listOf(TdApi.UpdateUserPrivacySettingRules(setting, rules)))
    }

    private fun terminate(id: Long): DemoAnswer {
        val session = sessions.firstOrNull { it.id == id } ?: return notFound()
        if (session.isCurrent) return badRequest("SESSION_IS_CURRENT")
        sessions.remove(session)
        return okChanged(emptyList())
    }

    /** A login code from another device links a sample session: the demo reaches no real device. */
    private fun linkDevice(
        world: DemoWorld,
        link: String,
    ): DemoAnswer {
        if (!link.startsWith("tg://login")) return badRequest("AUTH_TOKEN_INVALID")
        val session =
            session(false, "Desktop", TdApi.SessionDeviceTypeLinux(), world.now, world.now).apply {
                deviceModel = "Demo computer"
            }
        sessions += session
        return changed(session, emptyList())
    }

    /** Ada's story at the top of the stories, until the user has seen it. */
    fun activeStories(world: DemoWorld): TdApi.ChatActiveStories {
        if (!started) start(world)
        return TdApi.ChatActiveStories(
            DemoData.ADA_CHAT,
            TdApi.StoryListMain(),
            storyDate(world).toLong(),
            false,
            if (storyRead) STORY_ID else 0,
            arrayOf(TdApi.StoryInfo(STORY_ID, storyDate(world), false, false)),
        )
    }

    private fun chatStories(
        world: DemoWorld,
        chatId: Long,
    ): TdApi.ChatActiveStories =
        if (chatId == DemoData.ADA_CHAT) {
            activeStories(world)
        } else {
            TdApi.ChatActiveStories(chatId, null, 0, false, 0, emptyArray())
        }

    private fun storyDate(world: DemoWorld) = world.data.storyDate

    private fun story(
        world: DemoWorld,
        chatId: Long,
        storyId: Int,
    ): DemoAnswer {
        if (chatId != DemoData.ADA_CHAT || storyId != STORY_ID) return notFound()
        val photo =
            TdApi.Photo(
                false,
                null,
                arrayOf(
                    TdApi.PhotoSize(
                        "x",
                        DemoFiles.remote(DemoFile.StoryPhoto),
                        STORY_WIDTH,
                        STORY_HEIGHT,
                        intArrayOf(),
                    ),
                ),
            )
        return ok(
            TdApi.Story().apply {
                id = STORY_ID
                posterChatId = chatId
                posterId = TdApi.MessageSenderUser(DemoData.ADA)
                date = storyDate(world)
                isPostedToChatPage = true
                canBeReplied = true
                chosenReactionType = storyReaction
                privacySettings = TdApi.StoryPrivacySettingsEveryone(longArrayOf())
                content = TdApi.StoryContentPhoto(photo)
                areas = emptyArray()
                caption = TdApi.FormattedText(world.data.text(R.string.demo_story_caption), emptyArray())
                albumIds = intArrayOf()
            },
        )
    }

    private fun openStory(
        world: DemoWorld,
        chatId: Long,
        storyId: Int,
    ): DemoAnswer {
        if (chatId != DemoData.ADA_CHAT || storyId != STORY_ID) return notFound()
        if (storyRead) return ok(TdApi.Ok())
        storyRead = true
        return okChanged(listOf(TdApi.UpdateChatActiveStories(activeStories(world))))
    }

    private fun storyReactions(): TdApi.AvailableReactions {
        val reactions =
            DemoData.REACTIONS
                .map {
                    TdApi.AvailableReaction(
                        TdApi.ReactionTypeEmoji(it),
                        false,
                    )
                }.toTypedArray()
        return TdApi.AvailableReactions(reactions, emptyArray(), emptyArray(), false, false, null)
    }

    private fun reactToStory(
        world: DemoWorld,
        function: TdApi.SetStoryReaction,
    ): DemoAnswer {
        if (function.storyPosterChatId != DemoData.ADA_CHAT || function.storyId != STORY_ID) return notFound()
        storyReaction = function.reactionType
        val story = (story(world, function.storyPosterChatId, function.storyId).result as? TdResult.Success)?.value
        return okChanged(listOfNotNull((story as? TdApi.Story)?.let { TdApi.UpdateStory(it) }))
    }

    private fun country(code: Pair<String, String>): TdApi.CountryInfo {
        val (iso, calling) = code
        val region = Locale.Builder().setRegion(iso).build()
        return TdApi.CountryInfo(
            iso,
            region.displayCountry,
            region.getDisplayCountry(Locale.ENGLISH),
            flagOf(iso),
            false,
            arrayOf(calling),
        )
    }

    private fun countryCode(): String =
        Locale.getDefault().country.takeIf { iso -> COUNTRIES.any { it.first == iso } } ?: "GB"

    private fun phoneNumberInfo(prefix: String): TdApi.PhoneNumberInfo {
        val digits = prefix.filter(Char::isDigit)
        val match = COUNTRIES.filter { digits.startsWith(it.second) }.maxByOrNull { it.second.length }
        return TdApi.PhoneNumberInfo(
            match?.let(::country),
            match?.second.orEmpty(),
            digits.removePrefix(match?.second.orEmpty()),
            false,
        )
    }

    private fun flagOf(iso: String) =
        iso.uppercase().map { Character.toChars(REGIONAL_INDICATOR_A + (it - 'A')).concatToString() }.joinToString("")

    private companion object {
        const val DAY = 86_400
        const val FIRST_ADDRESS = 10
        const val INACTIVE_SESSION_DAYS = 180
        const val PHOTO_SIZE = 640
        const val STORY_ID = 1
        const val REGIONAL_INDICATOR_A = 0x1F1E6
        const val DOWNLOAD_LINK = "https://telegram.org/dl"
        const val TEMPORARY_LINK = "https://t.me/contact/demo"
        val USERNAME = Regex("[A-Za-z][A-Za-z0-9_]{4,31}")

        /** The countries the phone number fields offer in the demo, with their calling codes. */
        val COUNTRIES =
            listOf(
                "GB" to "44",
                "US" to "1",
                "DE" to "49",
                "FR" to "33",
                "ES" to "34",
                "IT" to "39",
                "NL" to "31",
                "IN" to "91",
                "BR" to "55",
                "JP" to "81",
                "AE" to "971",
                "AU" to "61",
            )
    }
}
