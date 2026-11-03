package app.geeflow.data.device.impl.controller

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.device.model.SupportedDevice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WendougeePackedProfileTest {
    @Test
    fun `when Data-S board reaches 18706 then selects packed stages`() {
        val versions = listOf(null, 180, 18705, 18706, 18707)

        val formats = versions.map { resolveWendougeeProfileFormat(SupportedDevice.WendougeeDataS, it) }

        assertEquals(
            listOf(
                WendougeeProfileFormat.Legacy,
                WendougeeProfileFormat.Legacy,
                WendougeeProfileFormat.Legacy,
                WendougeeProfileFormat.Packed,
                WendougeeProfileFormat.Packed,
            ),
            formats,
        )
    }

    @Test
    fun `when another model has a higher board version then keeps legacy stages`() {
        val models = listOf(
            SupportedDevice.LitaBA,
            SupportedDevice.LitaLA,
            SupportedDevice.LitaBR,
            SupportedDevice.LitaLR,
        )

        val formats = models.map { resolveWendougeeProfileFormat(it, 22413) }

        assertEquals(List(models.size) { WendougeeProfileFormat.Legacy }, formats)
    }

    @Test
    fun `when pressure stage ends by volume then matches native packed words`() {
        val profile = profile(listOf(ProfileStep.Pressure(5, 6f)))
        val compiler = WendougeeProfileCompiler(WendougeeProfileFormat.Packed)

        val writes = compiler.buildProfileWrites(profile)

        assertEquals(
            listOf(
                ProfileWrite.Multiple(2048, listOf(277, 40, 0, 0, 0, 0, 0, 0)),
                ProfileWrite.Multiple(2056, listOf(60, 1, 0, 5, 0, 0, 0, 0, 0)),
                ProfileWrite.Single(87, 2),
            ),
            writes,
        )
    }

    @Test
    fun `when mixed stages are bound with weight and auto link then packs both targets and flags`() {
        val profile = profile(
            listOf(ProfileStep.Pressure(5, 6f), ProfileStep.Wait(3), ProfileStep.Flow(10, 4.5f)),
        ).copy(finishCondition = Condition.Weight(36f), autoLinkOpen = true)
        val compiler = WendougeeProfileCompiler(WendougeeProfileFormat.Packed)

        val writes = compiler.buildProfileWrites(profile, isBinding = true)

        assertEquals(
            listOf(
                ProfileWrite.Multiple(2560, listOf(788, 0, 36, 0, 0, 0, 0, 0)),
                ProfileWrite.Multiple(2568, listOf(60, 0, 0, 5, 3, 0, 0, 0, 0)),
                ProfileWrite.Multiple(2577, listOf(11520, 3, 0, 10, 0, 0, 0, 0, 0)),
                ProfileWrite.Single(88, 2),
            ),
            writes,
        )
    }

    @Test
    fun `when packed flow reaches maximum then preserves its scaled byte`() {
        val profile = profile(listOf(ProfileStep.Flow(1, 12f)))
        val compiler = WendougeeProfileCompiler(WendougeeProfileFormat.Packed)

        val writes = compiler.buildProfileWrites(profile)

        assertEquals(ProfileWrite.Multiple(2056, listOf(30720, 3, 0, 1, 0, 0, 0, 0, 0)), writes[1])
    }

    @Test
    fun `when legacy stages are selected then preserves original register layout`() {
        val profile = profile(listOf(ProfileStep.Pressure(5, 6f), ProfileStep.Wait(3), ProfileStep.Flow(10, 4.5f)))
        val compiler = WendougeeProfileCompiler(WendougeeProfileFormat.Legacy)

        val writes = compiler.buildProfileWrites(profile)

        assertEquals(
            listOf(
                ProfileWrite.Multiple(2048, listOf(1, 1, 1, 1, 40, 0, 0)),
                ProfileWrite.Multiple(2056, listOf(5, 60, 0, 3, 0, 0)),
                ProfileWrite.Multiple(2065, listOf(10, 0, 45, 0, 1, 1)),
                ProfileWrite.Single(87, 2),
            ),
            writes,
        )
    }

    @Test
    fun `when packed finish target exceeds register capacity then rejects compilation`() {
        val profile = profile(listOf(ProfileStep.Pressure(5, 6f))).copy(finishCondition = Condition.Volume(65536f))
        val compiler = WendougeeProfileCompiler(WendougeeProfileFormat.Packed)

        val result = runCatching { compiler.buildProfileWrites(profile) }

        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }

    private fun profile(steps: List<ProfileStep>) = BrewProfile(
        userId = 1,
        name = "Native stages",
        description = "",
        finishCondition = Condition.Volume(40f),
        steps = steps,
    )
}
