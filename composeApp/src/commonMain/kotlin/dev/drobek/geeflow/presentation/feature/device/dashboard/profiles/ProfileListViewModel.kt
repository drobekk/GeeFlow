package dev.drobek.geeflow.presentation.feature.device.dashboard.profiles

import dev.drobek.geeflow.domain.brew.model.BrewProfile
import dev.drobek.geeflow.domain.brew.model.Condition
import dev.drobek.geeflow.domain.brew.model.ProfileStep
import dev.drobek.geeflow.domain.brew.usecase.BindProfileUseCase
import dev.drobek.geeflow.domain.brew.usecase.DeleteProfileUseCase
import dev.drobek.geeflow.domain.brew.usecase.ObserveDeviceProfileUseCase
import dev.drobek.geeflow.domain.brew.usecase.ObserveUserProfilesUseCase
import dev.drobek.geeflow.presentation.feature.device.dashboard.model.ChartData
import dev.drobek.geeflow.presentation.feature.device.dashboard.navigation.DeviceDestinations
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.AddProfileClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.BindProfileClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.EditProfileClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.HistoryClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.ProfileSelected
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.RemoveProfileClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModelEvent.SelectProfile
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModelEvent.ShowSnackbar
import dev.drobek.geeflow.viewmodel.BaseViewModel
import geeflow.composeapp.generated.resources.Res
import geeflow.composeapp.generated.resources.generic_error
import kotlinx.coroutines.flow.combine
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

@Factory
internal class ProfileListViewModel(
    @InjectedParam private val args: DeviceDestinations.DeviceDashboard,
    private val observeUserProfilesUseCase: ObserveUserProfilesUseCase,
    private val observeDeviceProfileUseCase: ObserveDeviceProfileUseCase,
    private val bindProfileUseCase: BindProfileUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase
) : BaseViewModel<ProfileListViewState, ProfileListViewModelEvent>(ProfileListViewState()) {

    init {
        launch {
            combine(
                flow = observeDeviceProfileUseCase(args.id),
                flow2 = observeUserProfilesUseCase(),
                transform = { deviceProfile, userProfiles -> listOfNotNull(deviceProfile) + userProfiles }
            ).collect(::profilesChanged)
        }
    }

    fun handleEvent(event: ProfileListEvent) = when (event) {
        is ProfileSelected -> setSelectedProfileId(event.id)
        is HistoryClicked -> Unit // TODO
        is AddProfileClicked -> Unit // TODO
        is BindProfileClicked -> bindProfile(event.id)
        is EditProfileClicked -> Unit // TODO
        is RemoveProfileClicked -> removeProfile(event.id)
    }

    private fun removeProfile(id: String) = launchCatching(::onError) {
        id.toLongOrNull()?.let { deleteProfileUseCase(it) }
    }

    private fun bindProfile(id: String) = launchCatching(::onError) {
        id.toLongOrNull()?.let { bindProfileUseCase(args.id, it) }
    }

    private fun setSelectedProfileId(id: String?) {
        modify { copy(profiles = profiles.map { it.copy(selected = it.id == id) }) }
        emitEvent(SelectProfile(id))
    }

    private fun onError(throwable: Throwable) {
        emitEvent { ShowSnackbar(getString(Res.string.generic_error)) }
    }

    private fun profilesChanged(profiles: List<BrewProfile>) {
        if (profiles.isEmpty()) return
        val selectedProfileId = viewState.value.profiles.find { it.selected }?.id ?: profiles.first().id.toString()
        val boundProfileId = profiles.find { it.boundDeviceMac == args.id }?.id?.toString()
        modify {
            copy(
                profiles = profiles.mapIndexed { index, profile ->
                    mapToProfile(
                        index = index,
                        profile = profile,
                        selected = selectedProfileId == profile.id.toString(),
                        bound = boundProfileId == profile.id.toString()
                    )
                }
            )
        }
        emitEvent(SelectProfile(selectedProfileId))
    }

    private fun mapToProfile(index: Int, profile: BrewProfile, selected: Boolean, bound: Boolean): ProfileListViewState.Profile {
        val conditionText = when (val cond = profile.finishCondition) {
            is Condition.Weight -> "${cond.target.toInt()}g"
            is Condition.Volume -> "${cond.target.toInt()}ml"
        }

        val targetData = mutableMapOf<Float, ChartData>()
        var currentTime = 0f
        var currentPressure = 0f
        var currentFlow = 0f

        for (step in profile.steps) {
            val duration = step.time.toFloat()
            val nextPressure = when (step) {
                is ProfileStep.Pressure -> step.pressure
                is ProfileStep.Wait -> 0f
                else -> currentPressure
            }
            val nextFlow = when (step) {
                is ProfileStep.Flow -> step.flow
                is ProfileStep.Wait -> 0f
                else -> currentFlow
            }

            if (currentTime == 0f) {
                targetData[0f] = ChartData(
                    pressure = nextPressure,
                    weight = 0f,
                    weightPerSecond = 0f,
                    volume = 0f,
                    volumePerSecond = nextFlow
                )
            } else if (currentPressure != nextPressure || currentFlow != nextFlow) {
                targetData[currentTime + 0.001f] = ChartData(
                    pressure = nextPressure,
                    weight = 0f,
                    weightPerSecond = 0f,
                    volume = 0f,
                    volumePerSecond = nextFlow
                )
            }

            currentPressure = nextPressure
            currentFlow = nextFlow
            currentTime += duration

            targetData[currentTime] = ChartData(
                pressure = currentPressure,
                weight = 0f,
                weightPerSecond = 0f,
                volume = 0f,
                volumePerSecond = currentFlow
            )
        }

        return ProfileListViewState.Profile(
            id = profile.id.toString(),
            number = (index + 1).toString(),
            name = profile.name,
            description = "$conditionText • ${profile.description}",
            brewByWeight = profile.finishCondition is Condition.Weight,
            bound = bound,
            selected = selected,
            targetData = targetData
        )
    }
}
