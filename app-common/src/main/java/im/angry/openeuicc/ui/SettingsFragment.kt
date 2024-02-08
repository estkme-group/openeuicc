package im.angry.openeuicc.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import im.angry.openeuicc.common.R
import im.angry.openeuicc.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SettingsFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_settings, rootKey)

        findPreference<Preference>("pref_info_app_version")
            ?.summary = requireContext().selfAppVersion

        findPreference<Preference>("pref_info_source_code")
            ?.setOnPreferenceClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.summary.toString())))
                true
            }

        findPreference<Preference>("pref_advanced_logs")
            ?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), LogsActivity::class.java))
                true
            }

        findPreference<CheckBoxPreference>("pref_notifications_download")
            ?.bindBooleanFlow(preferenceRepository.notificationDownloadFlow, PreferenceKeys.NOTIFICATION_DOWNLOAD)

        findPreference<CheckBoxPreference>("pref_notifications_delete")
            ?.bindBooleanFlow(preferenceRepository.notificationDeleteFlow, PreferenceKeys.NOTIFICATION_DELETE)

        findPreference<CheckBoxPreference>("pref_notifications_enable")
            ?.bindBooleanFlow(preferenceRepository.notificationEnableFlow, PreferenceKeys.NOTIFICATION_ENABLE)

        findPreference<CheckBoxPreference>("pref_notifications_disable")
            ?.bindBooleanFlow(preferenceRepository.notificationDisableFlow, PreferenceKeys.NOTIFICATION_DISABLE)
    }

    private fun CheckBoxPreference.bindBooleanFlow(flow: Flow<Boolean>, key: Preferences.Key<Boolean>) {
        lifecycleScope.launch {
            flow.collect { isChecked = it }
        }

        setOnPreferenceChangeListener { _, newValue ->
            runBlocking {
                preferenceRepository.updatePreference(key, newValue as Boolean)
            }
            true
        }
    }
}