package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.device.TimedPumpProfileSupport
import app.geeflow.domain.exception.RecordingCapacityExceededException

/**
 * One Modbus register write that makes up part of a profile upload. The controller
 * dispatches these to [app.geeflow.data.device.ble.modbus.ModbusSession]
 * Framing, CRC, and ACK matching live there.
 */
sealed interface ProfileWrite {
    val register: Int

    data class Single(override val register: Int, val value: Int) : ProfileWrite
    data class Multiple(override val register: Int, val values: List<Int>) : ProfileWrite
}

class WendougeeProfileCompiler(
    private val format: WendougeeProfileFormat = WendougeeProfileFormat.Legacy,
) {

    fun buildProfileWrites(profile: BrewProfile, isBinding: Boolean = false): List<ProfileWrite> {
        val isWeight = profile.finishCondition is Condition.Weight
        val targetValue = when (val cond = profile.finishCondition) {
            is Condition.Weight -> cond.target
            is Condition.Volume -> cond.target
            null -> error("Native profiles require a finish target")
        }
        if (profile.recording?.playbackPoints()?.size?.let {
                it > TimedPumpProfileSupport.MAX_RECORDING_POINTS
            } == true
        ) {
            throw RecordingCapacityExceededException()
        }
        require(TimedPumpProfileSupport.assessNative(profile).isEmpty()) {
            "Profile cannot run natively on this device"
        }
        return if (profile.recording != null) {
            buildFreeVariableWrites(profile, FV_FREE_MODE, isBinding, isWeight, targetValue)
        } else {
            buildStageProfileWrites(profile, isBinding, isWeight, targetValue, format)
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
            it.copy(pressure = it.pressure.coerceAtMost(TimedPumpProfileSupport.MAX_PRESSURE))
        }
        if (points.size > TimedPumpProfileSupport.MAX_RECORDING_POINTS) throw RecordingCapacityExceededException()
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
            // Native bank encoding sends weight as whole grams, unlike live telemetry's decigrams.
            if (index == FV_TABLE_SIZE - 1) {
                if (isWeight) targetValue.toInt() else last.weight.toInt()
            } else {
                (points.getOrNull(index) ?: last).weight.toInt()
            }
        }
        val blocks = listOf(pressure, volume, weight, flow).flatMap { it.chunked(FV_HALF_COUNT) }
        blocks.zip(WendougeeRegisters.FV_MEMORY_REGIONS).forEach { (values, register) ->
            require(values.all { it in 0..MAX_REGISTER_VALUE }) { "Profile measurement is outside the register range" }
            writes.add(ProfileWrite.Multiple(register, values))
        }
        val modeRegister = if (isBinding) WendougeeRegisters.BOUND_PROFILE_MODE else WendougeeRegisters.FV_PROFILE_MODE
        val keyOffset = modeRegister - WendougeeRegisters.FV_PROFILE_MODE
        writes.add(ProfileWrite.Multiple(WendougeeRegisters.FV_FINISH_CONDITION + keyOffset, listOf(if (isWeight) 0 else 1)))
        writes.add(ProfileWrite.Multiple(modeRegister, listOf(realMode)))
        writes.add(
            ProfileWrite.Single(
                WendougeeRegisters.FV_CONTROL_MODE + keyOffset,
                if (recording.controlMode == FreeHandControlMode.Flow) 1 else 0,
            ),
        )
        writes.add(ProfileWrite.Single(WendougeeRegisters.FV_TARGET_VALUE + keyOffset, targetValue.toInt()))
        writes.add(ProfileWrite.Single(WendougeeRegisters.FV_AUTO_LINK + keyOffset, if (profile.autoLinkOpen) 1 else 0))
        writes.add(ProfileWrite.Multiple(WendougeeRegisters.FV_TABLE_ENDS, listOf(FV_TABLE_ENDS_VALUE)))
        return writes
    }

    companion object {
        /** Native free-profile generator commits the uploaded banks to the selected shortcut. */
        fun profileCommitCoil(isBinding: Boolean): Int = if (isBinding) BOUND_PROFILE_COMMIT_COIL else PROFILE_COMMIT_COIL

        private const val PROFILE_COMMIT_COIL = 158
        private const val BOUND_PROFILE_COMMIT_COIL = 159
        private const val SENSOR_SCALE_FACTOR = 10f
        private const val FV_FREE_MODE = 4
        private const val FV_HALF_COUNT = 64
        private const val FV_TABLE_SIZE = 128
        private const val MAX_REGISTER_VALUE = 65535
        private const val FV_TABLE_ENDS_VALUE = 0x7f7f
    }
}
