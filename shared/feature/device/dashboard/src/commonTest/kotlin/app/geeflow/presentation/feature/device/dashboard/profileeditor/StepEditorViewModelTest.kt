package app.geeflow.presentation.feature.device.dashboard.profileeditor

import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.PressureLocation
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.data.device.model.ProfilingCapabilities
import app.geeflow.data.device.model.TargetRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class StepEditorViewModelTest {
    private fun editor(
        capabilities: ProfilingCapabilities = ProfilingCapabilities(
            livePressure = mapOf(PressureLocation.Pump to TargetRange(0f, 12f, 0.1f)),
            liveFlow = TargetRange(0f, 8f, 0.1f),
            telemetry = setOf(BrewMetric.PumpedVolume),
        ),
    ) = StepEditorViewModel(
        step = ProfileEditorViewState.Step(id = 1, type = StepType.Pressure, timeSec = 10, value = 9f),
        capabilities = capabilities,
        pressureRange = 0f..12f,
        flowRange = 0f..8f,
    )

    @Test
    fun unsupportedOptionsCannotBeSelected() {
        val editor = editor(capabilities = ProfilingCapabilities())
        editor.handleEvent(StepEditorEvent.TypeChanged(StepType.Flow))
        editor.handleEvent(StepEditorEvent.RampChanged(RampStyle.Linear))
        editor.handleEvent(StepEditorEvent.ConditionAdded)
        assertEquals(StepType.Pressure, editor.viewState.value.type)
        assertEquals(RampStyle.Instant, editor.viewState.value.rampStyle)
        assertEquals(listOf(BrewMetric.PhaseTime), editor.viewState.value.conditions.map { it.metric })
    }

    @Test
    fun addingConditionUsesAnAvailableSensor() {
        val editor = editor(capabilities = ProfilingCapabilities(telemetry = setOf(BrewMetric.CupWeight)))
        editor.handleEvent(StepEditorEvent.ConditionAdded)
        assertEquals(BrewMetric.CupWeight, editor.viewState.value.conditions.last().metric)
        assertEquals("36", editor.viewState.value.conditions.last().value)
    }

    @Test fun timeConditionCannotBeRemovedOrChangedToAnotherMetric() {
        val editor = editor()
        val time = editor.viewState.value.conditions.single()
        editor.handleEvent(StepEditorEvent.ConditionRemoved(time.id))
        assertEquals(time, editor.viewState.value.conditions.single())
        editor.handleEvent(StepEditorEvent.ConditionChanged(time.copy(metric = BrewMetric.CupWeight, value = "20")))
        assertEquals(BrewMetric.PhaseTime, editor.viewState.value.conditions.single().metric)
        assertEquals(20, assertNotNull(editor.viewState.value.toStep()).timeSec)
    }

    @Test fun renameOnlyChangesNameWhenConfirmed() {
        val editor = editor()
        editor.handleEvent(StepEditorEvent.RenameClicked)
        editor.handleEvent(StepEditorEvent.RenameDismissed)
        assertEquals("", editor.viewState.value.name)
        editor.handleEvent(StepEditorEvent.NameChanged(" Bloom "))
        assertEquals("Bloom", assertNotNull(editor.viewState.value.toStep()).phaseName)
    }

    @Test fun numericDialogValidatesAndCanBeCancelled() {
        val editor = editor()
        editor.handleEvent(StepEditorEvent.InputClicked(StepInput.Target))
        editor.handleEvent(StepEditorEvent.InputConfirmed(20f))
        assertEquals(9f, assertNotNull(editor.viewState.value.toStep()).value)
        editor.handleEvent(StepEditorEvent.InputDismissed)
        assertEquals(null, editor.viewState.value.input)
        editor.handleEvent(StepEditorEvent.InputClicked(StepInput.Target))
        editor.handleEvent(StepEditorEvent.InputConfirmed(5.5f))
        assertEquals(5.5f, assertNotNull(editor.viewState.value.toStep()).value)
        assertEquals(null, editor.viewState.value.input)
    }

    @Test fun switchingControlKeepsIndependentTargets() {
        val editor = editor()
        editor.handleEvent(StepEditorEvent.TargetChanged("6,5"))
        editor.handleEvent(StepEditorEvent.TypeChanged(StepType.Flow))
        editor.handleEvent(StepEditorEvent.TargetChanged("3"))
        editor.handleEvent(StepEditorEvent.TypeChanged(StepType.Pressure))
        assertEquals(6.5f, assertNotNull(editor.viewState.value.toStep()).value)
        editor.handleEvent(StepEditorEvent.TypeChanged(StepType.Flow))
        assertEquals(3f, assertNotNull(editor.viewState.value.toStep()).value)
    }

    @Test fun removingConditionPreservesRemainingIdentity() {
        val editor = editor()
        assertEquals(BrewMetric.PhaseTime, editor.viewState.value.conditions.single().metric)
        repeat(2) { editor.handleEvent(StepEditorEvent.ConditionAdded) }
        val last = editor.viewState.value.conditions.last()
        editor.handleEvent(StepEditorEvent.ConditionRemoved(1))
        assertEquals(listOf(0L, last.id), editor.viewState.value.conditions.map { it.id })
        assertEquals(last, editor.viewState.value.conditions.last())
    }
}
