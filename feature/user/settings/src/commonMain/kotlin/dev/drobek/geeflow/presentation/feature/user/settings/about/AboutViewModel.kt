package dev.drobek.geeflow.presentation.feature.user.settings.about

import dev.drobek.geeflow.core.presentation.BaseViewModel
import dev.drobek.geeflow.presentation.feature.user.settings.LicensesList
import dev.drobek.geeflow.presentation.feature.user.settings.about.AboutEvent.BackClicked
import dev.drobek.geeflow.presentation.feature.user.settings.about.AboutEvent.LicensesClicked
import dev.drobek.geeflow.presentation.feature.user.settings.about.AboutEvent.PrivacyPolicyClicked
import dev.drobek.geeflow.presentation.feature.user.settings.about.AboutEvent.SourceCodeClicked
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
