package im.angry.openeuicc.ui

import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import im.angry.openeuicc.R
import im.angry.openeuicc.util.*

class PrivilegedSettingsFragment : SettingsFragment(), PrivilegedEuiccContextMarker {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        addPreferencesFromResource(R.xml.pref_privileged_settings)
        mergePreferenceOverlay("pref_developer_overlay", "pref_developer")

        // It's stupid to _disable_ things for privileged, but for now, the per-app locale picker
        // is not usable for apps signed with the platform key.
        // ref: <https://android.googlesource.com/platform/packages/apps/Settings/+/refs/tags/android-15.0.0_r6/src/com/android/settings/applications/AppLocaleUtil.java#60>
        // This is disabled here, not moved to unprivileged, because I have hope that this will
        // eventually work for platform-signed apps. Or, at some point we might introduce our own
        // locale picker, which hopefully works whether privileged or not.
        requirePreference<Preference>("pref_advanced_language").isVisible = false

        // Force use TelephonyManager API
        requirePreference<CheckBoxPreference>("pref_developer_removable_telephony_manager")
            .bindBooleanFlow(preferenceRepository.removableTelephonyManagerFlow)
    }
}