package app.geeflow.presentation.feature.user.settings.about

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.presentation.feature.user.settings.LicensesList
import app.geeflow.presentation.feature.user.settings.about.AboutEvent.BackClicked
import app.geeflow.presentation.feature.user.settings.about.AboutEvent.LicensesClicked
import app.geeflow.presentation.feature.user.settings.about.AboutEvent.PrivacyPolicyClicked
import app.geeflow.presentation.feature.user.settings.about.AboutEvent.SourceCodeClicked
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class AboutViewModel : BaseViewModel<AboutViewState, AboutViewModelEvent>(AboutViewState()) {

    fun handleEvent(event: AboutEvent) = when (event) {
        BackClicked -> navigateBack()
        LicensesClicked -> navigateTo(LicensesList)
        PrivacyPolicyClicked -> emitEvent(AboutViewModelEvent.OpenUrl(URL_PRIVACY_POLICY))
        SourceCodeClicked -> emitEvent(AboutViewModelEvent.OpenUrl(URL_SOURCE_CODE))
    }

    private companion object {
        const val URL_PRIVACY_POLICY = "https://github.com/drobek/GeeFlow"
        const val URL_SOURCE_CODE = "https://github.com/drobek/GeeFlow"
    }
}
