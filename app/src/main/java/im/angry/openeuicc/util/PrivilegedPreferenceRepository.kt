package im.angry.openeuicc.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey

internal object PrivilegedPreferenceKeys {
    // ---- Developer Options ----
    val REMOVABLE_TELEPHONY_MANAGER = booleanPreferencesKey("removable_telephony_manager")
}

class PrivilegedPreferenceRepository(context: Context) : PreferenceRepository(context) {
    // ---- Developer Options ----
    val removableTelephonyManagerFlow =
        bindFlow(PrivilegedPreferenceKeys.REMOVABLE_TELEPHONY_MANAGER, false)
}