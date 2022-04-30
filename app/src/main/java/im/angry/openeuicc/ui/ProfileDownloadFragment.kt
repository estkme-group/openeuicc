package im.angry.openeuicc.ui

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.truphone.lpa.progress.DownloadProgress
import im.angry.openeuicc.R
import im.angry.openeuicc.databinding.FragmentProfileDownloadBinding
import im.angry.openeuicc.util.setWidthPercent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

class ProfileDownloadFragment : DialogFragment(), EuiccFragmentMarker, Toolbar.OnMenuItemClickListener {
    companion object {
        const val TAG = "ProfileDownloadFragment"

        fun newInstance(slotId: Int): ProfileDownloadFragment =
            newInstanceEuicc(ProfileDownloadFragment::class.java, slotId)
    }

    private var _binding: FragmentProfileDownloadBinding? = null
    private val binding get() = _binding!!

    private var downloading = false

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
                if (!downloading) dismiss()
            }
            setOnMenuItemClickListener(this@ProfileDownloadFragment)
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean = downloading ||
        when (item.itemId) {
            R.id.scan -> {
                barcodeScannerLauncher.launch(ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    setOrientationLocked(false)
                })
                true
            }
            R.id.ok -> {
                startDownloadProfile()
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

    private fun startDownloadProfile() {
        val server = binding.profileDownloadServer.editText!!.let {
            it.text.toString().trim().apply {
                if (isEmpty()) {
                    it.requestFocus()
                    return@startDownloadProfile
                }
            }
        }

        val code = binding.profileDownloadCode.editText!!.let {
            it.text.toString().trim().apply {
                if (isEmpty()) {
                    it.requestFocus()
                    return@startDownloadProfile
                }
            }
        }

        downloading = true

        binding.profileDownloadServer.editText!!.isEnabled = false
        binding.profileDownloadCode.editText!!.isEnabled = false

        binding.progress.isIndeterminate = true
        binding.progress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                doDownloadProfile(server, code)
            } catch (e: Exception) {
                Log.d(TAG, "Error downloading profile")
                Log.d(TAG, Log.getStackTraceString(e))
                Toast.makeText(context, R.string.profile_download_failed, Toast.LENGTH_LONG).show()
            } finally {
                if (parentFragment is EuiccProfilesChangedListener) {
                    (parentFragment as EuiccProfilesChangedListener).onEuiccProfilesChanged()
                }
                dismiss()
            }
        }
    }

    private suspend fun doDownloadProfile(server: String, code: String) = withContext(Dispatchers.IO) {
        channel.lpa.downloadProfile("1\$${server}\$${code}", DownloadProgress().apply {
            setProgressListener { _, _, percentage, _ ->
                binding.progress.isIndeterminate = false
                binding.progress.progress = (percentage * 100).toInt()
            }
        })
    }
}