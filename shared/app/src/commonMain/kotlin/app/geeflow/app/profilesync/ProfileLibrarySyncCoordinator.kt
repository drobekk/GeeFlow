package app.geeflow.app.profilesync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.geeflow.BuildKonfig
import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.provider.DefaultBrewProfileProvider
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.User
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileLibrarySyncController
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileLibrarySyncState
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import kotlin.time.Clock

@Single(binds = [ProfileLibrarySyncController::class])
class ProfileLibrarySyncCoordinator(
    private val dataStore: DataStore<Preferences>,
    private val api: ProfileLibraryApi,
    private val profiles: BrewProfileRepository,
    private val defaultProfiles: DefaultBrewProfileProvider,
    private val users: UserRepository,
    private val userSettings: UserSettingsRepository,
) : ProfileLibrarySyncController {
    private val log = Logger.withTag("ProfileLibrarySync")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()
    private val token = BuildKonfig.PROFILE_LIBRARY_TOKEN.trim()
    private val _state = MutableStateFlow(
        ProfileLibrarySyncState(
            phase = if (token.isBlank()) ProfileLibrarySyncState.Phase.NotConfigured else ProfileLibrarySyncState.Phase.Ready,
        ),
    )
    override val state: StateFlow<ProfileLibrarySyncState> = _state.asStateFlow()

    override suspend fun syncNow() {
        val selectedUser = users.selectedUser.first()
        if (selectedUser != null) syncUser(selectedUser)
    }

    private suspend fun syncUser(user: User) {
        mutex.withLock {
            if (token.isBlank()) {
                _state.value = ProfileLibrarySyncState(phase = ProfileLibrarySyncState.Phase.NotConfigured)
                return@withLock
            }
            _state.value = state.value.copy(phase = ProfileLibrarySyncState.Phase.Syncing, message = null)
            try {
                var completed = false
                var attempts = 0
                while (attempts < MAX_CONFLICT_RETRIES) {
                    val metadata = readMetadata(user.id)
                    val localProfiles = profiles.getBrewProfilesForUser(user.id)
                    val favoriteIds = userSettings.favoriteProfiles(user.id).first()
                    val pendingDeletions = profiles.pendingSyncDeletions(user.id).toSet()
                    val remote = api.fetch(token)
                    val plan = reconcile(remote, metadata, user.id, localProfiles, favoriteIds, pendingDeletions)

                    val accepted = if (sameRecords(plan.catalog.profiles, remote.profiles)) {
                        remote
                    } else {
                        try {
                            api.replace(token, remote.revision, plan.catalog.copy(revision = remote.revision))
                        } catch (conflict: ProfileLibraryRevisionConflict) {
                            attempts += 1
                            log.i { "Catalog revision conflict; retrying against revision ${conflict.catalog.revision}" }
                            if (attempts == MAX_CONFLICT_RETRIES) throw conflict
                            continue
                        }
                    }
                    applyPlan(plan, accepted, user.id)
                    completed = true
                    break
                }
                if (!completed) {
                    _state.value = ProfileLibrarySyncState(
                        phase = ProfileLibrarySyncState.Phase.Error,
                        message = "The profile library kept changing. Sync again in a moment.",
                    )
                }
            } catch (error: ProfileLibraryRequestException) {
                val phase = if (error.statusCode in 500..599) {
                    ProfileLibrarySyncState.Phase.Offline
                } else {
                    ProfileLibrarySyncState.Phase.Error
                }
                _state.value = ProfileLibrarySyncState(phase = phase, message = error.message)
            } catch (error: ProfileLibraryRevisionConflict) {
                _state.value = ProfileLibrarySyncState(
                    phase = ProfileLibrarySyncState.Phase.Error,
                    message = "The profile library kept changing. Sync again in a moment.",
                )
            } catch (error: Throwable) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                log.w(error) { "Profile sync failed" }
                _state.value = ProfileLibrarySyncState(
                    phase = ProfileLibrarySyncState.Phase.Offline,
                    message = "The profile library could not be reached. Local profiles are available offline.",
                )
            }
            if (_state.value.phase == ProfileLibrarySyncState.Phase.Syncing) {
                _state.value = ProfileLibrarySyncState(
                    phase = ProfileLibrarySyncState.Phase.Synced,
                    lastSyncedAt = Clock.System.now().toString(),
                )
            }
        }
    }

    internal fun reconcile(
        remote: ProfileLibraryCatalog,
        metadata: ProfileSyncMetadata,
        userId: Long,
        localProfiles: List<BrewProfile>,
        favoriteIds: Set<String>,
        pendingDeletions: Set<Long>,
    ): SyncPlan {
        require(remote.schemaVersion in 1..ProfileLibraryCatalog.CURRENT_SCHEMA_VERSION) {
            "Unsupported profile library schema ${remote.schemaVersion}"
        }
        val localById = localProfiles.associateBy { it.id }
        val defaultProfilesByName = defaultProfiles.getDefaultProfiles(userId)
            .associateBy { canonicalProfileName(it.name) }
        val records = remote.profiles.associateBy { it.id }.mapValues { (_, record) ->
            val standardDescription = defaultProfiles.standardizedDescription(
                record.profile.name,
                record.profile.description,
            )
            if (standardDescription == null || standardDescription == record.profile.description) record
            else record.copy(profile = record.profile.copy(description = standardDescription))
        }.toMutableMap()
        val entries = metadata.entries.associateBy { it.cloudId }.toMutableMap()
        val mappedLocalIds = entries.values.mapNotNull { it.localProfileId.takeIf { id -> id > 0 } }.toMutableSet()
        val remoteToLocal = mutableMapOf<String, Long>()
        val conflictCopies = mutableMapOf<String, BrewProfile>()
        val profilesToCreate = mutableMapOf<String, BrewProfile>()
        val profilesToDelete = mutableSetOf<Long>()

        entries.values.toList().forEach { entry ->
            val local = localById[entry.localProfileId]
            val record = records[entry.cloudId]

            if (record == null) {
                if (local != null) {
                    records[entry.cloudId] = ProfileLibraryRecord(
                        id = entry.cloudId,
                        profile = local.toPortableProfile(),
                        favorite = local.id.toString() in favoriteIds,
                    )
                    remoteToLocal[entry.cloudId] = local.id
                } else {
                    entries.remove(entry.cloudId)
                }
                return@forEach
            }

            if (local == null) {
                if (record.deleted) {
                    entries[entry.cloudId] = entry.copy(
                        localProfileId = 0,
                        baselineProfile = record.profile,
                        baselineFavorite = record.favorite,
                        baselineDeleted = true,
                    )
                } else if (entry.localProfileId in pendingDeletions) {
                    records[entry.cloudId] = record.copy(deleted = true)
                    entries[entry.cloudId] = entry.copy(
                        localProfileId = 0,
                        baselineProfile = record.profile,
                        baselineFavorite = record.favorite,
                        baselineDeleted = true,
                    )
                } else {
                    profilesToCreate[entry.cloudId] = record.profile.toLocalProfile(userId, localProfiles.size)
                    entries[entry.cloudId] = entry.copy(
                        baselineProfile = record.profile,
                        baselineFavorite = record.favorite,
                        baselineDeleted = false,
                    )
                }
                return@forEach
            }

            val localProfile = local.toPortableProfile()
            val localFavorite = local.id.toString() in favoriteIds
            val localChanged = (entry.baselineProfile?.let { it != localProfile } ?: true) ||
                entry.baselineFavorite != localFavorite
            val remoteChanged = record.revision != entry.remoteRevision

            if (record.deleted) {
                if (localChanged) {
                    val copyId = newProfileLibraryId()
                    val copyProfile = localProfile.copy(name = "${localProfile.name} (local copy)")
                    records[copyId] = ProfileLibraryRecord(
                        id = copyId,
                        favorite = localFavorite,
                        profile = copyProfile,
                    )
                    conflictCopies[copyId] = local.copy(name = copyProfile.name)
                }
                profilesToDelete += local.id
                mappedLocalIds -= local.id
                entries[entry.cloudId] = entry.copy(
                    localProfileId = 0,
                    baselineProfile = record.profile,
                    baselineFavorite = record.favorite,
                    baselineDeleted = true,
                )
                return@forEach
            }

            if (localChanged && remoteChanged &&
                (localProfile != record.profile || localFavorite != record.favorite)
            ) {
                val copyId = newProfileLibraryId()
                val copyProfile = localProfile.copy(name = "${localProfile.name} (local copy)")
                records[copyId] = ProfileLibraryRecord(id = copyId, favorite = localFavorite, profile = copyProfile)
                conflictCopies[copyId] = local.copy(name = copyProfile.name)
                remoteToLocal[entry.cloudId] = local.id
            } else if (localChanged && !remoteChanged) {
                records[entry.cloudId] = record.copy(profile = localProfile, favorite = localFavorite)
                remoteToLocal[entry.cloudId] = local.id
            } else {
                remoteToLocal[entry.cloudId] = local.id
            }
        }

        records.values.filter { it.id !in entries && it.id !in conflictCopies }.toList().forEach { record ->
            if (record.deleted) return@forEach
            val matchingLocal = localProfiles.firstOrNull { profile ->
                profile.id !in mappedLocalIds && profile.toPortableProfile() == record.profile
            } ?: localProfiles.firstOrNull { profile ->
                val default = defaultProfilesByName[canonicalProfileName(profile.name)]
                profile.id !in mappedLocalIds &&
                    canonicalProfileName(profile.name) == canonicalProfileName(record.profile.name) &&
                    default?.toPortableProfile() == profile.toPortableProfile()
            }
            if (matchingLocal != null) {
                remoteToLocal[record.id] = matchingLocal.id
                mappedLocalIds += matchingLocal.id
            } else {
                profilesToCreate[record.id] = record.profile.toLocalProfile(userId, localProfiles.size + profilesToCreate.size)
            }
        }

        val unchangedDefaults = defaultProfilesByName.values.map { it.toPortableProfile() }.toSet()
        localProfiles.filter { it.id !in mappedLocalIds }.forEach { local ->
            val localId = records.keys.firstOrNull { remoteToLocal[it] == local.id }
            val isUnchangedUnstarredDefault =
                local.toPortableProfile() in unchangedDefaults && local.id.toString() !in favoriteIds
            if (localId == null && !isUnchangedUnstarredDefault) {
                val cloudId = newProfileLibraryId()
                val record = ProfileLibraryRecord(
                    id = cloudId,
                    favorite = local.id.toString() in favoriteIds,
                    profile = local.toPortableProfile(),
                )
                records[cloudId] = record
                remoteToLocal[cloudId] = local.id
                mappedLocalIds += local.id
            }
        }

        val orderedRecords = records.values.sortedBy { it.id }
        return SyncPlan(
            catalog = ProfileLibraryCatalog(
                schemaVersion = maxOf(remote.schemaVersion, if (orderedRecords.any { it.profile.program.version == 2 }) 2 else 1),
                revision = remote.revision,
                profiles = orderedRecords,
            ),
            remoteToLocal = remoteToLocal,
            conflictCopies = conflictCopies,
            profilesToCreate = profilesToCreate,
            profilesToDelete = profilesToDelete,
            observedLocalProfiles = localProfiles.associate { it.id to it.toPortableProfile() },
            observedFavoriteIds = favoriteIds,
            pendingDeletions = pendingDeletions,
            previousMetadataEntries = metadata.entries.associateBy { it.cloudId },
        )
    }

    private suspend fun applyPlan(plan: SyncPlan, accepted: ProfileLibraryCatalog, userId: Long) {
        val localIds = plan.remoteToLocal.toMutableMap()
        plan.conflictCopies.forEach { (cloudId, profile) ->
            val stored = profiles.upsertSynchronizedProfile(profile.copy(id = BrewProfile.NEW_ID, userId = userId))
            localIds[cloudId] = stored.id
        }
        plan.profilesToDelete.forEach { localId ->
            val current = profiles.getBrewProfileById(localId)
            val observed = plan.observedLocalProfiles[localId]
            val favoriteIds = userSettings.favoriteProfiles(userId).first()
            val changedWhileSyncing = observed != null &&
                (current?.toPortableProfile() != observed ||
                    (localId.toString() in favoriteIds) != (localId.toString() in plan.observedFavoriteIds))
            if (!changedWhileSyncing) {
                userSettings.setFavoriteProfile(userId, localId.toString(), favorite = false)
                profiles.removeSynchronizedProfile(localId)
            }
        }
        plan.profilesToCreate.forEach { (cloudId, profile) ->
            val stored = profiles.upsertSynchronizedProfile(profile.copy(id = BrewProfile.NEW_ID, userId = userId))
            localIds[cloudId] = stored.id
        }

        var currentFavoriteIds = userSettings.favoriteProfiles(userId).first()
        val deferredLocalChanges = mutableSetOf<String>()
        accepted.profiles.forEach { record ->
            if (record.deleted) return@forEach
            val localId = localIds[record.id] ?: return@forEach
            val current = profiles.getBrewProfileById(localId)
            val observed = plan.observedLocalProfiles[localId]
            val currentFavorite = localId.toString() in currentFavoriteIds
            val changedWhileSyncing = observed != null &&
                (current?.toPortableProfile() != observed || currentFavorite != (localId.toString() in plan.observedFavoriteIds))
            if (changedWhileSyncing) {
                deferredLocalChanges += record.id
                return@forEach
            }
            val synchronized = record.profile.toLocalProfile(
                userId = userId,
                position = current?.position ?: profiles.getBrewProfilesForUser(userId).size,
                id = localId,
            )
            if (current != synchronized) profiles.upsertSynchronizedProfile(synchronized)
            userSettings.setFavoriteProfile(userId, localId.toString(), record.favorite)
            currentFavoriteIds = if (record.favorite) {
                currentFavoriteIds + localId.toString()
            } else {
                currentFavoriteIds - localId.toString()
            }
        }

        val savedEntries = accepted.profiles.map { record ->
            plan.previousMetadataEntries[record.id]
                ?.takeIf { record.id in deferredLocalChanges }
                ?: ProfileSyncEntry(
                    cloudId = record.id,
                    localProfileId = if (record.deleted) 0L else localIds[record.id] ?: 0L,
                    baselineProfile = record.profile,
                    baselineFavorite = record.favorite,
                    baselineDeleted = record.deleted,
                    remoteRevision = record.revision,
                )
        }
        dataStore.edit { preferences ->
            preferences[metadataKey(userId)] = json.encodeToString(
                ProfileSyncMetadata(catalogRevision = accepted.revision, entries = savedEntries),
            )
        }
        profiles.clearPendingSyncDeletions(userId, plan.pendingDeletions)
    }

    private suspend fun readMetadata(userId: Long): ProfileSyncMetadata {
        val stored = dataStore.data.first()[metadataKey(userId)] ?: return ProfileSyncMetadata()
        return json.decodeFromString(stored)
    }

    private fun sameRecords(
        left: List<ProfileLibraryRecord>,
        right: List<ProfileLibraryRecord>,
    ): Boolean = left.size == right.size &&
        left.associateBy { it.id } == right.associateBy { it.id }

    internal data class SyncPlan(
        val catalog: ProfileLibraryCatalog,
        val remoteToLocal: Map<String, Long>,
        val conflictCopies: Map<String, BrewProfile>,
        val profilesToCreate: Map<String, BrewProfile>,
        val profilesToDelete: Set<Long>,
        val observedLocalProfiles: Map<Long, PortableBrewProfile>,
        val observedFavoriteIds: Set<String>,
        val pendingDeletions: Set<Long>,
        val previousMetadataEntries: Map<String, ProfileSyncEntry>,
    )

    companion object {
        private const val MAX_CONFLICT_RETRIES = 3
    }

    private fun metadataKey(userId: Long) = stringPreferencesKey("profile_library_sync_metadata_$userId")
}

private fun canonicalProfileName(name: String): String = when (name.trim().lowercase()) {
    "adaptive v2", "adaptive v2 (lita, no scale)" -> "adaptive v2"
    else -> name.trim().lowercase()
}
