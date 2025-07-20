package im.angry.openeuicc.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey

internal object UnprivilegedPreferenceKeys {
    // ---- Miscellaneous ----
    val SKIP_QUICK_AVAILABILITY = booleanPreferencesKey("skip_quick_availability")
}

class UnprivilegedPreferenceRepository(context: Context) : PreferenceRepository(context) {
    // ---- Miscellaneous ----
    val skipQuickAvailabilityFlow = bindFlow(UnprivilegedPreferenceKeys.SKIP_QUICK_AVAILABILITY, false)
}