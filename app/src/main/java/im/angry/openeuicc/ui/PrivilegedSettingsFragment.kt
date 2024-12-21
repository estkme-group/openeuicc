package im.angry.openeuicc.ui

import android.os.Bundle
import androidx.preference.Preference

class PrivilegedSettingsFragment : SettingsFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        // It's stupid to _disable_ things for privileged, but for now, the per-app locale picker
        // is not usable for apps signed with the platform key.
        // ref: <https://android.googlesource.com/platform/packages/apps/Settings/+/refs/tags/android-15.0.0_r6/src/com/android/settings/applications/AppLocaleUtil.java#60>
        // This is disabled here, not moved to unprivileged, because I have hope that this will
        // eventually work for platform-signed apps. Or, at some point we might introduce our own
        // locale picker, which hopefully works whether privileged or not.
        requirePreference<Preference>("pref_advanced_language").isVisible = false
    }
}