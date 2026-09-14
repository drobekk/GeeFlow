package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.domain.exception.RecordingCapacityExceededException

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
            null -> error("Native profiles require a finish target")
        }
        if (profile.recording?.playbackPoints()?.size?.let { it > WendougeeProfiling.MAX_RECORDING_POINTS } == true) {
            throw RecordingCapacityExceededException()
        }
        require(WendougeeProfiling.assessNative(profile).isEmpty()) { "Profile cannot run natively on this device" }
        val realMode = if (profile.recording != null) FV_FREE_MODE else 2
        return if (profile.recording != null) {
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
        val recording = requireNotNull(profile.recording) { "Free variable profile has no recording" }
        val points = recording.playbackPoints().map {
            it.copy(pressure = it.pressure.coerceAtMost(WendougeeProfiling.MAX_PRESSURE))
        }
        if (points.size > WendougeeProfiling.MAX_RECORDING_POINTS) throw RecordingCapacityExceededException()
        require(targetValue.isFinite() && targetValue > 0 && targetValue <= MAX_REGISTER_VALUE) {
            "Invalid finish target"
        }
        val last = points.last()
        val pressure = List(FV_TABLE_SIZE) { ((points.getOrNull(it) ?: last).pressure * SENSOR_SCALE_FACTOR).toInt() }
        val flow = List(FV_TABLE_SIZE) { ((points.getOrNull(it) ?: last).flowRate * SENSOR_SCALE_FACTOR).toInt() }
        val volume = List(FV_TABLE_SIZE) { index ->
            if (index == FV_TABLE_SIZE - 1) last.volume.toInt() else points.getOrNull(index)?.volume?.toInt() ?: 0
        }
        val weight = List(FV_TABLE_SIZE) { index ->
            // The endpoint is a whole-gram target. Captures only verify zero measured weights;
            // nonzero samples use telemetry's decigram scale pending a recording with a scale.
            if (index == FV_TABLE_SIZE - 1) {
                if (isWeight) targetValue.toInt() else last.weight.toInt()
            } else {
                ((points.getOrNull(index)?.weight ?: 0f) * SENSOR_SCALE_FACTOR).toInt()
            }
        }
        val blocks = listOf(pressure, volume, weight, flow).flatMap { it.chunked(FV_HALF_COUNT) }
        blocks.zip(WendougeeRegisters.FV_MEMORY_REGIONS).forEach { (values, register) ->
            require(values.all { it in 0..MAX_REGISTER_VALUE }) { "Profile measurement is outside the register range" }
            writes.add(ProfileWrite.Multiple(register, values))
        }
        writes.add(ProfileWrite.Multiple(WendougeeRegisters.FV_FINISH_CONDITION, listOf(if (isWeight) 0 else 1)))
        val modeRegister = if (isBinding) WendougeeRegisters.BOUND_PROFILE_MODE else WendougeeRegisters.FV_PROFILE_MODE
        writes.add(ProfileWrite.Multiple(modeRegister, listOf(realMode)))
        writes.add(
            ProfileWrite.Single(
                WendougeeRegisters.FV_CONTROL_MODE,
                if (recording.controlMode == FreeHandControlMode.Flow) 1 else 0,
            ),
        )
        writes.add(ProfileWrite.Single(WendougeeRegisters.FV_TARGET_VALUE, targetValue.toInt()))
        writes.add(ProfileWrite.Single(WendougeeRegisters.FV_AUTO_LINK, if (profile.autoLinkOpen) 1 else 0))
        writes.add(ProfileWrite.Multiple(WendougeeRegisters.FV_TABLE_ENDS, listOf(FV_TABLE_ENDS_VALUE)))
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
            1,
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

    companion object {
        private const val SENSOR_SCALE_FACTOR = 10f
        private const val FV_FREE_MODE = 4
        private const val FV_HALF_COUNT = 64
        private const val FV_TABLE_SIZE = 128
        private const val MAX_REGISTER_VALUE = 65535
        private const val FV_TABLE_ENDS_VALUE = 0x7f7f
        private const val CONSTANT_MODE_HEADER_OFFSET = 8
        private const val STEP_REGISTER_SIZE = 9
    }
}
