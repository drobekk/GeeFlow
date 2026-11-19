package app.geeflow.app.profilesync

import app.geeflow.data.brew.model.*
import app.geeflow.data.brew.impl.BrewProgramAdapter
import app.geeflow.data.brew.provider.DefaultBrewProfileProvider
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.*

class ProfileLibraryV2SyncTest {
    private val fixture: ProfileLibraryCatalog get() = Json.decodeFromString(requireNotNull(javaClass.getResource("/catalog-v2.json")).readText())
    private fun coordinator() = ProfileLibrarySyncCoordinator(mockk(), mockk(), mockk(), DefaultBrewProfileProvider(), mockk(), mockk())
    @Test fun cloudRecipeIsCreatedLocallyAndDatabaseRoundTripIsLossless() {
        val remote=fixture
        val plan=coordinator().reconcile(remote,ProfileSyncMetadata(),1,emptyList(),emptySet(),emptySet())
        val local=assertNotNull(plan.profilesToCreate[remote.profiles.first().id])
        assertEquals(remote.profiles.first().profile,local.toPortableProfile())
        assertEquals(local.program,BrewProgramAdapter.decode(BrewProgramAdapter.encode(local.program)))
        assertEquals(2,plan.catalog.schemaVersion)
        val encoded=Json.encodeToString(plan.catalog)
        assertEquals(plan.catalog,Json.decodeFromString<ProfileLibraryCatalog>(encoded))
    }
    @Test fun appEditsAndFavoritesKeepSourceCapturesLimitsAndTombstones() {
        val catalog=fixture;val record=catalog.profiles.first();val local=record.profile.toLocalProfile(1,0,42).copy(name="App rename")
        val metadata=ProfileSyncMetadata(catalog.revision,listOf(ProfileSyncEntry(record.id,42,record.profile,false,false,record.revision)))
        val tombstone=record.copy(id="550e8400-e29b-41d4-a716-446655440010",deleted=true)
        val plan=coordinator().reconcile(catalog.copy(profiles=catalog.profiles+tombstone),metadata,1,listOf(local),setOf("42"),emptySet())
        val uploaded=plan.catalog.profiles.single { it.id==record.id }
        assertEquals("App rename",uploaded.profile.name);assertTrue(uploaded.favorite)
        assertEquals(record.profile.program,uploaded.profile.program)
        assertTrue(plan.catalog.profiles.single { it.id==tombstone.id }.deleted)
    }
    @Test fun metadataOnlyTabletEditPreservesAdvancedProgramAndTemplate() {
        val program=fixture.profiles.first().profile.program
        val edited=assertIs<BrewProgram.Phases>(program.withFinishConfiguration(Condition.Weight(30f)))
        assertEquals((program as BrewProgram.Phases).phases,edited.phases)
        assertEquals(program.recipe?.source,edited.recipe?.source)
        assertEquals(program.recipe?.templatePhases,edited.recipe?.templatePhases)
        assertEquals(30f,edited.recipe?.settings?.targetYieldG)
    }
    @Test fun rangeFeedbackSurvivesSyncAndDatabaseSerialization() {
        val record=fixture.profiles.first()
        val program=record.profile.program as BrewProgram.Phases
        val phase=program.phases.last().copy(secondaryLimit=SecondaryLimit(BrewMetric.PumpPressure,9.5f,minimum=7f))
        val ranged=record.copy(profile=record.profile.copy(program=program.copy(phases=program.phases.dropLast(1)+phase)))
        val catalog=fixture.copy(profiles=listOf(ranged))
        val plan=coordinator().reconcile(catalog,ProfileSyncMetadata(),1,emptyList(),emptySet(),emptySet())
        val local=assertNotNull(plan.profilesToCreate[ranged.id])
        assertEquals(ranged.profile.program,BrewProgramAdapter.decode(BrewProgramAdapter.encode(local.program)))
        assertEquals(ranged.profile.program,Json.decodeFromString<ProfileLibraryCatalog>(Json.encodeToString(plan.catalog)).profiles.first().profile.program)
    }
    @Test fun unknownCatalogVersionIsRejected() {
        assertFails { coordinator().reconcile(fixture.copy(schemaVersion=3),ProfileSyncMetadata(),1,emptyList(),emptySet(),emptySet()) }
    }
}
