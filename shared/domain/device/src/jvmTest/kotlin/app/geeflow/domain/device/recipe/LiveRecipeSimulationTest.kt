package app.geeflow.domain.device.recipe

import app.geeflow.domain.device.*
import java.io.File
import kotlin.test.*
import kotlin.time.Instant
import kotlin.time.Duration.Companion.milliseconds

/** Deliberately simple synthetic plant, used to verify control bounds and capture output, not hydraulic fidelity. */
class LiveRecipeSimulationTest {
    @Test fun captureAllRecipeVariantsWithRestrictiveAndPermissiveSyntheticPucks() {
        fun source(name: String) = LiveRecipeImport.parse(requireNotNull(javaClass.getResource("/live_recipes/$name")).readText(), name)
        val adaptive = source("best_practice.json")
        val variants = listOf("cremina" to source("cremina.json"), "machine-flow" to adaptive,
            "cup-flow" to LiveRecipeImport.cupFlowCopy(adaptive, adaptive.phases.last().id, 1.5f, "cup-fixture"))
        val directory = File("build/live-recipe-simulations").apply { check(isDirectory || mkdirs()) }
        val epoch = Instant.parse("2026-11-10T00:00:00Z")
        val capturedStarts = mutableMapOf<String, MutableList<Float>>()
        variants.forEach { (variant, document) ->
            listOf("restrictive" to .15f, "permissive" to .28f).forEach { (puck, conductance) ->
                val engine = LiveRecipeEngine(LiveRecipeCompiler.compile(document))
                val estimator = CupFlowEstimator()
                var pressure = 0f; var machineFlow = 0f; var weight = 0f; var volume = 0f; var command = 0f
                var finish: String? = null
                val samples = mutableListOf<LiveTestSample>()
                val events = mutableListOf(LiveTestCaptureEvent("configuration", epoch, 0, mapOf(
                    "recipe_snapshot" to LiveRecipeJson.encode(document),
                    "simulation_only" to "true", "plant" to "first-order pressure with 350 ms response; flow=conductance*pressure; cup mass arrival=0.8*machine flow after 600 ms; not a measured machine model",
                )))
                val delayedFlow = ArrayDeque<Float>()
                for (tick in 0..(document.settings.deadlineMillis / 100).toInt()) {
                    val now = tick * 100L
                    pressure += (command - pressure) * (.1f / .35f)
                    machineFlow = conductance * pressure
                    volume += machineFlow * .1f
                    delayedFlow.addLast(machineFlow)
                    if (delayedFlow.size > 6) weight += delayedFlow.removeFirst() * .8f * .1f
                    val cupFlow = estimator.add(now, weight)
                    val measurements = RecipeMeasurements(buildMap {
                        put(RecipeMetric.Weight, TimedRecipeValue(weight, now)); put(RecipeMetric.Volume, TimedRecipeValue(volume, now))
                        put(RecipeMetric.Pressure, TimedRecipeValue(pressure, now)); put(RecipeMetric.MachineFlow, TimedRecipeValue(machineFlow, now))
                        cupFlow?.let { put(RecipeMetric.CupFlow, TimedRecipeValue(it, now)) }
                    })
                    val output = engine.tick(now, measurements)
                    output.events.forEach { event ->
                        events += LiveTestCaptureEvent(event.type, epoch + now.milliseconds, now, event.fields)
                        if (event.type == "measurement_captured" && event.fields["metric"] == if (variant == "cup-flow") "CupFlow" else "MachineFlow")
                            capturedStarts.getOrPut(variant) { mutableListOf() } += event.fields.getValue("value").toFloat()
                    }
                    finish = output.finish
                    if (finish != null) { events += LiveTestCaptureEvent("run_ended", epoch + now.milliseconds, now, mapOf("reason" to finish, "simulation_only" to "true")); break }
                    command = requireNotNull(output.appliedPressureBar)
                    assertTrue(command.isFinite() && command in 0f..document.settings.pressureCeilingBar)
                    samples += LiveTestSample(now, epoch + now.milliseconds, epoch + now.milliseconds, 0, 0, output.phaseId,
                        pressure, machineFlow, weight, cupFlow, null, command, output.requestedTarget, output.targetUnit?.name,
                        output.requestedPressureBar, command, output.constraints.joinToString(","))
                }
                assertNotNull(finish)
                assertTrue(samples.isNotEmpty())
                val state = LiveTestRunState(mode = LiveTestMode.ShadowControl, recipeSnapshot = LiveRecipeJson.encode(document), samples = samples, captureEvents = events)
                File(directory, "$variant-$puck.csv").writeText(LiveTestCaptureFormatter.csv(state))
                File(directory, "$variant-$puck.jsonl").writeText(LiveTestCaptureFormatter.jsonl(state))
                File(directory, "$variant-$puck-report.txt").writeText("SYNTHETIC SIMULATION ONLY\n" + LiveTestCaptureFormatter.verificationReport(state))
            }
        }
        listOf("machine-flow", "cup-flow").forEach { variant ->
            val values = requireNotNull(capturedStarts[variant])
            assertEquals(2, values.size)
            assertTrue(kotlin.math.abs(values[0] - values[1]) > .05f)
        }
    }
}
