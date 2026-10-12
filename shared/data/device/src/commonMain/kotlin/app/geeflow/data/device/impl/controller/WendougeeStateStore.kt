package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.SingleDoseGrinder
import app.geeflow.data.device.model.SmartScale
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal data class WendougeeSnapshot(
    val machine: DeviceState = DeviceState(),
    val scales: List<SmartScale> = emptyList(),
    val grinders: List<SingleDoseGrinder> = emptyList(),
    val integrationFlags: Int? = null,
)

internal data class DecodedWendougeeFrame(val snapshot: WendougeeSnapshot, val scanActive: Boolean?)

/** Decoding transforms an immutable snapshot; callbacks never mutate a StateFlow. */
internal fun decodeWendougeeFrame(frame: ByteArray, control: Boolean, initial: WendougeeSnapshot): DecodedWendougeeFrame {
    var next = initial
    var scanActive: Boolean? = null
    val parser = WendougeeFrameParser(
        onStateUpdate = { update -> next = next.copy(machine = next.machine.update()) },
        onScaleFound = { scale ->
            val current = if (scale.isConnected) next.scales.map { it.copy(isConnected = false) } else next.scales
            next = next.copy(scales = current.filterNot { it.name == scale.name } + scale)
        },
        onGrinderFound = { grinder ->
            val current = if (grinder.isConnected) next.grinders.map { it.copy(isConnected = false) } else next.grinders
            next = next.copy(grinders = current.filterNot { it.name == grinder.name } + grinder)
        },
        onScanStatus = { scanActive = it },
        onIntegrationFlags = { next = next.copy(integrationFlags = it) },
    )
    if (control) parser.parseControlFrame(frame) else parser.parseModbusFrame(frame)
    return DecodedWendougeeFrame(next, scanActive)
}

internal class WendougeeStateStore {
    private val snapshot = MutableStateFlow(WendougeeSnapshot())
    val machine: StateFlow<DeviceState> = SnapshotView(snapshot) { it.machine }
    val scales: StateFlow<List<SmartScale>> = SnapshotView(snapshot) { it.scales }
    val grinders: StateFlow<List<SingleDoseGrinder>> = SnapshotView(snapshot) { it.grinders }
    val flags: StateFlow<Int?> = SnapshotView(snapshot) { it.integrationFlags }

    fun updateMachine(update: (DeviceState) -> DeviceState) = snapshot.update { it.copy(machine = update(it.machine)) }
    fun setScales(scales: List<SmartScale>) = snapshot.update { it.copy(scales = scales) }
    fun setGrinders(grinders: List<SingleDoseGrinder>) = snapshot.update { it.copy(grinders = grinders) }
    fun setFlags(flags: Int?) = snapshot.update { it.copy(integrationFlags = flags) }
    fun reset() {
        snapshot.value = WendougeeSnapshot()
    }

    fun receive(frame: ByteArray, control: Boolean): Boolean? {
        while (true) {
            val before = snapshot.value
            val decoded = decodeWendougeeFrame(frame, control, before)
            if (snapshot.compareAndSet(before, decoded.snapshot)) return decoded.scanActive
        }
    }
}

/** Synchronous value projection; commands see the snapshot before an ACK is delivered. */
@OptIn(InternalCoroutinesApi::class)
private class SnapshotView<S, T>(private val source: StateFlow<S>, private val read: (S) -> T) : StateFlow<T> {
    override val value: T get() = read(source.value)
    override val replayCache: List<T> get() = listOf(value)
    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        source.map(read).distinctUntilChanged().collect(collector)
        error("A state flow cannot complete")
    }
}
