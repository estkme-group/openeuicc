package im.angry.openeuicc.ui

import android.content.pm.PackageManager
import android.icu.text.ListFormatter
import android.os.Build
import android.os.Bundle
import android.se.omapi.Reader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import im.angry.easyeuicc.R
import im.angry.openeuicc.util.EUICC_DEFAULT_ISDR_AID
import im.angry.openeuicc.util.UnprivilegedEuiccContextMarker
import im.angry.openeuicc.util.connectSEService
import im.angry.openeuicc.util.decodeHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

open class QuickCompatibilityFragment : Fragment(), UnprivilegedEuiccContextMarker {
    companion object {
        enum class Compatibility {
            COMPATIBLE,
            NOT_COMPATIBLE,
        }

        data class CompatibilityResult(
            val compatibility: Compatibility,
            val slotsOmapi: List<String> = emptyList(),
            val slotsIsdr: List<String> = emptyList()
        )
    }

    private val conclusion: TextView by lazy {
        requireView().requireViewById(R.id.quick_compatibility_conclusion)
    }

    private val resultSlots: TextView by lazy {
        requireView().requireViewById(R.id.quick_compatibility_result_slots)
    }

    private val resultSlotsIsdr: TextView by lazy {
        requireView().requireViewById(R.id.quick_compatibility_result_slots_isdr)
    }

    private val resultNotes: TextView by lazy {
        requireView().requireViewById(R.id.quick_compatibility_result_notes)
    }

    private val skipCheckBox: CheckBox by lazy {
        requireView().requireViewById(R.id.quick_compatibility_skip)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_quick_compatibility, container, false).apply {
        requireViewById<TextView>(R.id.quick_compatibility_device_information)
            .text = formatDeviceInformation()
        requireViewById<Button>(R.id.quick_compatibility_button_continue)
            .setOnClickListener { onContinueToApp() }
        // Can't use the lazy field yet
        requireViewById<CheckBox>(R.id.quick_compatibility_skip).setOnCheckedChangeListener { compoundButton, b ->
            if (compoundButton.isVisible) {
                runBlocking {
                    preferenceRepository.skipQuickCompatibilityFlow
                        .updatePreference(b)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            onCompatibilityUpdate(withContext(Dispatchers.IO) {
                getCompatibilityCheckResult()
            })
        }
    }

    private fun onContinueToApp() {
        requireActivity().finish()
    }

    private fun onCompatibilityUpdate(result: CompatibilityResult) {
        conclusion.text = formatConclusion(result)
        if (result.compatibility == Compatibility.COMPATIBLE) {
            // Don't show the message again, ever, if the result is compatible
            runBlocking {
                preferenceRepository.skipQuickCompatibilityFlow
                    .updatePreference(true)
            }
            resultSlots.isVisible = true
            resultSlots.text = getString(
                R.string.quick_compatibility_result_slots,
                ListFormatter.getInstance().format(result.slotsOmapi)
            )
            resultSlotsIsdr.isVisible = true
            resultSlotsIsdr.text = if (result.slotsIsdr.isEmpty()) {
                getString(R.string.quick_compatibility_unknown)
            } else {
                getString(
                    R.string.quick_compatibility_result_slots_isdr,
                    ListFormatter.getInstance().format(result.slotsIsdr)
                )
            }
            resultNotes.isVisible = true
        } else {
            resultNotes.isVisible = true
            resultNotes.text = getString(R.string.quick_compatibility_result_notes_incompatible)
            skipCheckBox.isVisible = true
        }
    }

    private suspend fun getCompatibilityCheckResult(): CompatibilityResult {
        val service = connectSEService(requireContext())
        if (!service.isConnected) {
            return CompatibilityResult(Compatibility.NOT_COMPATIBLE)
        }
        val omapiSlots = service.readers.filter { it.isSIM }.map { it.slotIndex }
        val slots = service.readers.filter { it.isSIM }.mapNotNull { reader ->
            try {
                // Note: we ONLY check the default ISD-R AID, because this test is for the _device_,
                // NOT the eUICC. We don't care what AID a potential eUICC might use, all we need to
                // check is we can open _some_ AID.
                reader.openSession().openLogicalChannel(EUICC_DEFAULT_ISDR_AID.decodeHex())?.close()
                reader.slotIndex
            } catch (_: SecurityException) {
                // Ignore; this is expected when everything works
                // ref: https://android.googlesource.com/platform/frameworks/base/+/4fe64fb4712a99d5da9c9a0eb8fd5169b252e1e1/omapi/java/android/se/omapi/Session.java#305
                // SecurityException is only thrown when Channel is constructed, which means everything else needs to succeed
                reader.slotIndex
            } catch (_: Exception) {
                null
            }
        }
        if (omapiSlots.isEmpty()) {
            return CompatibilityResult(Compatibility.NOT_COMPATIBLE)
        }
        return CompatibilityResult(Compatibility.COMPATIBLE, slotsOmapi = omapiSlots.map { "SIM$it" }, slotsIsdr = slots.map { "SIM$it" })
    }

    open fun formatConclusion(result: CompatibilityResult): String {
        val usbHost = requireContext().packageManager
            .hasSystemFeature(PackageManager.FEATURE_USB_HOST)
        val resId = when (result.compatibility) {
            Compatibility.COMPATIBLE ->
                R.string.quick_compatibility_compatible

            Compatibility.NOT_COMPATIBLE -> if (usbHost)
                R.string.quick_compatibility_not_compatible_but_usb else
                R.string.quick_compatibility_not_compatible
        }
        return getString(resId, getString(R.string.app_name))
    }

    open fun formatDeviceInformation() = buildString {
        appendLine("BRAND: ${Build.BRAND}")
        appendLine("DEVICE: ${Build.DEVICE}")
        appendLine("MODEL: ${Build.MODEL}")
        appendLine("VERSION.RELEASE: ${Build.VERSION.RELEASE}")
        appendLine("VERSION.SDK_INT: ${Build.VERSION.SDK_INT}")
    }
}

val Reader.isSIM: Boolean
    get() = name.startsWith("SIM")

val Reader.slotIndex: Int
    get() = (name.replace("SIM", "").toIntOrNull() ?: 1)