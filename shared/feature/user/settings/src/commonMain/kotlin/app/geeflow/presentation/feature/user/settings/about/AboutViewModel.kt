package app.geeflow.presentation.feature.user.settings.about

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.presentation.feature.user.settings.LicensesList
import app.geeflow.presentation.feature.user.settings.about.AboutEvent.BackClicked
import app.geeflow.presentation.feature.user.settings.about.AboutEvent.LicensesClicked
import app.geeflow.presentation.feature.user.settings.about.AboutEvent.PrivacyPolicyClicked
import app.geeflow.presentation.feature.user.settings.about.AboutEvent.SourceCodeClicked
import app.geeflow.presentation.feature.user.settings.about.AboutEvent.TermsAndConditionsClicked
import app.geeflow.ui.GeeFlowUrls
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class AboutViewModel : BaseViewModel<AboutViewState, AboutViewModelEvent>(AboutViewState()) {

    fun handleEvent(event: AboutEvent) = when (event) {
        BackClicked -> navigateBack()
        LicensesClicked -> navigateTo(LicensesList)
        TermsAndConditionsClicked -> emitEvent(AboutViewModelEvent.OpenUrl(GeeFlowUrls.TermsAndConditions))
        PrivacyPolicyClicked -> emitEvent(AboutViewModelEvent.OpenUrl(GeeFlowUrls.PrivacyPolicy))
        SourceCodeClicked -> emitEvent(AboutViewModelEvent.OpenUrl(GeeFlowUrls.SourceCode))
    }
}
