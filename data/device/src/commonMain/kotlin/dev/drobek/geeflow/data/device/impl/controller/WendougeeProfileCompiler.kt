package dev.drobek.geeflow.data.device.impl.controller

import dev.drobek.geeflow.data.brew.model.BrewProfile
import dev.drobek.geeflow.data.brew.model.Condition
import dev.drobek.geeflow.data.brew.model.ProfileMode
import dev.drobek.geeflow.data.brew.model.ProfileStep
import dev.drobek.geeflow.data.device.ble.ModbusCrcCalculator

data class ModbusCommand(val payload: ByteArray, val expectedFc: Byte, val regHi: Byte, val regLo: Byte)

class WendougeeProfileCompiler {

    fun buildProfileUploadCommands(profile: BrewProfile, isBinding: Boolean = false): List<ModbusCommand> {
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
            buildFreeVariableCommands(profile, realMode, isBinding, isWeight, targetValue)
        } else {
            buildConstantModeCommands(profile, realMode, isBinding, isWeight, targetValue)
        }
    }

    private fun buildFreeVariableCommands(
        profile: BrewProfile,
        realMode: Int,
        isBinding: Boolean,
        isWeight: Boolean,
        targetValue: Float,
    ): List<ModbusCommand> {
        val commands = mutableListOf<ModbusCommand>()
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
            commands.add(buildWriteMultiple(reg, values))
        }

        commands.add(buildWriteMultiple(WendougeeRegisters.FV_FINISH_CONDITION, listOf(if (isWeight) 0 else 1)))
        val modeRegister = if (isBinding) WendougeeRegisters.BOUND_PROFILE_MODE else WendougeeRegisters.FV_PROFILE_MODE
        commands.add(buildWriteSingle(modeRegister, realMode))
        commands.add(buildWriteSingle(WendougeeRegisters.FV_OFFSET_ZEROING, 0))
        commands.add(buildWriteSingle(WendougeeRegisters.FV_TARGET_VALUE, targetValue.toInt()))
        commands.add(buildWriteSingle(WendougeeRegisters.FV_AUTO_LINK, if (profile.autoLinkOpen) 1 else 0))
        return commands
    }

    private fun buildConstantModeCommands(
        profile: BrewProfile,
        realMode: Int,
        isBinding: Boolean,
        isWeight: Boolean,
        targetValue: Float,
    ): List<ModbusCommand> {
        val commands = mutableListOf<ModbusCommand>()
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
        commands.add(buildWriteMultiple(startReg, headerValues))

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
            commands.add(buildWriteMultiple(currentReg, stepValues))
            currentReg += STEP_REGISTER_SIZE
        }

        val modeRegister = if (isBinding) WendougeeRegisters.BOUND_PROFILE_MODE else WendougeeRegisters.FV_PROFILE_MODE
        commands.add(buildWriteSingle(modeRegister, realMode))
        return commands
    }

    private fun buildWriteSingle(reg: Int, value: Int): ModbusCommand {
        val regHi = (reg ushr BYTE_SHIFT).toByte()
        val regLo = (reg and BYTE_MASK).toByte()
        val header = byteArrayOf(
            DEVICE_ADDRESS,
            FC_WRITE_SINGLE,
            regHi,
            regLo,
            (value ushr BYTE_SHIFT).toByte(),
            (value and BYTE_MASK).toByte(),
        )
        return ModbusCommand(header + ModbusCrcCalculator.calculateCRC(header), FC_WRITE_SINGLE, regHi, regLo)
    }

    fun buildWriteMultiple(reg: Int, values: List<Int>): ModbusCommand {
        val regHi = (reg ushr BYTE_SHIFT).toByte()
        val regLo = (reg and BYTE_MASK).toByte()
        val num = values.size
        val byteCount = num * 2

        val header = byteArrayOf(
            DEVICE_ADDRESS,
            FC_WRITE_MULTIPLE,
            regHi,
            regLo,
            (num ushr BYTE_SHIFT).toByte(),
            (num and BYTE_MASK).toByte(),
            byteCount.toByte(),
        )

        val data = ByteArray(byteCount)
        values.forEachIndexed { i, v ->
            data[i * 2] = (v ushr BYTE_SHIFT).toByte()
            data[i * 2 + 1] = (v and BYTE_MASK).toByte()
        }

        val payload = header + data
        return ModbusCommand(payload + ModbusCrcCalculator.calculateCRC(payload), FC_WRITE_MULTIPLE, regHi, regLo)
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
        private const val BYTE_SHIFT = 8
        private const val BYTE_MASK = 0xFF
        private const val DEVICE_ADDRESS: Byte = 0x01
        private const val FC_WRITE_SINGLE: Byte = 0x06
        private const val FC_WRITE_MULTIPLE: Byte = 0x10
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
