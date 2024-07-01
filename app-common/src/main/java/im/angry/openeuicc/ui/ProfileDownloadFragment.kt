package im.angry.openeuicc.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import im.angry.openeuicc.common.R
import im.angry.openeuicc.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.typeblog.lpac_jni.ProfileDownloadCallback
import kotlin.Exception

class ProfileDownloadFragment : BaseMaterialDialogFragment(),
    Toolbar.OnMenuItemClickListener, EuiccChannelFragmentMarker {
    companion object {
        const val TAG = "ProfileDownloadFragment"

        fun newInstance(slotId: Int, portId: Int, finishWhenDone: Boolean = false): ProfileDownloadFragment =
            newInstanceEuicc(ProfileDownloadFragment::class.java, slotId, portId) {
                putBoolean("finishWhenDone", finishWhenDone)
            }
    }

    private lateinit var toolbar: Toolbar
    private lateinit var profileDownloadServer: TextInputLayout
    private lateinit var profileDownloadCode: TextInputLayout
    private lateinit var profileDownloadConfirmationCode: TextInputLayout
    private lateinit var profileDownloadIMEI: TextInputLayout
    private lateinit var profileDownloadFreeSpace: TextView
    private lateinit var progress: ProgressBar

    private var freeNvram: Int = -1

    private var downloading = false

    private val finishWhenDone by lazy {
        requireArguments().getBoolean("finishWhenDone", false)
    }

    private val barcodeScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { content ->
            Log.d(TAG, content)
            onScanResult(content)
        }
    }

    private val gallerySelectorLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { result ->
        if (result == null) return@registerForActivityResult

        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                requireContext().contentResolver.openInputStream(result)?.let { input ->
                    val bmp = BitmapFactory.decodeStream(input)
                    input.close()

                    decodeQrFromBitmap(bmp)?.let {
                        withContext(Dispatchers.Main) {
                            onScanResult(it)
                        }
                    }

                    bmp.recycle()
                }
            }
        }
    }

    private fun onScanResult(result: String) {
        val components = result.split("$")
        if (components.size < 3 || components[0] != "LPA:1") return
        profileDownloadServer.editText?.setText(components[1])
        profileDownloadCode.editText?.setText(components[2])
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile_download, container, false)

        toolbar = view.requireViewById(R.id.toolbar)
        profileDownloadServer = view.requireViewById(R.id.profile_download_server)
        profileDownloadCode = view.requireViewById(R.id.profile_download_code)
        profileDownloadConfirmationCode = view.requireViewById(R.id.profile_download_confirmation_code)
        profileDownloadIMEI = view.requireViewById(R.id.profile_download_imei)
        profileDownloadFreeSpace = view.requireViewById(R.id.profile_download_free_space)
        progress = view.requireViewById(R.id.progress)

        toolbar.inflateMenu(R.menu.fragment_profile_download)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.apply {
            setTitle(R.string.profile_download)
            setNavigationOnClickListener {
                if (!downloading) {
                    dismiss()
                }
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
            R.id.scan_from_gallery -> {
                gallerySelectorLauncher.launch("image/*")
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

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()
        profileDownloadIMEI.editText!!.text = Editable.Factory.getInstance().newEditable(
            try {
                telephonyManager.getImei(channel.logicalSlotId) ?: ""
            } catch (e: Exception) {
                ""
            }
        )

        lifecycleScope.launch(Dispatchers.IO) {
            // Fetch remaining NVRAM
            val str = channel.lpa.euiccInfo2?.freeNvram?.also {
                freeNvram = it
            }?.let { formatFreeSpace(it) }

            withContext(Dispatchers.Main) {
                profileDownloadFreeSpace.text = getString(R.string.profile_download_free_space,
                    str ?: getText(R.string.unknown))
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).also {
            it.setCanceledOnTouchOutside(false)
        }
    }

    private fun startDownloadProfile() {
        val server = profileDownloadServer.editText!!.let {
            it.text.toString().trim().apply {
                if (isEmpty()) {
                    it.requestFocus()
                    return@startDownloadProfile
                }
            }
        }

        val code = profileDownloadCode.editText!!.text.toString().trim()
            .ifBlank { null }
        val confirmationCode = profileDownloadConfirmationCode.editText!!.text.toString().trim()
            .ifBlank { null }
        val imei = profileDownloadIMEI.editText!!.text.toString().trim()
            .ifBlank { null }

        downloading = true

        profileDownloadServer.editText!!.isEnabled = false
        profileDownloadCode.editText!!.isEnabled = false
        profileDownloadConfirmationCode.editText!!.isEnabled = false
        profileDownloadIMEI.editText!!.isEnabled = false

        progress.isIndeterminate = true
        progress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                doDownloadProfile(server, code, confirmationCode, imei)
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

    private suspend fun doDownloadProfile(
        server: String,
        code: String?,
        confirmationCode: String?,
        imei: String?
    ) = beginTrackedOperation {
        val res = channel.lpa.downloadProfile(
            server,
            code,
            imei,
            confirmationCode,
            object : ProfileDownloadCallback {
                override fun onStateUpdate(state: ProfileDownloadCallback.DownloadState) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        progress.isIndeterminate = false
                        progress.progress = state.progress
                    }
                }
            })

        if (!res) {
            // TODO: Provide more details on the error
            throw RuntimeException("Failed to download profile; this is typically caused by another error happened before.")
        }

        // If we get here, we are successful
        // This function is wrapped in beginTrackedOperation, so by returning the settings value,
        // We only send notifications if the user allowed us to
        preferenceRepository.notificationDownloadFlow.first()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (finishWhenDone) {
            activity?.finish()
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        if (finishWhenDone) {
            activity?.finish()
        }
    }
}