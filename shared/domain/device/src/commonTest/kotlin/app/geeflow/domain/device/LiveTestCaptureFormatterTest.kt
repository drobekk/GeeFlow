package app.geeflow.domain.device

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.time.Clock

class LiveTestCaptureFormatterTest {
    @Test fun exportsVersionedJsonlCsvAndVerificationReport() {
        val state = LiveTestRunState(
            mode = LiveTestMode.ShadowControl,
            captureEvents = listOf(
                LiveTestCaptureEvent("configuration", Clock.System.now(), 0, mapOf("settings_validated_for_physical_control" to "false")),
                LiveTestCaptureEvent("scale_packet", Clock.System.now(), null, mapOf("packet_hex" to "030B\"00")),
                LiveTestCaptureEvent("command_enqueued", Clock.System.now(), 100, mapOf("command_id" to "1")),
                LiveTestCaptureEvent("command_acknowledged", Clock.System.now(), 110, mapOf("acknowledgement_latency_ms" to "18")),
                LiveTestCaptureEvent("stop_confirmation", Clock.System.now(), null, mapOf("confirmed" to "true")),
                LiveTestCaptureEvent("run_ended", Clock.System.now(), null, mapOf("reason" to "user_stop")),
            ),
        )

        val jsonl = LiveTestCaptureFormatter.jsonl(state)
        assertEquals(6, jsonl.lineSequence().filter(String::isNotEmpty).count())
        assertContains(jsonl, "\"schema_version\":1")
        assertContains(jsonl, "030B\\\"00")
        assertContains(LiveTestCaptureFormatter.csv(state), "elapsed_ms,machine_telemetry_at")
        assertContains(LiveTestCaptureFormatter.verificationReport(state), "Maximum command acknowledgement latency: 18 ms")
        assertContains(LiveTestCaptureFormatter.verificationReport(state), "Hydraulic response validated: no")
    }
}
