package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ProfileMode
import app.geeflow.data.brew.model.ProfileStep

/**
 * One Modbus register write that makes up part of a profile upload. The controller
 * dispatches these to [app.geeflow.data.device.ble.modbus.ModbusSession]
 * — framing, CRC, and ACK matching live there.
 */
sealed interface ProfileWrite {
    val register: Int

    data class Single(override val register: Int, val value: Int) : ProfileWrite
    data class Multiple(override val register: Int, val values: List<Int>) : ProfileWrite
}

class WendougeeProfileCompiler {

    fun buildProfileWrites(profile: BrewProfile, isBinding: Boolean = false): List<ProfileWrite> {
        val isWeight = profile.finishCondition is Condition.Weight
        val targetValue = when (val cond = profile.finishCondition) {
            is Condition.Weight -> cond.target
            is Condition.Volume -> cond.target
        }
        val realMode = when (profile.mode) {
            ProfileMode.VariablePressure -> 2
            ProfileMode.ConstantPressure -> 1
            ProfileMode.FreeVariable -> FV_FREE_MODE
        }
        return if (profile.mode == ProfileMode.FreeVariable) {
            buildFreeVariableWrites(profile, realMode, isBinding, isWeight, targetValue)
        } else {
            buildConstantModeWrites(profile, realMode, isBinding, isWeight, targetValue)
        }
    }

    private fun buildFreeVariableWrites(
        profile: BrewProfile,
        realMode: Int,
        isBinding: Boolean,
        isWeight: Boolean,
        targetValue: Float,
    ): List<ProfileWrite> {
        val writes = mutableListOf<ProfileWrite>()
        val points = resampleSteps(profile.steps, RESAMPLE_COUNT, RESAMPLE_INTERVAL)

        val registers = WendougeeRegisters.FV_MEMORY_REGIONS
        val dataTypes = listOf("bar", "bar", "rflow", "rflow", "weight", "weight", "flow", "flow")

        for (k in 0 until FV_REGION_COUNT) {
            val reg = registers[k]
            val type = dataTypes[k]
            val isSecondHalf = k % 2 != 0
            val startIndex = if (isSecondHalf) FV_HALF_COUNT else 0

            val values = (0 until FV_HALF_COUNT).map { i ->
                val point = points.getOrNull(startIndex + i)
                when (type) {
                    "bar" -> ((point?.pressure ?: 0f) * SENSOR_SCALE_FACTOR).toInt()
                    "flow", "rflow" -> ((point?.flow ?: 0f) * SENSOR_SCALE_FACTOR).toInt()
                    else -> 0
                }
            }
            writes.add(ProfileWrite.Multiple(reg, values))
        }

        writes.add(ProfileWrite.Multiple(WendougeeRegisters.FV_FINISH_CONDITION, listOf(if (isWeight) 0 else 1)))
        val modeRegister = if (isBinding) WendougeeRegisters.BOUND_PROFILE_MODE else WendougeeRegisters.FV_PROFILE_MODE
        writes.add(ProfileWrite.Single(modeRegister, realMode))
        writes.add(ProfileWrite.Single(WendougeeRegisters.FV_OFFSET_ZEROING, 0))
        writes.add(ProfileWrite.Single(WendougeeRegisters.FV_TARGET_VALUE, targetValue.toInt()))
        writes.add(ProfileWrite.Single(WendougeeRegisters.FV_AUTO_LINK, if (profile.autoLinkOpen) 1 else 0))
        return writes
    }

    private fun buildConstantModeWrites(
        profile: BrewProfile,
        realMode: Int,
        isBinding: Boolean,
        isWeight: Boolean,
        targetValue: Float,
    ): List<ProfileWrite> {
        val writes = mutableListOf<ProfileWrite>()
        val startReg = if (isBinding) WendougeeRegisters.CONSTANT_MODE_BOUND_BASE else WendougeeRegisters.CONSTANT_MODE_BASE

        val headerValues = listOf(
            if (isWeight) 0 else 1,
            if (profile.mode == ProfileMode.VariablePressure) 1 else 0,
            1,
            1,
            if (!isWeight) targetValue.toInt() else 0,
            if (isWeight) targetValue.toInt() else 0,
            if (profile.autoLinkOpen) 1 else 0,
        )
        writes.add(ProfileWrite.Multiple(startReg, headerValues))

        data class MachineStep(
            val time: Int,
            val bar: Float,
            val flow: Float,
            var awaitTime: Int = 0,
            val isFlowPriority: Boolean,
        )

        val machineSteps = mutableListOf<MachineStep>()
        profile.steps.forEach { step ->
            when (step) {
                is ProfileStep.Wait -> machineSteps.lastOrNull()?.let { it.awaitTime = step.time }
                is ProfileStep.Pressure -> machineSteps.add(MachineStep(step.time, step.pressure, 0f, 0, false))
                is ProfileStep.Flow -> machineSteps.add(MachineStep(step.time, 0f, step.flow, 0, true))
            }
        }

        var currentReg = startReg + CONSTANT_MODE_HEADER_OFFSET
        machineSteps.forEachIndexed { index, step ->
            val isLast = index == machineSteps.size - 1
            val stepValues = listOf(
                step.time,
                (step.bar * SENSOR_SCALE_FACTOR).toInt(),
                (step.flow * SENSOR_SCALE_FACTOR).toInt(),
                step.awaitTime,
                if (isLast) 1 else 0,
                if (step.isFlowPriority) 1 else 0,
            )
            writes.add(ProfileWrite.Multiple(currentReg, stepValues))
            currentReg += STEP_REGISTER_SIZE
        }

        val modeRegister = if (isBinding) WendougeeRegisters.BOUND_PROFILE_MODE else WendougeeRegisters.FV_PROFILE_MODE
        writes.add(ProfileWrite.Single(modeRegister, realMode))
        return writes
    }

    private data class ResampledPoint(val pressure: Float, val flow: Float)

    private fun resampleSteps(steps: List<ProfileStep>, count: Int, interval: Float): List<ResampledPoint> {
        val result = mutableListOf<ResampledPoint>()
        var currentPressure = 0f
        var currentFlow = 0f

        val timeline = mutableListOf<ResampledPoint>()
        steps.forEach { step ->
            val durationTicks = (step.time / interval).toInt()
            when (step) {
                is ProfileStep.Pressure -> currentPressure = step.pressure
                is ProfileStep.Flow -> currentFlow = step.flow
                is ProfileStep.Wait -> {
                    currentPressure = 0f
                    currentFlow = 0f
                }
            }
            repeat(durationTicks) {
                timeline.add(ResampledPoint(currentPressure, currentFlow))
            }
        }

        repeat(count) { i ->
            result.add(timeline.getOrNull(i) ?: ResampledPoint(0f, 0f))
        }
        return result
    }

    companion object {
        private const val SENSOR_SCALE_FACTOR = 10f
        private const val FV_FREE_MODE = 4
        private const val RESAMPLE_COUNT = 128
        private const val RESAMPLE_INTERVAL = 0.1f
        private const val FV_REGION_COUNT = 8
        private const val FV_HALF_COUNT = 64
        private const val CONSTANT_MODE_HEADER_OFFSET = 8
        private const val STEP_REGISTER_SIZE = 9
    }
}
