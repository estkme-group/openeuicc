package im.angry.openeuicc.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey

internal object UnprivilegedPreferenceKeys {
    // ---- Miscellaneous ----
    val SKIP_QUICK_COMPATIBILITY = booleanPreferencesKey("skip_quick_compatibility")
}

class UnprivilegedPreferenceRepository(context: Context) : PreferenceRepository(context) {
    // ---- Miscellaneous ----
    val skipQuickCompatibilityFlow = bindFlow(UnprivilegedPreferenceKeys.SKIP_QUICK_COMPATIBILITY, false)
}