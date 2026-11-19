package app.geeflow.presentation.feature.user.settings.about

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.presentation.feature.user.settings.LicensesList
import app.geeflow.presentation.feature.user.settings.about.AboutEvent.BackClicked
import app.geeflow.presentation.feature.user.settings.about.AboutEvent.LicensesClicked
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class AboutViewModel : BaseViewModel<AboutViewState, Unit>(AboutViewState()) {

    fun handleEvent(event: AboutEvent) = when (event) {
        BackClicked -> navigateBack()
        LicensesClicked -> navigateTo(LicensesList)
    }
}
