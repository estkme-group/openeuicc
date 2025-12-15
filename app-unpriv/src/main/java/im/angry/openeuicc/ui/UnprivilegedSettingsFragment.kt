package im.angry.openeuicc.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import im.angry.easyeuicc.R
import im.angry.openeuicc.util.*
import java.security.MessageDigest

class UnprivilegedSettingsFragment : SettingsFragment() {
    private val firstSigner by lazy {
        val packageInfo = with(requireContext()) {
            packageManager.getPackageInfo(packageName, /* flags = */ GET_SIGNING_CERTIFICATES)
        }
        packageInfo.signingInfo!!.apkContentsSigners.first().toByteArray()
            .let(MessageDigest.getInstance("SHA-1")::digest)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        addPreferencesFromResource(R.xml.pref_unprivileged_settings)
        mergePreferenceOverlay("pref_info_overlay", "pref_info")

        requirePreference<Preference>("pref_info_ara_m").apply {
            summary = firstSigner.encodeHex()
            setOnPreferenceClickListener {
                requireContext().getSystemService(ClipboardManager::class.java)!!
                    .setPrimaryClip(ClipData.newPlainText("ara-m", summary))
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) Toast
                    .makeText(requireContext(), R.string.toast_ara_m_copied, Toast.LENGTH_SHORT)
                    .show()
                true
            }
        }
    }
}
