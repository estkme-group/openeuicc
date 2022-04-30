package im.angry.openeuicc.ui

import android.app.Dialog
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import im.angry.openeuicc.R
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.databinding.FragmentProfileDownloadBinding
import im.angry.openeuicc.util.setWidthPercent

class ProfileDownloadFragment(val channel: EuiccChannel) : DialogFragment(), Toolbar.OnMenuItemClickListener {
    companion object {
        const val TAG = "ProfileDownloadFragment"
    }

    private var _binding: FragmentProfileDownloadBinding? = null
    private val binding get() = _binding!!

    private val barcodeScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { content ->
            val components = content.split("$")
            if (components.size != 3 || components[0] != "LPA:1") return@registerForActivityResult
            binding.profileDownloadServer.editText?.setText(components[1])
            binding.profileDownloadCode.editText?.setText(components[2])
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileDownloadBinding.inflate(inflater, container, false)
        binding.toolbar.inflateMenu(R.menu.fragment_profile_download)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.apply {
            setTitle(R.string.profile_download)
            setNavigationOnClickListener {
                dismiss()
            }
            setOnMenuItemClickListener(this@ProfileDownloadFragment)
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.scan -> {
                barcodeScannerLauncher.launch(ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    setOrientationLocked(false)
                })
                true
            }
            else -> false
        }

    override fun onResume() {
        super.onResume()
        setWidthPercent(95)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).also {
            it.window?.requestFeature(Window.FEATURE_NO_TITLE)
            it.setCanceledOnTouchOutside(false)
        }
    }
}