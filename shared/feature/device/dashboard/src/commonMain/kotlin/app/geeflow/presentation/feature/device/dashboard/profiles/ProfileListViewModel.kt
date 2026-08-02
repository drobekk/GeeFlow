package app.geeflow.presentation.feature.device.dashboard.profiles

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.core.presentation.launchCatching
import app.geeflow.core.presentation.toUserMessage
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.Condition
import app.geeflow.domain.brew.usecase.BindProfileUseCase
import app.geeflow.domain.brew.usecase.DeleteProfileUseCase
import app.geeflow.domain.brew.usecase.ObserveDeviceProfileUseCase
import app.geeflow.domain.brew.usecase.ObserveUserProfilesUseCase
import app.geeflow.domain.brew.usecase.UpdateBrewProfilesPositionsUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.navigation.destination.DeviceDashboard
import app.geeflow.presentation.feature.device.dashboard.ProfileEditor
import app.geeflow.presentation.feature.device.dashboard.model.toTargetData
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.AddProfileClicked
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.BindProfileClicked
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.EditProfileClicked
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.HistoryClicked
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.ProfileSelected
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.RemoveProfileClicked
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.Reordered
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModelEvent.SelectProfile
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModelEvent.ShowSnackbar
import co.touchlab.kermit.Logger
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
    private val updateBrewProfilesPositionsUseCase: UpdateBrewProfilesPositionsUseCase,
) : BaseViewModel<ProfileListViewState, ProfileListViewModelEvent>(ProfileListViewState()) {

    private var currentDomainProfiles: List<BrewProfile> = emptyList()

    init {
        launch {
            combine(
                observeDeviceProfileUseCase(args.deviceId),
                observeUserProfilesUseCase(),
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
        is AddProfileClicked -> navigateTo(ProfileEditor(args.deviceId))
        is BindProfileClicked -> bindProfile(event.id)
        is EditProfileClicked -> navigateTo(ProfileEditor(args.deviceId, event.id.toLongOrNull()))
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
                        bound = boundProfileId == profile.id.toString(),
                    )
                },
            )
        }
        emitEvent(SelectProfile(selectedProfileId))
    }

    private fun mapToProfile(index: Int, profile: BrewProfile, selected: Boolean, bound: Boolean): ProfileListViewState.Profile {
        val conditionText = when (val cond = profile.finishCondition) {
            is Condition.Weight -> "${cond.target.toInt()}g"
            is Condition.Volume -> "${cond.target.toInt()}ml"
        }

        return ProfileListViewState.Profile(
            id = profile.id.toString(),
            number = (index + 1).toString(),
            name = profile.name,
            description = "$conditionText • ${profile.description}",
            brewByWeight = profile.finishCondition is Condition.Weight,
            bound = bound,
            selected = selected,
            targetData = profile.steps.toTargetData(),
        )
    }
}
