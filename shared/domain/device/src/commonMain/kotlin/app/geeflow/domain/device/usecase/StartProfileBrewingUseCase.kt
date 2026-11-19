package app.geeflow.domain.device.usecase

import app.geeflow.data.brew.model.BrewMetric
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.ScaleReadingSource
import app.geeflow.data.user.UserRepository
import app.geeflow.domain.device.ProfileExecutionCoordinator
import app.geeflow.domain.device.AppWeightCutoff
import app.geeflow.domain.device.WeightCutoffSource
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import org.koin.core.annotation.Factory

@Factory
class StartProfileBrewingUseCase(
    private val provider: DeviceControllerProvider,
    private val userRepository: UserRepository,
    private val coordinator: ProfileExecutionCoordinator,
    private val scale: ScaleReadingSource,
) {
    suspend operator fun invoke(
        deviceId: Long,
        profile: BrewProfile,
        stopAtWeightGrams: Float? = null,
    ) = with(provider.getController(deviceId)) {
        requireReady(deviceId)
        val target = stopAtWeightGrams?.also {
            require(it.isFinite() && it > 0f) { "Yield target must be positive." }
        }
        val now = Clock.System.now()
        val machine = deviceState.value
        val machineWeight = machine.weight
        val machineScaleConnected = machine.smartScale?.isConnected == true
        val machineScaleFresh = machineWeight?.isFinite() == true && machine.telemetryTime?.let {
            (now - it).inWholeMilliseconds in 0..CUTOFF_SCALE_STALE_MILLIS
        } == true
        val machineScaleTared = machineWeight?.let {
            kotlin.math.abs(it) <= START_WEIGHT_TOLERANCE_GRAMS
        } == true
        val machineScaleReady = machineScaleConnected && machineScaleFresh && machineScaleTared
        val state = scale.state.value
        val reading = state.reading
        val bookooScaleReady = state.connected && reading != null &&
            (now - reading.receivedAt).inWholeMilliseconds in 0..CUTOFF_SCALE_STALE_MILLIS &&
            kotlin.math.abs(reading.frame.weightGrams) <= START_WEIGHT_TOLERANCE_GRAMS
        val weightSourceReady = machineScaleReady || bookooScaleReady
        val cutoff = target?.takeIf { weightSourceReady }?.let { targetGrams ->
            val source = if (machineScaleReady) WeightCutoffSource.MachineScale else WeightCutoffSource.Bookoo
            AppWeightCutoff(
                targetGrams,
                source,
                bookooConnected = { scale.state.value.connected },
                bookooReading = { scale.state.value.reading },
            )
        }
        val runtimeProfile = if (weightSourceReady) profile else profile.withoutScaleRequirements()
        val userId = userRepository.selectedUser.first()?.id ?: error("No user selected")
        coordinator.start(deviceId, runtimeProfile.copy(userId = userId), cutoff)
        target != null && cutoff == null
    }
}

private fun app.geeflow.data.brew.model.BrewProfile.withoutScaleRequirements(): app.geeflow.data.brew.model.BrewProfile {
    val phases = (program as? app.geeflow.data.brew.model.BrewProgram.Phases)?.phases ?: return copy(
        finishCondition = if (finishCondition is app.geeflow.data.brew.model.Condition.Weight) null else finishCondition,
    )
    val removedCaptures = phases.flatMap { it.entryCaptures + it.exitCaptures }
        .filter { it.metric == BrewMetric.CupWeight }
        .map { it.name }
        .toSet()
    fun withoutScaleSource(source: app.geeflow.data.brew.model.TargetSource?) = source?.takeUnless {
        it.metric == BrewMetric.CupWeight || it.capture in removedCaptures
    }
    val fallbackProgram = (program as app.geeflow.data.brew.model.BrewProgram.Phases).copy(
        phases = phases.map { phase ->
            phase.copy(
                exitConditions = phase.exitConditions.filterNot { it.metric == BrewMetric.CupWeight },
                entryCaptures = phase.entryCaptures.filterNot { it.metric == BrewMetric.CupWeight },
                exitCaptures = phase.exitCaptures.filterNot { it.metric == BrewMetric.CupWeight },
                targetSource = withoutScaleSource(phase.targetSource),
                ramp = phase.ramp.copy(source = withoutScaleSource(phase.ramp.source)),
            )
        },
    )
    return copy(
        finishCondition = if (finishCondition is app.geeflow.data.brew.model.Condition.Weight) null else finishCondition,
        program = fallbackProgram,
    )
}

private const val CUTOFF_SCALE_STALE_MILLIS = 1_000L
private const val START_WEIGHT_TOLERANCE_GRAMS = 2f
