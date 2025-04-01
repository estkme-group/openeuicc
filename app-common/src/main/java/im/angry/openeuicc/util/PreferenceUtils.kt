package im.angry.openeuicc.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.Fragment
import im.angry.openeuicc.OpenEuiccApplication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Base64

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "prefs")

val Context.preferenceRepository: PreferenceRepository
    get() = (applicationContext as OpenEuiccApplication).appContainer.preferenceRepository

val Fragment.preferenceRepository: PreferenceRepository
    get() = requireContext().preferenceRepository

internal object PreferenceKeys {
    // ---- Profile Notifications ----
    val NOTIFICATION_DOWNLOAD = booleanPreferencesKey("notification_download")
    val NOTIFICATION_DELETE = booleanPreferencesKey("notification_delete")
    val NOTIFICATION_SWITCH = booleanPreferencesKey("notification_switch")

    // ---- Advanced ----
    val DISABLE_SAFEGUARD_REMOVABLE_ESIM = booleanPreferencesKey("disable_safeguard_removable_esim")
    val VERBOSE_LOGGING = booleanPreferencesKey("verbose_logging")

    // ---- Developer Options ----
    val DEVELOPER_OPTIONS_ENABLED = booleanPreferencesKey("developer_options_enabled")
    val REFRESH_AFTER_SWITCH = booleanPreferencesKey("refresh_after_switch")
    val UNFILTERED_PROFILE_LIST = booleanPreferencesKey("unfiltered_profile_list")
    val IGNORE_TLS_CERTIFICATE = booleanPreferencesKey("ignore_tls_certificate")
    val EUICC_MEMORY_RESET = booleanPreferencesKey("euicc_memory_reset")
    val ISDR_AID_LIST = stringPreferencesKey("isdr_aid_list")
}

const val EUICC_DEFAULT_ISDR_AID = "A0000005591010FFFFFFFF8900000100"

internal object PreferenceConstants {
    val DEFAULT_AID_LIST = """
        # One AID per line. Comment lines start with #.
        # Refs: <https://euicc-manual.osmocom.org/docs/lpa/applet-id-oem/>

        # eUICC standard
        $EUICC_DEFAULT_ISDR_AID

        # eSTK.me
        A06573746B6D65FFFFFFFF4953442D52

        # eSIM.me
        A0000005591010000000008900000300

        # 5ber.eSIM
        A0000005591010FFFFFFFF8900050500

        # Xesim
        A0000005591010FFFFFFFF8900000177
    """.trimIndent()
}

open class PreferenceRepository(private val context: Context) {
    // Expose flows so that we can also handle default values
    // ---- Profile Notifications ----
    val notificationDownloadFlow = bindFlow(PreferenceKeys.NOTIFICATION_DOWNLOAD, true)
    val notificationDeleteFlow = bindFlow(PreferenceKeys.NOTIFICATION_DELETE, true)
    val notificationSwitchFlow = bindFlow(PreferenceKeys.NOTIFICATION_SWITCH, false)

    // ---- Advanced ----
    val disableSafeguardFlow = bindFlow(PreferenceKeys.DISABLE_SAFEGUARD_REMOVABLE_ESIM, false)
    val verboseLoggingFlow = bindFlow(PreferenceKeys.VERBOSE_LOGGING, false)

    // ---- Developer Options ----
    val refreshAfterSwitchFlow = bindFlow(PreferenceKeys.REFRESH_AFTER_SWITCH, true)
    val developerOptionsEnabledFlow = bindFlow(PreferenceKeys.DEVELOPER_OPTIONS_ENABLED, false)
    val unfilteredProfileListFlow = bindFlow(PreferenceKeys.UNFILTERED_PROFILE_LIST, false)
    val ignoreTLSCertificateFlow = bindFlow(PreferenceKeys.IGNORE_TLS_CERTIFICATE, false)
    val euiccMemoryResetFlow = bindFlow(PreferenceKeys.EUICC_MEMORY_RESET, false)
    val isdrAidListFlow = bindFlow(
        PreferenceKeys.ISDR_AID_LIST,
        PreferenceConstants.DEFAULT_AID_LIST,
        { Base64.getEncoder().encodeToString(it.encodeToByteArray()) },
        { Base64.getDecoder().decode(it).decodeToString() })

    protected fun <T> bindFlow(
        key: Preferences.Key<T>,
        defaultValue: T,
        encoder: (T) -> T = { it },
        decoder: (T) -> T = { it }
    ): PreferenceFlowWrapper<T> =
        PreferenceFlowWrapper(context, key, defaultValue, encoder, decoder)
}

class PreferenceFlowWrapper<T> private constructor(
    private val context: Context,
    private val key: Preferences.Key<T>,
    inner: Flow<T>,
    private val encoder: (T) -> T,
) : Flow<T> by inner {
    internal constructor(
        context: Context,
        key: Preferences.Key<T>,
        defaultValue: T,
        encoder: (T) -> T,
        decoder: (T) -> T
    ) : this(
        context,
        key,
        context.dataStore.data.map { it[key]?.let(decoder) ?: defaultValue },
        encoder
    )

    suspend fun updatePreference(value: T) {
        context.dataStore.edit { it[key] = encoder(value) }
    }

    suspend fun removePreference() {
        context.dataStore.edit { it.remove(key) }
    }
}
