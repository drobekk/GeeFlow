package dev.drobek.geeflow.data.device.impl.controller

import dev.drobek.geeflow.data.brew.model.BrewProfile
import dev.drobek.geeflow.data.brew.model.Condition
import dev.drobek.geeflow.data.brew.model.ProfileMode
import dev.drobek.geeflow.data.brew.model.ProfileStep
import dev.drobek.geeflow.data.device.ble.ModbusCrcCalculator

data class ModbusCommand(val payload: ByteArray, val expectedFc: Byte, val regHi: Byte, val regLo: Byte)

class WendougeeProfileCompiler {

    fun buildProfileUploadCommands(profile: BrewProfile, isBinding: Boolean = false): List<ModbusCommand> {
        val commands = mutableListOf<ModbusCommand>()
        val isWeight = profile.finishCondition is Condition.Weight
        val targetValue = when (val cond = profile.finishCondition) {
            is Condition.Weight -> cond.target
            is Condition.Volume -> cond.target
        }

        val realMode = when (profile.mode) {
            ProfileMode.VariablePressure -> 2
            ProfileMode.ConstantPressure -> 1
            ProfileMode.FreeVariable -> 4
        }

        if (profile.mode == ProfileMode.FreeVariable) {
            val points = resampleSteps(profile.steps, 128, 0.1f)

            val registers = WendougeeRegisters.FV_MEMORY_REGIONS
            val dataTypes = listOf("bar", "bar", "rflow", "rflow", "weight", "weight", "flow", "flow")

            for (k in 0 until 8) {
                val reg = registers[k]
                val type = dataTypes[k]
                val isSecondHalf = k % 2 != 0
                val startIndex = if (isSecondHalf) 64 else 0

                val values = (0 until 64).map { i ->
                    val point = points.getOrNull(startIndex + i)
                    when (type) {
                        "bar" -> ((point?.pressure ?: 0f) * 10).toInt()
                        "flow", "rflow" -> ((point?.flow ?: 0f) * 10).toInt()
                        "weight" -> 0
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
        } else {
            val startReg = if (isBinding) WendougeeRegisters.CONSTANT_MODE_BOUND_BASE else WendougeeRegisters.CONSTANT_MODE_BASE

            val headerValues = listOf(
                if (isWeight) 0 else 1,
                if (profile.mode == ProfileMode.VariablePressure) 1 else 0,
                1,
                1,
                if (!isWeight) targetValue.toInt() else 0,
                if (isWeight) targetValue.toInt() else 0,
                if (profile.autoLinkOpen) 1 else 0
            )
            commands.add(buildWriteMultiple(startReg, headerValues))

            data class MachineStep(
                val time: Int,
                val bar: Float,
                val flow: Float,
                var awaitTime: Int = 0,
                val isFlowPriority: Boolean
            )

            val machineSteps = mutableListOf<MachineStep>()
            profile.steps.forEach { step ->
                when (step) {
                    is ProfileStep.Wait -> {
                        machineSteps.lastOrNull()?.let { it.awaitTime = step.time }
                    }
                    is ProfileStep.Pressure -> machineSteps.add(MachineStep(step.time, step.pressure, 0f, 0, false))
                    is ProfileStep.Flow -> machineSteps.add(MachineStep(step.time, 0f, step.flow, 0, true))
                }
            }

            var currentReg = startReg + 8
            machineSteps.forEachIndexed { index, step ->
                val isLast = index == machineSteps.size - 1
                val stepValues = listOf(
                    step.time,
                    (step.bar * 10).toInt(),
                    (step.flow * 10).toInt(),
                    step.awaitTime,
                    if (isLast) 1 else 0,
                    if (step.isFlowPriority) 1 else 0
                )
                commands.add(buildWriteMultiple(currentReg, stepValues))
                currentReg += 9
            }

            val modeRegister = if (isBinding) WendougeeRegisters.BOUND_PROFILE_MODE else WendougeeRegisters.FV_PROFILE_MODE
            commands.add(buildWriteSingle(modeRegister, realMode))
        }

        return commands
    }

    private fun buildWriteSingle(reg: Int, value: Int): ModbusCommand {
        val regHi = (reg ushr 8).toByte()
        val regLo = (reg and 0xFF).toByte()
        val header = byteArrayOf(
            0x01,
            0x06,
            regHi,
            regLo,
            (value ushr 8).toByte(),
            (value and 0xFF).toByte()
        )
        return ModbusCommand(header + ModbusCrcCalculator.calculateCRC(header), 0x06, regHi, regLo)
    }

    fun buildWriteMultiple(reg: Int, values: List<Int>): ModbusCommand {
        val regHi = (reg ushr 8).toByte()
        val regLo = (reg and 0xFF).toByte()
        val num = values.size
        val byteCount = num * 2

        val header = byteArrayOf(
            0x01,
            0x10,
            regHi,
            regLo,
            (num ushr 8).toByte(),
            (num and 0xFF).toByte(),
            byteCount.toByte()
        )

        val data = ByteArray(byteCount)
        values.forEachIndexed { i, v ->
            data[i * 2] = (v ushr 8).toByte()
            data[i * 2 + 1] = (v and 0xFF).toByte()
        }

        val payload = header + data
        return ModbusCommand(payload + ModbusCrcCalculator.calculateCRC(payload), 0x10, regHi, regLo)
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
}
