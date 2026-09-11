@file:Suppress("TooManyFunctions", "LongParameterList")

package app.geeflow.presentation.feature.device.dashboard.profileeditor

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.core.presentation.launchCatching
import app.geeflow.core.presentation.toUserMessage
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.device.model.DeviceState
import app.geeflow.domain.brew.usecase.BindProfileUseCase
import app.geeflow.domain.brew.usecase.GetBrewProfileUseCase
import app.geeflow.domain.brew.usecase.ObserveBrewDataUseCase
import app.geeflow.domain.brew.usecase.ObserveDeviceProfileUseCase
import app.geeflow.domain.brew.usecase.SaveBrewProfileUseCase
import app.geeflow.domain.device.usecase.GetDeviceConstraintsUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.domain.device.usecase.StartProfileBrewingUseCase
import app.geeflow.domain.device.usecase.StopBrewingUseCase
import app.geeflow.domain.exception.DeviceNotConnectedException
import app.geeflow.domain.user.usecase.GetSkipManualBrewHistoryUseCase
import app.geeflow.domain.user.usecase.GetVisibleChartsUseCase
import app.geeflow.domain.user.usecase.ToggleChartVisibilityUseCase
import app.geeflow.navigation.NavEvent
import app.geeflow.presentation.feature.device.dashboard.ProfileEditor
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Brew
import app.geeflow.presentation.feature.device.dashboard.model.toChartData
import app.geeflow.presentation.feature.device.dashboard.model.toDashboard
import app.geeflow.presentation.feature.device.dashboard.model.toDomain
import app.geeflow.presentation.feature.device.dashboard.model.toTargetData
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.AddStepClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.BackClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.DetailsConfirmed
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.DialogDismissed
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.EditDetailsClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.FinishTargetClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.FinishTargetConfirmed
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.SaveClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.StepClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.StepRemoved
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.StepTypeSelected
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.StepValuesConfirmed
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.StepsReordered
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.StopClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.TestClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.ToggleChartVisibility
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorViewModelEvent.ShowSnackbar
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorViewState.Step
import co.touchlab.kermit.Logger
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_bound_requires_connection
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_default_name
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_description
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class ProfileEditorViewModel(
    @InjectedParam private val args: ProfileEditor,
    private val getBrewProfile: GetBrewProfileUseCase,
    private val bindProfile: BindProfileUseCase,
    private val saveBrewProfile: SaveBrewProfileUseCase,
    private val getDeviceConstraints: GetDeviceConstraintsUseCase,
    private val startProfileBrewing: StartProfileBrewingUseCase,
    private val stopBrewing: StopBrewingUseCase,
    private val toggleChartVisibility: ToggleChartVisibilityUseCase,
    observeBrewData: ObserveBrewDataUseCase,
    observeDeviceState: ObserveDeviceStateUseCase,
    getVisibleCharts: GetVisibleChartsUseCase,
    observeDeviceProfile: ObserveDeviceProfileUseCase,
    getSkipManualBrewHistory: GetSkipManualBrewHistoryUseCase,
) : BaseViewModel<ProfileEditorViewState, ProfileEditorViewModelEvent>(ProfileEditorViewState()) {

    private var nextStepId = 0L
    private var boundProfileId: Long? = null
    private var skipManualBrews = true

    init {
        launch {
            val constraints = getDeviceConstraints(args.deviceId)
            modify { copy(pressureRange = constraints.pressureRange, flowRange = constraints.flowRange) }
        }
        launch {
            val profile = args.profileId?.let { getBrewProfile(it) }
            if (profile == null) {
                val defaultName = getString(Res.string.profile_editor_default_name)
                modify { copy(profileName = defaultName) }
            } else {
                loadProfile(profile)
            }
        }
        launch { getSkipManualBrewHistory().collect { skipManualBrews = it } }
        launch {
            observeBrewData(args.deviceId).collect { session ->
                if (session.mode == BrewMode.Manual && skipManualBrews) return@collect
                modify { copy(brew = brew.copy(time = session.elapsedSeconds, data = session.toChartData())) }
            }
        }
        launch {
            observeDeviceState(args.deviceId).collect { state ->
                modify { copy(isBrewing = state.brewStatus == DeviceState.BrewStatus.Profile) }
            }
        }
        launch { getVisibleCharts().collect { charts -> modify { copy(visibleCharts = charts.toDashboard()) } } }
        launch { observeDeviceProfile(args.deviceId).collect { boundProfileId = it?.id } }
    }

    @Suppress("CyclomaticComplexMethod")
    fun handleEvent(event: ProfileEditorEvent) = when (event) {
        is BackClicked -> navigate(NavEvent.Back)
        is SaveClicked -> saveProfile()
        is TestClicked -> testProfile()
        is StopClicked -> launchCatching(::onError) { stopBrewing(args.deviceId) }
        is ToggleChartVisibility -> launch { toggleChartVisibility(event.type.toDomain()) }
        is EditDetailsClicked -> modify { copy(dialog = ProfileEditorDialog.Details) }
        is DetailsConfirmed -> modify {
            copy(profileName = event.name, description = event.description, dialog = null)
        }
        is AddStepClicked -> modify { copy(dialog = ProfileEditorDialog.StepTypePicker) }
        is StepTypeSelected -> showNewStepValues(event.type)
        is StepClicked -> showStepValues(event.id)
        is StepRemoved -> updateSteps { filterNot { it.id == event.id } }
        is StepsReordered -> reorderSteps(event.from, event.to)
        is StepValuesConfirmed -> confirmStepValues(event.timeSec, event.value)
        is FinishTargetClicked -> finishTargetClicked(event.type)
        is FinishTargetConfirmed -> confirmFinishTarget(event.target)
        is DialogDismissed -> modify { copy(dialog = null) }
    }

    private fun loadProfile(profile: BrewProfile) {
        nextStepId = profile.steps.size.toLong()
        modify {
            copy(
                profileName = profile.name,
                description = profile.description,
                finishTarget = profile.finishCondition.toFinishTarget(),
            )
        }
        updateSteps { profile.steps.mapIndexed { index, step -> step.toStep(index.toLong()) } }
    }

    private fun showNewStepValues(type: StepType) = modify {
        copy(
            dialog = ProfileEditorDialog.StepValues(
                type = type,
                stepId = null,
                timeSec = ProfileEditorDefaults.defaultTimeSec(type),
                value = ProfileEditorDefaults.defaultValue(type),
            ),
        )
    }

    private fun showStepValues(id: Long) {
        val step = viewState.value.steps.find { it.id == id } ?: return
        modify {
            copy(
                dialog = ProfileEditorDialog.StepValues(
                    type = step.type,
                    stepId = step.id,
                    timeSec = step.timeSec,
                    value = step.value,
                ),
            )
        }
    }

    private fun confirmStepValues(timeSec: Int, value: Float) {
        val dialog = viewState.value.dialog as? ProfileEditorDialog.StepValues ?: return
        if (dialog.stepId == null) {
            val step = Step(id = nextStepId++, type = dialog.type, timeSec = timeSec, value = value)
            updateSteps { this + step }
        } else {
            updateSteps { map { if (it.id == dialog.stepId) it.copy(timeSec = timeSec, value = value) else it } }
        }
        modify { copy(dialog = null) }
    }

    /** Selects the target type first; only a press on the already selected one opens the value dialog. */
    private fun finishTargetClicked(type: FinishTargetType) = modify {
        if (finishTarget.type == type) {
            copy(dialog = ProfileEditorDialog.FinishTargetValue(type = type, target = finishTarget.target))
        } else {
            copy(finishTarget = finishTarget.copy(type = type), brew = Brew())
        }
    }

    // Editing the profile invalidates whatever the last test drew, so the recorded brew is dropped
    // from the chart and the brew bar. A test that is still running simply refills it on the next tick.
    private fun confirmFinishTarget(target: Float) {
        val dialog = viewState.value.dialog as? ProfileEditorDialog.FinishTargetValue ?: return
        modify {
            val updated = when (dialog.type) {
                FinishTargetType.Volume -> finishTarget.copy(volume = target)
                FinishTargetType.Weight -> finishTarget.copy(weight = target)
            }
            copy(finishTarget = updated, dialog = null, brew = Brew())
        }
    }

    private fun reorderSteps(from: Int, to: Int) = updateSteps {
        toMutableList().apply { add(to, removeAt(from)) }
    }

    private fun testProfile() = launchCatching(::onError) {
        startProfileBrewing(args.deviceId, editedProfile())
    }

    /** The profile as edited. Neither brewing nor binding persists it, so its user is left unset. */
    private fun editedProfile(): BrewProfile {
        val state = viewState.value
        return BrewProfile(
            id = args.profileId ?: BrewProfile.NEW_ID,
            userId = 0,
            name = state.profileName,
            description = state.description,
            finishCondition = state.finishTarget.toCondition(),
            steps = state.steps.map { it.toDomain() },
        )
    }

    private fun saveProfile() = launchCatching(::onError) {
        val profile = editedProfile()
        // A bound profile has to reach the machine before the change is stored, so that a failed
        // rebind leaves neither the device nor the database on a half-applied version.
        if (profile.id == boundProfileId) {
            try {
                bindProfile(args.deviceId, profile)
            } catch (_: DeviceNotConnectedException) {
                emitEvent(ShowSnackbar(getString(Res.string.profile_editor_bound_requires_connection)))
                return@launchCatching
            }
        }
        saveBrewProfile(
            id = profile.id,
            name = profile.name,
            description = profile.description.ifBlank {
                getString(
                    Res.string.profile_editor_description,
                    profile.steps.size,
                    profile.steps.sumOf { it.time },
                )
            },
            finishCondition = profile.finishCondition,
            steps = profile.steps,
        )
        navigate(NavEvent.Back)
    }

    private fun updateSteps(block: List<Step>.() -> List<Step>) = modify {
        val updated = steps.block()
        copy(
            steps = updated,
            targetData = updated.map { it.toDomain() }.toTargetData(),
            brew = Brew(),
        )
    }

    private fun onError(throwable: Throwable) {
        Logger.e(throwable = throwable) { "${this::class.simpleName}" }
        launch { emitEvent(ShowSnackbar(throwable.toUserMessage())) }
    }
}
