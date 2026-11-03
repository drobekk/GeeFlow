package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.ProfileStep

internal fun buildStageProfileWrites(
    profile: BrewProfile,
    isBinding: Boolean,
    isWeight: Boolean,
    targetValue: Float,
    format: WendougeeProfileFormat,
): List<ProfileWrite> {
    require(targetValue.isFinite() && targetValue > 0 && targetValue <= MAX_REGISTER_VALUE) { "Invalid finish target" }
    val base = if (isBinding) WendougeeRegisters.CONSTANT_MODE_BOUND_BASE else WendougeeRegisters.CONSTANT_MODE_BASE
    val steps = profile.machineSteps()
    return buildList {
        add(ProfileWrite.Multiple(base, stageHeader(format, isWeight, targetValue, profile.autoLinkOpen)))
        steps.forEachIndexed { index, step ->
            val last = index == steps.lastIndex
            val values = when (format) {
                WendougeeProfileFormat.Legacy -> step.legacyWords(last)
                WendougeeProfileFormat.Packed -> step.packedWords(last)
            }
            require(values.all { it in 0..MAX_REGISTER_VALUE }) { "Profile step is outside the register range" }
            add(ProfileWrite.Multiple(base + HEADER_SIZE + index * STEP_SIZE, values))
        }
        val modeRegister = if (isBinding) WendougeeRegisters.BOUND_PROFILE_MODE else WendougeeRegisters.FV_PROFILE_MODE
        add(ProfileWrite.Single(modeRegister, NATIVE_STAGE_MODE))
    }
}

private fun stageHeader(
    format: WendougeeProfileFormat,
    isWeight: Boolean,
    targetValue: Float,
    autoLink: Boolean,
): List<Int> = when (format) {
    WendougeeProfileFormat.Legacy -> listOf(
        if (isWeight) 0 else 1,
        1,
        1,
        1,
        if (isWeight) 0 else targetValue.toInt(),
        if (isWeight) targetValue.toInt() else 0,
        if (autoLink) 1 else 0,
    )
    WendougeeProfileFormat.Packed -> listOf(
        // Native timed stages: pressureMode=1, craft.mode=1, direct=0; independent of step priority.
        (if (isWeight) 0 else 1) or PRESSURE_MODE or TIMED_STAGE_MODE or HAS_WEIGHT or
            (if (autoLink) AUTO_LINK else 0),
        if (isWeight) 0 else targetValue.toInt(),
        if (isWeight) targetValue.toInt() else 0,
        0,
        0,
        0,
        0,
        0,
    )
}

private fun BrewProfile.machineSteps(): List<MachineStep> = buildList {
    steps.forEach { step ->
        when (step) {
            is ProfileStep.Wait -> {
                val previous = removeAt(lastIndex)
                add(previous.copy(awaitTime = step.time))
            }
            is ProfileStep.Pressure -> add(MachineStep(step.time, (step.pressure * SENSOR_SCALE).toInt(), 0, false))
            is ProfileStep.Flow -> add(MachineStep(step.time, 0, (step.flow * SENSOR_SCALE).toInt(), true))
        }
    }
}

private data class MachineStep(
    val time: Int,
    val pressure: Int,
    val flow: Int,
    val flowPriority: Boolean,
    val awaitTime: Int = 0,
) {
    fun legacyWords(last: Boolean): List<Int> = listOf(
        time,
        pressure,
        flow,
        if (last) 0 else awaitTime,
        if (last) 1 else 0,
        if (flowPriority) 1 else 0,
    )

    fun packedWords(last: Boolean): List<Int> {
        require(pressure in 0..MAX_BYTE_VALUE && flow in 0..MAX_BYTE_VALUE) { "Profile target exceeds one byte" }
        return listOf(
            pressure or (flow shl BYTE_BITS),
            (if (last) 1 else 0) or (if (flowPriority) FLOW_PRIORITY else 0),
            0,
            time,
            if (last) 0 else awaitTime,
            0,
            0,
            0,
            0,
        )
    }
}

private const val MAX_REGISTER_VALUE = 65535
private const val MAX_BYTE_VALUE = 255
private const val BYTE_BITS = 8
private const val HEADER_SIZE = 8
private const val STEP_SIZE = 9
private const val SENSOR_SCALE = 10f
private const val PRESSURE_MODE = 1 shl 2
private const val TIMED_STAGE_MODE = 1 shl 4
private const val HAS_WEIGHT = 1 shl 8
private const val AUTO_LINK = 1 shl 9
private const val FLOW_PRIORITY = 1 shl 1
private const val NATIVE_STAGE_MODE = 2
