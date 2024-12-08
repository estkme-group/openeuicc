package im.angry.openeuicc.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import im.angry.easyeuicc.R
import im.angry.openeuicc.util.encodeHex
import java.security.MessageDigest

class UnprivilegedSettingsFragment : SettingsFragment() {
    private val firstSigner by lazy {
        val packageInfo = requireContext().let {
            it.packageManager.getPackageInfo(
                it.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            )
        }
        packageInfo.signingInfo!!.apkContentsSigners.first().let {
            MessageDigest.getInstance("SHA-1")
                .apply { update(it.toByteArray()) }
                .digest()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        addPreferencesFromResource(R.xml.pref_unprivileged_settings)
        findPreference<Preference>("pref_developer_ara_m")?.apply {
            bindCategory(this, "pref_developer")
            isVisible = true
            summary = firstSigner.encodeHex()
            setOnPreferenceClickListener {
                requireContext().getSystemService(ClipboardManager::class.java)!!
                    .setPrimaryClip(ClipData.newPlainText("ara-m", summary))
                Toast.makeText(requireContext(), R.string.toast_ara_m_copied, Toast.LENGTH_SHORT)
                    .show()
                true
            }
        }
    }

    private fun bindCategory(preference: Preference, key: String) {
        preference.parent!!.removePreference(preference)
        findPreference<PreferenceCategory>(key)!!.addPreference(preference)
    }
}