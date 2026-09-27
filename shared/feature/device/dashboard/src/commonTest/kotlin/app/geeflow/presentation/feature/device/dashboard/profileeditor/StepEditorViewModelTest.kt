package app.geeflow.presentation.feature.device.dashboard.profileeditor

import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.ConditionOperator
import app.geeflow.data.brew.model.PressureLocation
import app.geeflow.data.brew.model.RampStyle
import app.geeflow.data.device.model.NativeProfilingCapabilities
import app.geeflow.data.device.model.ProfilingCapabilities
import app.geeflow.data.device.model.TargetRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StepEditorViewModelTest {
    @Test
    fun `condition operator toggles and survives saving the step`() {
        val editor = editor()
        assertEquals(ConditionOperator.Or, editor.viewState.value.conditionOperator)
        editor.handleEvent(StepEditorEvent.ConditionOperatorToggled)
        val step = assertNotNull(editor.viewState.value.toStep())
        assertEquals(ConditionOperator.And, step.toPhase().conditionOperator)
        val reopened = StepEditorViewState(step, ProfilingCapabilities(), 0f..12f, 0f..8f)
        assertEquals(ConditionOperator.And, reopened.conditionOperator)
        editor.handleEvent(StepEditorEvent.ConditionOperatorToggled)
        assertEquals(ConditionOperator.Or, editor.viewState.value.conditionOperator)
    }

    private fun editor(
        capabilities: ProfilingCapabilities = ProfilingCapabilities(
            native = NativeProfilingCapabilities(
                pressureLocations = setOf(PressureLocation.Pump),
                flow = true,
                exitMetrics = setOf(BrewMetric.PhaseTime),
            ),
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

    @Test
    fun `experimental flow step locks new steps to flow while wait remains available`() {
        val existing = ProfileEditorViewState.Step(
            id = 1,
            type = StepType.Flow,
            timeSec = 10,
            value = 4f,
            experimental = true,
        )
        val editor = StepEditorViewModel(
            step = ProfileEditorViewState.Step(id = 2, type = StepType.Flow, timeSec = 10, value = 4f),
            capabilities = ProfilingCapabilities(
                native = NativeProfilingCapabilities(
                    pressureLocations = setOf(PressureLocation.Pump),
                    flow = true,
                    pause = true,
                    exitMetrics = setOf(BrewMetric.PhaseTime),
                ),
            ),
            pressureRange = 0f..12f,
            flowRange = 0f..8f,
            profileSteps = listOf(existing),
        )

        assertEquals(StepType.Flow, editor.viewState.value.lockedControlType)
        assertTrue(editor.viewState.value.controlAvailable(StepType.Pressure))
        assertFalse(editor.viewState.value.controlSupported(StepType.Pressure))
        assertTrue(editor.viewState.value.controlSupported(StepType.Wait))
        editor.handleEvent(StepEditorEvent.TypeChanged(StepType.Pressure))
        assertEquals(StepType.Flow, editor.viewState.value.type)
    }

    @Test
    fun `first step can switch to flow before an experimental step exists`() {
        val editor = editor()

        editor.handleEvent(StepEditorEvent.ConditionAdded)
        assertNull(editor.viewState.value.lockedControlType)
        editor.handleEvent(StepEditorEvent.TypeChanged(StepType.Flow))
        assertEquals(StepType.Flow, editor.viewState.value.type)
    }

    @Test
    fun `draft experimental condition does not lock control before the step is saved`() {
        val existing = ProfileEditorViewState.Step(id = 1, type = StepType.Pressure, timeSec = 10, value = 9f)
        val editor = StepEditorViewModel(
            step = ProfileEditorViewState.Step(id = 2, type = StepType.Flow, timeSec = 10, value = 4f),
            capabilities = ProfilingCapabilities(
                native = NativeProfilingCapabilities(
                    pressureLocations = setOf(PressureLocation.Pump),
                    flow = true,
                    exitMetrics = setOf(BrewMetric.PhaseTime),
                ),
                livePressure = mapOf(PressureLocation.Pump to TargetRange(0f, 12f, .1f)),
                liveFlow = TargetRange(0f, 8f, .1f),
                telemetry = setOf(BrewMetric.PumpedVolume),
            ),
            pressureRange = 0f..12f,
            flowRange = 0f..8f,
            profileSteps = listOf(existing),
        )

        assertNull(editor.viewState.value.lockedControlType)
        editor.handleEvent(StepEditorEvent.ConditionAdded)
        assertNull(editor.viewState.value.lockedControlType)
        assertNotNull(editor.viewState.value.toStep())
        editor.handleEvent(StepEditorEvent.TypeChanged(StepType.Pressure))
        assertEquals(StepType.Pressure, editor.viewState.value.type)
        editor.handleEvent(StepEditorEvent.TypeChanged(StepType.Flow))
        assertEquals(StepType.Flow, editor.viewState.value.type)
    }
}
