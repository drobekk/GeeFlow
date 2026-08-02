package app.geeflow.presentation.feature.device.dashboard.profileeditor

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.core.presentation.launchCatching
import app.geeflow.core.presentation.toUserMessage
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.domain.brew.usecase.GetBrewProfileUseCase
import app.geeflow.domain.brew.usecase.SaveBrewProfileUseCase
import app.geeflow.domain.brew.usecase.SaveBrewProfileUseCase.Companion.NEW_PROFILE_ID
import app.geeflow.domain.device.usecase.GetDeviceConstraintsUseCase
import app.geeflow.navigation.NavEvent
import app.geeflow.presentation.feature.device.dashboard.ProfileEditor
import app.geeflow.presentation.feature.device.dashboard.model.toTargetData
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.AddStepClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.BackClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.DialogDismissed
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.FinishTargetClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.FinishTargetConfirmed
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.RenameClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.RenameConfirmed
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.SaveClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.StepClicked
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.StepRemoved
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.StepTypeSelected
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.StepValuesConfirmed
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorEvent.StepsReordered
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorViewModelEvent.ShowSnackbar
import app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorViewState.Step
import co.touchlab.kermit.Logger
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_default_name
import geeflow.shared.feature.device.dashboard.generated.resources.profile_editor_description
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class ProfileEditorViewModel(
    @InjectedParam private val args: ProfileEditor,
    private val getBrewProfile: GetBrewProfileUseCase,
    private val saveBrewProfile: SaveBrewProfileUseCase,
    private val getDeviceConstraints: GetDeviceConstraintsUseCase,
) : BaseViewModel<ProfileEditorViewState, ProfileEditorViewModelEvent>(ProfileEditorViewState()) {

    private var nextStepId = 0L

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
    }

    fun handleEvent(event: ProfileEditorEvent) = when (event) {
        is BackClicked -> navigate(NavEvent.Back)
        is SaveClicked -> saveProfile()
        is RenameClicked -> modify { copy(dialog = ProfileEditorDialog.Rename) }
        is RenameConfirmed -> modify { copy(profileName = event.name, dialog = null) }
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
            copy(finishTarget = finishTarget.copy(type = type))
        }
    }

    private fun confirmFinishTarget(target: Float) {
        val dialog = viewState.value.dialog as? ProfileEditorDialog.FinishTargetValue ?: return
        modify {
            val updated = when (dialog.type) {
                FinishTargetType.Volume -> finishTarget.copy(volume = target)
                FinishTargetType.Weight -> finishTarget.copy(weight = target)
            }
            copy(finishTarget = updated, dialog = null)
        }
    }

    private fun reorderSteps(from: Int, to: Int) = updateSteps {
        toMutableList().apply { add(to, removeAt(from)) }
    }

    private fun saveProfile() = launchCatching(::onError) {
        val state = viewState.value
        val steps = state.steps.map { it.toDomain() }
        saveBrewProfile(
            id = args.profileId ?: NEW_PROFILE_ID,
            name = state.profileName,
            description = getString(Res.string.profile_editor_description, steps.size, steps.sumOf { it.time }),
            finishCondition = state.finishTarget.toCondition(),
            steps = steps,
        )
        navigate(NavEvent.Back)
    }

    /** Keeps [ProfileEditorViewState.targetData] in sync with the steps the chart renders. */
    private fun updateSteps(block: List<Step>.() -> List<Step>) = modify {
        val updated = steps.block()
        copy(steps = updated, targetData = updated.map { it.toDomain() }.toTargetData())
    }

    private fun onError(throwable: Throwable) {
        Logger.e(throwable = throwable) { "${this::class.simpleName}" }
        launch { emitEvent(ShowSnackbar(throwable.toUserMessage())) }
    }
}
