@file:Suppress("TooManyFunctions", "LongParameterList")

package app.geeflow.presentation.feature.device.dashboard.profiles

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.core.presentation.launchCatching
import app.geeflow.core.presentation.toUserMessage
import app.geeflow.data.brew.model.BrewHistoryEntry
import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewScaleSample
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.toProgram
import app.geeflow.data.device.model.requiredMetrics
import app.geeflow.domain.brew.usecase.BindProfileUseCase
import app.geeflow.domain.brew.usecase.DeleteProfileUseCase
import app.geeflow.domain.brew.usecase.DuplicateProfileUseCase
import app.geeflow.domain.brew.usecase.GetBrewHistoryDataUseCase
import app.geeflow.domain.brew.usecase.GetBrewHistoryScaleSamplesUseCase
import app.geeflow.domain.brew.usecase.GetBrewHistoryUseCase
import app.geeflow.domain.brew.usecase.ObserveDeviceProfileUseCase
import app.geeflow.domain.brew.usecase.ObserveUserProfilesUseCase
import app.geeflow.domain.brew.usecase.UpdateBrewProfilesPositionsUseCase
import app.geeflow.domain.device.usecase.GetProfileSupportUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.domain.user.usecase.GetFavoriteProfilesUseCase
import app.geeflow.domain.user.usecase.ToggleFavoriteProfileUseCase
import app.geeflow.navigation.destination.DeviceDashboard
import app.geeflow.presentation.feature.device.dashboard.livecontrol.LiveRecipeEditorRoute
import app.geeflow.presentation.feature.device.dashboard.ProfileEditor
import app.geeflow.presentation.feature.device.dashboard.model.ChartData
import app.geeflow.presentation.feature.device.dashboard.model.profileCardDescription
import app.geeflow.presentation.feature.device.dashboard.model.toTargetData
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.AddProfileClicked
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.BindProfileClicked
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.DuplicateProfileClicked
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.EditProfileClicked
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.HistoryBrewSelected
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.HistoryClicked
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.LoadMoreHistory
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.ProfileSelected
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.RemoveProfileClicked
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.Reordered
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent.SearchQueryChanged
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModelEvent.SelectProfile
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModelEvent.ShowHistoryBrew
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModelEvent.ShowSnackbar
import co.touchlab.kermit.Logger
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.brew_history_description
import geeflow.shared.feature.device.dashboard.generated.resources.brew_history_freehand
import geeflow.shared.feature.device.dashboard.generated.resources.brew_history_manual
import geeflow.shared.feature.device.dashboard.generated.resources.brew_history_profile
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_duplicate_name
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import kotlin.time.Instant

@Factory
internal class ProfileListViewModel(
    @InjectedParam private val args: DeviceDashboard,
    private val observeUserProfilesUseCase: ObserveUserProfilesUseCase,
    private val observeDeviceProfileUseCase: ObserveDeviceProfileUseCase,
    private val observeDeviceStateUseCase: ObserveDeviceStateUseCase,
    private val getProfileSupport: GetProfileSupportUseCase,
    private val bindProfileUseCase: BindProfileUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val duplicateProfileUseCase: DuplicateProfileUseCase,
    private val updateBrewProfilesPositionsUseCase: UpdateBrewProfilesPositionsUseCase,
    private val getBrewHistoryUseCase: GetBrewHistoryUseCase,
    private val getBrewHistoryDataUseCase: GetBrewHistoryDataUseCase,
    private val getBrewHistoryScaleSamplesUseCase: GetBrewHistoryScaleSamplesUseCase,
    private val getFavoriteProfiles: GetFavoriteProfilesUseCase,
    private val toggleFavoriteProfile: ToggleFavoriteProfileUseCase,
    private val profileLibrarySync: ProfileLibrarySyncController,
) : BaseViewModel<ProfileListViewState, ProfileListViewModelEvent>(ProfileListViewState()) {

    private var favoriteProfileIds: Set<String> = emptySet()
    private var currentDomainProfiles: List<BrewProfile> = emptyList()
    private var historyEntries: List<BrewHistoryEntry> = emptyList()
    private var historyEndReached = false
    private var searchQuery = ""
    private var historyJob: Job? = null

    init {
        launch {
            profileLibrarySync.state.collect { syncState ->
                modify { copy(profileLibrarySyncState = syncState) }
            }
        }
        launch {
            getFavoriteProfiles().collect { favorites ->
                favoriteProfileIds = favorites
                modify { copy(profiles = profiles.map { it.copy(favorite = it.id in favorites) }) }
            }
        }
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
                modify {
                    copy(
                        smartScaleConnected = it.smartScale?.isConnected == true,
                    )
                }
            }
        }
    }

    fun handleEvent(event: ProfileListEvent) = when (event) {
        is ProfileListEvent.OpenLiveRecipe -> navigateTo(LiveRecipeEditorRoute(args.deviceId, event.id))
        is ProfileListEvent.ToggleFavoriteClicked -> launchCatching(::onError) { toggleFavoriteProfile(event.id) }
        ProfileListEvent.SyncProfileLibraryNow -> launch { profileLibrarySync.syncNow() }
        is ProfileSelected -> setSelectedProfileId(event.id)
        is ProfileListEvent.ProfilePreviewSelected -> setSelectedProfileId(event.id, navigateToBrewPage = false)
        is HistoryClicked -> toggleHistory()
        is HistoryBrewSelected -> showHistoryBrew(event.id)
        is ProfileListEvent.HistoryGrindSettingChanged -> historyGrindSettingChanged(event)
        is LoadMoreHistory -> loadMoreHistory()
        is SearchQueryChanged -> searchQueryChanged(event.query)
        is AddProfileClicked -> navigateTo(ProfileEditor(args.deviceId))
        is BindProfileClicked -> bindProfile(event.id)
        is EditProfileClicked -> navigateTo(ProfileEditor(args.deviceId, event.id.toLongOrNull()))
        is DuplicateProfileClicked -> duplicateProfile(event.id)
        is RemoveProfileClicked -> removeProfile(event.id)
        is Reordered -> reorderProfiles(event.from, event.to)
    }

    private fun toggleHistory() {
        val showHistory = !viewState.value.showHistory
        modify { copy(showHistory = showHistory) }
        if (showHistory) reloadHistory() else clearHistory()
    }

    private fun searchQueryChanged(query: String) {
        if (query == searchQuery) return
        searchQuery = query
        if (viewState.value.showHistory) reloadHistory(debounce = true)
    }

    private fun clearHistory() {
        historyJob?.cancel()
        historyEntries = emptyList()
        historyEndReached = false
        modify { copy(history = emptyList(), historyLoading = false) }
    }

    private fun reloadHistory(debounce: Boolean = false) {
        historyJob?.cancel()
        historyEntries = emptyList()
        historyEndReached = false
        modify { copy(history = emptyList(), historyLoading = true) }
        historyJob = launchCatching(::onError) {
            if (debounce) delay(SEARCH_DEBOUNCE_MS)
            loadHistoryPage()
        }
    }

    private fun loadMoreHistory() {
        if (historyEndReached || historyJob?.isActive == true) return
        modify { copy(historyLoading = true) }
        historyJob = launchCatching(::onError) { loadHistoryPage() }
    }

    private suspend fun loadHistoryPage() {
        val page = getBrewHistoryUseCase.forDevice(args.deviceId, searchQuery, HISTORY_PAGE_SIZE, historyEntries.size)
        historyEndReached = page.size < HISTORY_PAGE_SIZE
        historyEntries = historyEntries + page
        val brews = page.map { it.toHistoryBrew() }
        modify { copy(history = history + brews, historyLoading = false) }
    }

    private fun showHistoryBrew(id: String) = launchCatching(::onError) {
        val entry = historyEntries.find { it.id.toString() == id } ?: return@launchCatching
        modify { copy(history = history.map { it.copy(selected = it.id == id) }) }
        val data = getBrewHistoryDataUseCase(entry.id).mapValues { (_, point) ->
            ChartData(
                pressure = point.pressure,
                weight = point.weight,
                weightPerSecond = point.weightRate,
                volume = point.volume,
                volumePerSecond = point.flowRate,
                temperatureC = point.brewBoilerTempC,
            )
        }
        emitEvent(
            ShowHistoryBrew(
                name = entry.displayName(),
                description = entry.executionTrace?.profile?.description.orEmpty(),
                durationSeconds = entry.durationSeconds,
                phaseProgram = entry.profileProgram ?: entry.executionTrace?.profile?.program ?: entry.profileSteps.toProgram(),
                phaseTransitions = entry.executionTrace?.transitions.orEmpty(),
                data = data,
                scaleSamples = getBrewHistoryScaleSamplesUseCase(entry.id),
                targetData = entry.executionTrace?.toTargetData()
                    ?: entry.profileRecording?.toTargetData()
                    ?: entry.profileSteps.toTargetData(),
                targetWeightGrams = entry.executionTrace?.stopTargetGrams
                    ?: (entry.executionTrace?.profile?.finishCondition as? Condition.Weight)?.target
                    ?: (entry.profileProgram as? BrewProgram.Phases)?.recipe?.settings?.targetYieldG,
                id = entry.id,
                startedAt = entry.startedAt,
                endedAt = entry.endedAt,
                doseGrams = entry.doseGrams,
                grindSetting = entry.grindSetting,
                extractionStartMillis = entry.extractionStartMillis,
                extractionStartEstimated = entry.extractionStartEstimated,
                finalWeightGrams = entry.finalWeightGrams,
                recipeRatio = entry.recipeRatio,
                recipeTargetGrams = entry.recipeTargetGrams,
                recordedPhaseIds = entry.executionTrace?.targets.orEmpty().mapNotNull { it.phaseId }.toSet(),
            ),
        )
    }

    private fun historyGrindSettingChanged(event: ProfileListEvent.HistoryGrindSettingChanged) {
        val entryId = event.id.toLongOrNull() ?: return
        historyEntries = historyEntries.map { entry ->
            if (entry.id == entryId) entry.copy(grindSetting = event.grindSetting) else entry
        }
    }

    private suspend fun BrewHistoryEntry.toHistoryBrew() = ProfileListViewState.HistoryBrew(
        id = id.toString(),
        name = displayName(),
        description = getString(Res.string.brew_history_description, startedAt.formatted(), durationSeconds),
    )

    private suspend fun BrewHistoryEntry.displayName(): String = profileName ?: getString(
        when (mode) {
            BrewMode.Manual -> Res.string.brew_history_manual
            BrewMode.Freehand -> Res.string.brew_history_freehand
            BrewMode.Profile -> Res.string.brew_history_profile
        },
    )

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

    private fun duplicateProfile(id: String) = launchCatching(::onError) {
        val profileId = id.toLongOrNull() ?: return@launchCatching
        val source = currentDomainProfiles.find { it.id == profileId } ?: return@launchCatching
        duplicateProfileUseCase(profileId, getString(Res.string.profile_list_duplicate_name, source.name))
    }

    private fun removeProfile(id: String) = launchCatching(::onError) {
        id.toLongOrNull()?.let { deleteProfileUseCase(it) }
    }

    private fun bindProfile(id: String) = launchCatching(::onError) {
        id.toLongOrNull()?.let { bindProfileUseCase(args.deviceId, it) }
    }

    private fun setSelectedProfileId(id: String?, navigateToBrewPage: Boolean = true) {
        modify { copy(profiles = profiles.map { it.copy(selected = it.id == id) }) }
        emitEvent(SelectProfile(id, navigateToBrewPage = navigateToBrewPage))
    }

    private fun onError(throwable: Throwable) {
        Logger.e(throwable = throwable) { "Unknown error in ProfileListViewModel" }
        modify { copy(historyLoading = false) }
        launch { emitEvent(ShowSnackbar(throwable.toUserMessage())) }
    }

    private suspend fun profilesChanged(profiles: List<BrewProfile>, boundProfileId: String?) {
        val orderedProfiles = profiles.sortedWith(
            compareBy<BrewProfile> {
                when (it.name.trim().lowercase()) {
                    "adaptive v2", "adaptive v2 (lita, no scale)" -> 0
                    "londinium" -> 1
                    else -> 2
                }
            }.thenBy { it.position }.thenBy { it.name.lowercase() },
        )
        currentDomainProfiles = orderedProfiles
        if (orderedProfiles.isEmpty()) return
        val selectedProfileId = viewState.value.profiles.find { it.selected }?.id ?: orderedProfiles.first().id.toString()
        val mappedProfiles = orderedProfiles.mapIndexed { index, profile ->
            mapToProfile(
                index = index,
                profile = profile,
                selected = selectedProfileId == profile.id.toString(),
                bound = boundProfileId == profile.id.toString(),
            )
        }
        modify { copy(profiles = mappedProfiles) }
        emitEvent(SelectProfile(selectedProfileId))
    }

    private suspend fun mapToProfile(
        index: Int,
        profile: BrewProfile,
        selected: Boolean,
        bound: Boolean,
    ): ProfileListViewState.Profile = ProfileListViewState.Profile(
        id = profile.id.toString(),
        number = (index + 1).toString(),
        name = profile.name,
        description = profile.profileCardDescription(),
        brewByWeight = BrewMetric.CupWeight in profile.requiredMetrics(),
        experimental = getProfileSupport(args.deviceId, profile).experimental,
        canBind = getProfileSupport(args.deviceId, profile).bindingAllowed,
        bound = bound,
        selected = selected,
        program = profile.program,
        finishCondition = profile.finishCondition,
        targetData = profile.toTargetData(),
        controlKind = profile.program.controlKind(),
        favorite = profile.id.toString() in favoriteProfileIds,
    )

    private companion object {
        private const val HISTORY_PAGE_SIZE = 20
        private const val SEARCH_DEBOUNCE_MS = 300L
    }
}

private val HistoryDateTimeFormat = LocalDateTime.Format {
    year()
    char('-')
    monthNumber()
    char('-')
    day()
    char(' ')
    hour()
    char(':')
    minute()
}

private fun Instant.formatted(): String =
    HistoryDateTimeFormat.format(toLocalDateTime(TimeZone.currentSystemDefault()))
