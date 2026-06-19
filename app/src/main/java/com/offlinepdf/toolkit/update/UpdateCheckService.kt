package com.offlinepdf.toolkit.update

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.worldvisionsoft.pdftoolkit.BuildConfig
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(val latestVersion: String, val forceUpdate: Boolean)

@Singleton
class UpdateCheckService @Inject constructor() {

    private val remoteConfig = FirebaseRemoteConfig.getInstance()

    suspend fun fetchUpdateInfo(): UpdateInfo? {
        return try {
            val fetchInterval = if (BuildConfig.DEBUG) 0L else 3600L
            remoteConfig.setConfigSettingsAsync(
                remoteConfigSettings { minimumFetchIntervalInSeconds = fetchInterval }
            ).await()
            remoteConfig.setDefaultsAsync(
                mapOf("version_name" to "1.0.0", "force_update" to true)
            ).await()
            remoteConfig.fetchAndActivate().await()
            val version = remoteConfig.getString("version_name").takeIf { it.isNotBlank() } ?: return null
            val forceUpdate = remoteConfig.getBoolean("force_update")
            UpdateInfo(version, forceUpdate)
        } catch (e: Exception) {
            null
        }
    }

    fun isUpdateAvailable(currentVersion: String, remoteVersion: String): Boolean =
        compareVersions(currentVersion, remoteVersion) < 0

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.trim().split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.trim().split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val diff = parts1.getOrElse(i) { 0 }.compareTo(parts2.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }
}
