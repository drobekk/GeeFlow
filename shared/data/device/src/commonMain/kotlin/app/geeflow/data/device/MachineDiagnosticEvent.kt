package app.geeflow.data.device

import kotlinx.coroutines.flow.SharedFlow
import kotlin.time.Instant

data class MachineDiagnosticEvent(
    val name: String,
    val receivedAt: Instant,
    val fields: Map<String, String>,
)

interface MachineDiagnosticEventSource {
    val diagnosticEvents: SharedFlow<MachineDiagnosticEvent>
}
