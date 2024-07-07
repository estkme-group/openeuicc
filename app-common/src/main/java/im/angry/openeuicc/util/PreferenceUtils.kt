package im.angry.openeuicc.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.Fragment
import im.angry.openeuicc.OpenEuiccApplication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "prefs")

val Context.preferenceRepository: PreferenceRepository
    get() = (applicationContext as OpenEuiccApplication).appContainer.preferenceRepository

val Fragment.preferenceRepository: PreferenceRepository
    get() = requireContext().preferenceRepository

object PreferenceKeys {
    val NOTIFICATION_DOWNLOAD = booleanPreferencesKey("notification_download")
    val NOTIFICATION_DELETE = booleanPreferencesKey("notification_delete")
    val NOTIFICATION_SWITCH = booleanPreferencesKey("notification_switch")
    val DISABLE_SAFEGUARD_REMOVABLE_ESIM = booleanPreferencesKey("disable_safeguard_removable_esim")
}

class PreferenceRepository(context: Context) {
    private val dataStore = context.dataStore

    // Expose flows so that we can also handle default values
    // ---- Profile Notifications ----
    val notificationDownloadFlow: Flow<Boolean> =
        dataStore.data.map { it[PreferenceKeys.NOTIFICATION_DOWNLOAD] ?: true }

    val notificationDeleteFlow: Flow<Boolean> =
        dataStore.data.map { it[PreferenceKeys.NOTIFICATION_DELETE] ?: true }

    val notificationSwitchFlow: Flow<Boolean> =
        dataStore.data.map { it[PreferenceKeys.NOTIFICATION_SWITCH] ?: false }

    // ---- Advanced ----
    val disableSafeguardFlow: Flow<Boolean> =
        dataStore.data.map { it[PreferenceKeys.DISABLE_SAFEGUARD_REMOVABLE_ESIM] ?: false }

    suspend fun <T> updatePreference(key: Preferences.Key<T>, value: T) {
        dataStore.edit {
            it[key] = value
        }
    }
}