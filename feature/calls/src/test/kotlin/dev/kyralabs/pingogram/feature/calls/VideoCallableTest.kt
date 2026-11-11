package dev.kyralabs.pingogram.feature.calls

import app.cash.turbine.test
import dev.kyralabs.pingogram.core.common.result.DomainError
import dev.kyralabs.pingogram.core.common.result.Outcome
import dev.kyralabs.pingogram.core.domain.model.Birthdate
import dev.kyralabs.pingogram.core.domain.model.FormattedText
import dev.kyralabs.pingogram.core.domain.model.ProfileAudiosPage
import dev.kyralabs.pingogram.core.domain.model.UserFullInfo
import dev.kyralabs.pingogram.core.domain.model.UserProfilePhotosPage
import dev.kyralabs.pingogram.core.domain.repository.UserProfileRepository
import dev.kyralabs.pingogram.feature.calls.ui.CameraAccess
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** [videoCallable]: what the call buttons ask before they offer a video call. */
class VideoCallableTest {
    private val profiles = FakeProfiles()

    @Test
    fun `a user who can be called and takes video calls can get one, and changes follow`() =
        runTest {
            videoCallable(42, profiles, Camera(hasCamera = true)).test {
                profiles.info.emit(Outcome.Success(info(canBeCalled = true, supportsVideoCalls = true)))
                assertTrue(awaitItem())
                profiles.info.emit(Outcome.Success(info(canBeCalled = false, supportsVideoCalls = true)))
                assertFalse(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(listOf(42L), profiles.asked)
        }

    @Test
    fun `an app without video calls, or a full info that failed, offers voice only`() =
        runTest {
            videoCallable(42, profiles, Camera(hasCamera = true)).test {
                profiles.info.emit(Outcome.Success(info(canBeCalled = true, supportsVideoCalls = false)))
                assertFalse(awaitItem())
                profiles.info.emit(Outcome.Failure(DomainError.Network))
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a device without a camera offers voice only and asks TDLib nothing`() =
        runTest {
            videoCallable(42, profiles, Camera(hasCamera = false)).test {
                assertFalse(awaitItem())
                awaitComplete()
            }
            assertTrue(profiles.asked.isEmpty())
        }

    private fun info(
        canBeCalled: Boolean,
        supportsVideoCalls: Boolean,
    ) = UserFullInfo(
        bio = FormattedText.Empty,
        birthdate = null,
        isBlocked = false,
        botDescription = null,
        groupsInCommon = 0,
        canBeCalled = canBeCalled,
        supportsVideoCalls = supportsVideoCalls,
    )

    private class Camera(
        private val hasCamera: Boolean,
    ) : CameraAccess {
        override fun hasCamera() = hasCamera

        override fun isGranted() = false
    }

    private class FakeProfiles : UserProfileRepository {
        override suspend fun profileAudios(
            userId: Long,
            offset: Int,
            limit: Int,
        ): Outcome<ProfileAudiosPage> = Outcome.Success(ProfileAudiosPage(persistentListOf(), 0))

        val info = MutableSharedFlow<Outcome<UserFullInfo>>(replay = 1)
        val asked = mutableListOf<Long>()

        override fun fullInfo(userId: Long): Flow<Outcome<UserFullInfo>> {
            asked += userId
            return info
        }

        override suspend fun profilePhotos(
            userId: Long,
            offset: Int,
            limit: Int,
        ): Outcome<UserProfilePhotosPage> = Outcome.Failure(DomainError.Network)

        override suspend fun suggestBirthdate(
            userId: Long,
            birthdate: Birthdate,
        ): Outcome<Unit> = Outcome.Failure(DomainError.Network)

        override suspend fun setNote(
            userId: Long,
            note: String,
        ): Outcome<Unit> = Outcome.Failure(DomainError.Network)
    }
}
