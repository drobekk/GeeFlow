package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.AppPaletteStyle
import app.geeflow.data.user.model.AppTheme
import app.geeflow.data.user.model.ThemeMode
import app.geeflow.data.user.model.User
import app.geeflow.domain.user.model.AppearanceSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAppearanceSettingsUseCaseTest {
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val settings = mockk<UserSettingsRepository>(relaxUnitFun = true)
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))

    init {
        every { users.selectedUser } returns selected
    }

    @Test
    fun `when user is selected then combines settings for that user`() = runTest {
        every { settings.darkMode(7) } returns flowOf(ThemeMode.DARK)
        every { settings.appTheme(7) } returns flowOf(AppTheme.CUSTOM)
        every { settings.paletteStyle(7) } returns flowOf(AppPaletteStyle.VIBRANT)
        every { settings.customSeedColor(7) } returns flowOf(123)
        every { settings.fullScreenMode(7) } returns flowOf(true)
        every { settings.keepScreenOn(7) } returns flowOf(true)
        every { settings.keepScreenOnOnlyWhileCharging(7) } returns flowOf(true)
        every { settings.screensaverEnabled(7) } returns flowOf(true)
        every { settings.screensaverTimeoutMinutes(7) } returns flowOf(12)
        val useCase = GetAppearanceSettingsUseCase(settings, users)

        val result = useCase().first()

        assertEquals(
            AppearanceSettings(
                themeMode = ThemeMode.DARK,
                appTheme = AppTheme.CUSTOM,
                paletteStyle = AppPaletteStyle.VIBRANT,
                customSeedColor = 123,
                fullScreenMode = true,
                keepScreenOn = true,
                keepScreenOnOnlyWhileCharging = true,
                screensaverEnabled = true,
                screensaverTimeoutMinutes = 12,
            ),
            result
        )
    }

    @Test
    fun `when selected user changes then observes new user settings`() = runTest {
        every { settings.darkMode(7) } returns flowOf(ThemeMode.DARK)
        every { settings.appTheme(7) } returns flowOf(AppTheme.CUSTOM)
        every { settings.paletteStyle(7) } returns flowOf(AppPaletteStyle.VIBRANT)
        every { settings.customSeedColor(7) } returns flowOf(123)
        every { settings.fullScreenMode(7) } returns flowOf(true)
        every { settings.keepScreenOn(7) } returns flowOf(true)
        every { settings.keepScreenOnOnlyWhileCharging(7) } returns flowOf(true)
        every { settings.screensaverEnabled(7) } returns flowOf(true)
        every { settings.screensaverTimeoutMinutes(7) } returns flowOf(12)
        val useCase = GetAppearanceSettingsUseCase(settings, users)
        selected.value = User(id = 8, name = "Grace")
        every { settings.darkMode(8) } returns flowOf(ThemeMode.DARK)
        every { settings.appTheme(8) } returns flowOf(AppTheme.CUSTOM)
        every { settings.paletteStyle(8) } returns flowOf(AppPaletteStyle.VIBRANT)
        every { settings.customSeedColor(8) } returns flowOf(123)
        every { settings.fullScreenMode(8) } returns flowOf(true)
        every { settings.keepScreenOn(8) } returns flowOf(true)
        every { settings.keepScreenOnOnlyWhileCharging(8) } returns flowOf(true)
        every { settings.screensaverEnabled(8) } returns flowOf(true)
        every { settings.screensaverTimeoutMinutes(8) } returns flowOf(12)

        val result = useCase().first()

        assertEquals(
            AppearanceSettings(
                themeMode = ThemeMode.DARK,
                appTheme = AppTheme.CUSTOM,
                paletteStyle = AppPaletteStyle.VIBRANT,
                customSeedColor = 123,
                fullScreenMode = true,
                keepScreenOn = true,
                keepScreenOnOnlyWhileCharging = true,
                screensaverEnabled = true,
                screensaverTimeoutMinutes = 12,
            ),
            result
        )
        // MockK verifies this call without collecting its returned Flow.
        @Suppress("IgnoredReturnValue")
        verify(exactly = 0) { settings.darkMode(7) }
    }
}
