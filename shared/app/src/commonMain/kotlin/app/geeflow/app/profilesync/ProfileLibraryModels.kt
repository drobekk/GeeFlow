package app.geeflow.app.profilesync

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.SingleDoseSettings
import app.geeflow.data.brew.model.validate
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class ProfileLibraryCatalog(
    val schemaVersion: Int = 1,
    val revision: Long = 0,
    val profiles: List<ProfileLibraryRecord> = emptyList(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
    }
}

@Serializable
data class ProfileLibraryRecord(
    val id: String,
    val revision: Long = 0,
    val favorite: Boolean = false,
    val deleted: Boolean = false,
    val profile: PortableBrewProfile,
)

/** Fields that belong to a recipe, without app-local database IDs or list positions. */
@Serializable
data class PortableBrewProfile(
    val name: String,
    val description: String,
    val finishCondition: Condition? = null,
    val autoLinkOpen: Boolean = false,
    val singleDoseSettings: SingleDoseSettings = SingleDoseSettings(),
    val program: BrewProgram,
) {
    fun toLocalProfile(userId: Long, position: Int, id: Long = BrewProfile.NEW_ID): BrewProfile {
        program.validate()
        return BrewProfile(
            id = id,
            userId = userId,
            name = name,
            description = description,
            finishCondition = finishCondition,
            autoLinkOpen = autoLinkOpen,
            singleDoseSettings = singleDoseSettings,
            program = program,
            position = position,
        )
    }
}

fun BrewProfile.toPortableProfile() = PortableBrewProfile(
    name = name,
    description = description,
    finishCondition = finishCondition,
    autoLinkOpen = autoLinkOpen,
    singleDoseSettings = singleDoseSettings,
    program = program,
)

@Serializable
data class ProfileSyncMetadata(
    val catalogRevision: Long = 0,
    val entries: List<ProfileSyncEntry> = emptyList(),
)

@Serializable
data class ProfileSyncEntry(
    val cloudId: String,
    val localProfileId: Long,
    val baselineProfile: PortableBrewProfile? = null,
    val baselineFavorite: Boolean = false,
    val baselineDeleted: Boolean = false,
    val remoteRevision: Long = 0,
)

internal fun newProfileLibraryId(): String = Uuid.random().toString()
