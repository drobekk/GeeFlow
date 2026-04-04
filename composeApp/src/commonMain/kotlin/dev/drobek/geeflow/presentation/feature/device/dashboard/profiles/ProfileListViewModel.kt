package dev.drobek.geeflow.presentation.feature.device.dashboard.profiles

import co.touchlab.kermit.Logger
import dev.drobek.geeflow.core.presentation.BaseViewModel
import dev.drobek.geeflow.core.presentation.launch
import dev.drobek.geeflow.core.presentation.launchCatching
import dev.drobek.geeflow.domain.brew.model.BrewProfile
import dev.drobek.geeflow.domain.brew.model.Condition
import dev.drobek.geeflow.domain.brew.model.ProfileStep
import dev.drobek.geeflow.domain.brew.usecase.BindProfileUseCase
import dev.drobek.geeflow.domain.brew.usecase.DeleteProfileUseCase
import dev.drobek.geeflow.domain.brew.usecase.ObserveDeviceProfileUseCase
import dev.drobek.geeflow.domain.brew.usecase.ObserveUserProfilesUseCase
import dev.drobek.geeflow.domain.brew.usecase.UpdateBrewProfilesPositionsUseCase
import dev.drobek.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import dev.drobek.geeflow.domain.exception.toUserMessage
import dev.drobek.geeflow.navigation.destination.DeviceDashboard
import dev.drobek.geeflow.presentation.feature.device.dashboard.model.ChartData
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.AddProfileClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.BindProfileClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.EditProfileClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.HistoryClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.ProfileSelected
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.RemoveProfileClicked
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.Reordered
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModelEvent.SelectProfile
import dev.drobek.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModelEvent.ShowSnackbar
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

@Factory
internal class ProfileListViewModel(
    @InjectedParam private val args: DeviceDashboard,
    private val observeUserProfilesUseCase: ObserveUserProfilesUseCase,
    private val observeDeviceProfileUseCase: ObserveDeviceProfileUseCase,
    private val observeDeviceStateUseCase: ObserveDeviceStateUseCase,
    private val bindProfileUseCase: BindProfileUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val updateBrewProfilesPositionsUseCase: UpdateBrewProfilesPositionsUseCase
) : BaseViewModel<ProfileListViewState, ProfileListViewModelEvent>(ProfileListViewState()) {

    private var currentDomainProfiles: List<BrewProfile> = emptyList()

    init {
        launch {
            combine(
                observeDeviceProfileUseCase(args.deviceId),
                observeUserProfilesUseCase()
            ) { deviceProfile, userProfiles ->
                deviceProfile to (listOfNotNull(deviceProfile) + userProfiles.filter { it.id != deviceProfile?.id })
            }.collect { (deviceProfile, profiles) ->
                profilesChanged(profiles, deviceProfile?.id?.toString())
            }
        }
        launch {
            observeDeviceStateUseCase(args.deviceId).collect {
                modify { copy(smartScaleConnected = it.smartScale?.isConnected == true) }
            }
        }
    }

    fun handleEvent(event: ProfileListEvent) = when (event) {
        is ProfileSelected -> setSelectedProfileId(event.id)
        is HistoryClicked -> Unit // TODO
        is AddProfileClicked -> Unit // TODO
        is BindProfileClicked -> bindProfile(event.id)
        is EditProfileClicked -> Unit // TODO
        is RemoveProfileClicked -> removeProfile(event.id)
        is Reordered -> reorderProfiles(event.from, event.to)
    }

    private fun reorderProfiles(from: Int, to: Int) {
        val profiles = viewState.value.profiles.toMutableList()
        if (profiles[from].bound || profiles[to].bound) return

        val profile = profiles.removeAt(from)
        profiles.add(to, profile)
        modify { copy(profiles = profiles) }

        val reorderedDomainProfiles = profiles
            .mapNotNull { profile -> currentDomainProfiles.find { it.id.toString() == profile.id } }
            .mapIndexed { index, brewProfile -> brewProfile.copy(position = index) }

        launch {
            updateBrewProfilesPositionsUseCase(reorderedDomainProfiles)
        }
    }

    private fun removeProfile(id: String) = launchCatching(::onError) {
        id.toLongOrNull()?.let { deleteProfileUseCase(it) }
    }

    private fun bindProfile(id: String) = launchCatching(::onError) {
        id.toLongOrNull()?.let { bindProfileUseCase(args.deviceId, it) }
    }

    private fun setSelectedProfileId(id: String?) {
        modify { copy(profiles = profiles.map { it.copy(selected = it.id == id) }) }
        emitEvent(SelectProfile(id))
    }

    private fun onError(throwable: Throwable) {
        Logger.e(throwable = throwable) { "Unknown error in ProfileListViewModel" }
        launch { emitEvent(ShowSnackbar(throwable.toUserMessage())) }
    }

    private fun profilesChanged(profiles: List<BrewProfile>, boundProfileId: String?) {
        currentDomainProfiles = profiles
        if (profiles.isEmpty()) return
        val selectedProfileId = viewState.value.profiles.find { it.selected }?.id ?: profiles.first().id.toString()
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

        data class StepEvent(val time: Float, val pressure: Float, val flow: Float)

        val events = mutableListOf<StepEvent>()
        var currentTime = 0f
        var currentPressure = 0f
        var currentFlow = 0f

        for (step in profile.steps) {
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
            events.add(StepEvent(currentTime, nextPressure, nextFlow))
            currentPressure = nextPressure
            currentFlow = nextFlow
            currentTime += step.time.toFloat()
        }

        val totalTicks = (currentTime * 10).toInt()
        val targetData = buildMap(totalTicks + 1) {
            for (tick in 0..totalTicks) {
                val t = tick / 10f
                val event = events.lastOrNull { it.time <= t } ?: events.first()
                put(
                    key = t,
                    value = ChartData(
                        pressure = event.pressure,
                        weight = 0f,
                        weightPerSecond = 0f,
                        volume = 0f,
                        volumePerSecond = event.flow
                    )
                )
            }
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
