package im.angry.openeuicc.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import im.angry.openeuicc.common.R
import im.angry.openeuicc.util.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

open class SettingsFragment: PreferenceFragmentCompat() {
    private lateinit var developerPref: PreferenceCategory

    // Hidden developer options switch
    private var numClicks = 0
    private var lastClickTimestamp = -1L
    private var lastToast: Toast? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_settings, rootKey)

        developerPref = requirePreference("pref_developer")

        // Show / hide developer preference based on whether it is enabled
        lifecycleScope.launch {
            preferenceRepository.developerOptionsEnabledFlow
                .onEach { developerPref.isVisible = it }
                .collect()
        }

        requirePreference<Preference>("pref_info_app_version").apply {
            summary = requireContext().selfAppVersion

            // Enable developer options when this is clicked for 7 times
            setOnPreferenceClickListener(::onAppVersionClicked)
        }

        requirePreference<Preference>("pref_advanced_language").apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@apply
            isVisible = true
            intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
            }
        }

        requirePreference<Preference>("pref_advanced_logs").apply {
            intent = Intent(requireContext(), LogsActivity::class.java)
        }

        requirePreference<CheckBoxPreference>("pref_notifications_download")
            .bindBooleanFlow(preferenceRepository.notificationDownloadFlow)

        requirePreference<CheckBoxPreference>("pref_notifications_delete")
            .bindBooleanFlow(preferenceRepository.notificationDeleteFlow)

        requirePreference<CheckBoxPreference>("pref_notifications_switch")
            .bindBooleanFlow(preferenceRepository.notificationSwitchFlow)

        requirePreference<CheckBoxPreference>("pref_advanced_disable_safeguard_removable_esim")
            .bindBooleanFlow(preferenceRepository.disableSafeguardFlow)

        requirePreference<CheckBoxPreference>("pref_advanced_verbose_logging")
            .bindBooleanFlow(preferenceRepository.verboseLoggingFlow)

        requirePreference<CheckBoxPreference>("pref_developer_unfiltered_profile_list")
            .bindBooleanFlow(preferenceRepository.unfilteredProfileListFlow)

        requirePreference<CheckBoxPreference>("pref_developer_ignore_tls_certificate")
            .bindBooleanFlow(preferenceRepository.ignoreTLSCertificateFlow)
    }

    protected fun <T : Preference> requirePreference(key: CharSequence) =
        findPreference<T>(key)!!

    override fun onStart() {
        super.onStart()
        setupRootViewInsets(requireView().requireViewById(R.id.recycler_view))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onAppVersionClicked(pref: Preference): Boolean {
        if (developerPref.isVisible) return false
        val now = System.currentTimeMillis()
        if (now - lastClickTimestamp >= 1000) {
            numClicks = 1
        } else {
            numClicks++
        }
        lastClickTimestamp = now

        if (numClicks == 7) {
            lifecycleScope.launch {
                preferenceRepository.developerOptionsEnabledFlow.updatePreference(true)

                lastToast?.cancel()
                Toast.makeText(
                    requireContext(),
                    R.string.developer_options_enabled,
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (numClicks > 1) {
            lastToast?.cancel()
            lastToast = Toast.makeText(
                requireContext(),
                getString(R.string.developer_options_steps, 7 - numClicks),
                Toast.LENGTH_SHORT
            )
            lastToast!!.show()
        }

        return true
    }

    protected fun CheckBoxPreference.bindBooleanFlow(flow: PreferenceFlowWrapper<Boolean>) {
        lifecycleScope.launch {
            flow.collect { isChecked = it }
        }

        setOnPreferenceChangeListener { _, newValue ->
            runBlocking {
                flow.updatePreference(newValue as Boolean)
            }
            true
        }
    }

    protected fun mergePreferenceOverlay(overlayKey: String, targetKey: String) {
        val overlayCat = requirePreference<PreferenceCategory>(overlayKey)
        val targetCat = requirePreference<PreferenceCategory>(targetKey)

        val prefs = buildList {
            for (i in 0..<overlayCat.preferenceCount) {
                add(overlayCat.getPreference(i))
            }
        }

        prefs.forEach {
            overlayCat.removePreference(it)
            targetCat.addPreference(it)
        }

        overlayCat.parent?.removePreference(overlayCat)
    }
}