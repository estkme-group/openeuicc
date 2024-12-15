package im.angry.openeuicc.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import im.angry.openeuicc.common.R
import im.angry.openeuicc.service.EuiccChannelManagerService.Companion.waitDone
import im.angry.openeuicc.util.*
import kotlinx.coroutines.launch
import net.typeblog.lpac_jni.LocalProfileAssistant

class ProfileRenameFragment : BaseMaterialDialogFragment(), EuiccChannelFragmentMarker {
    companion object {
        const val TAG = "ProfileRenameFragment"

        fun newInstance(slotId: Int, portId: Int, iccid: String, currentName: String): ProfileRenameFragment {
            val instance = newInstanceEuicc(ProfileRenameFragment::class.java, slotId, portId)
            instance.requireArguments().apply {
                putString("iccid", iccid)
                putString("currentName", currentName)
            }
            return instance
        }
    }

    private lateinit var toolbar: Toolbar
    private lateinit var profileRenameNewName: TextInputLayout
    private lateinit var progress: ProgressBar

    private var renaming = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile_rename, container, false)

        toolbar = view.requireViewById(R.id.toolbar)
        profileRenameNewName = view.requireViewById(R.id.profile_rename_new_name)
        progress = view.requireViewById(R.id.progress)

        toolbar.inflateMenu(R.menu.fragment_profile_rename)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.apply {
            setTitle(R.string.rename)
            setNavigationOnClickListener {
                if (!renaming) dismiss()
            }
            setOnMenuItemClickListener {
                if (!renaming) rename()
                true
            }
        }
    }

    override fun onStart() {
        super.onStart()
        profileRenameNewName.editText!!.setText(requireArguments().getString("currentName"))
    }

    override fun onResume() {
        super.onResume()
        setWidthPercent(95)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).also {
            it.setCanceledOnTouchOutside(false)
        }
    }

    private fun rename() {
        val name = profileRenameNewName.editText!!.text.toString().trim()
        if (name.length >= 64) {
            Toast.makeText(context, R.string.toast_profile_name_too_long, Toast.LENGTH_LONG).show()
            return
        }

        renaming = true
        progress.isIndeterminate = true
        progress.visibility = View.VISIBLE

        lifecycleScope.launch {
            ensureEuiccChannelManager()
            euiccChannelManagerService.waitForForegroundTask()
            val res = euiccChannelManagerService.launchProfileRenameTask(
                slotId,
                portId,
                requireArguments().getString("iccid")!!,
                name
            ).waitDone()

            when (res) {
                is LocalProfileAssistant.ProfileNameTooLongException -> {
                    Toast.makeText(
                        requireContext(),
                        R.string.profile_rename_too_long,
                        Toast.LENGTH_LONG
                    ).show()
                }

                is LocalProfileAssistant.ProfileNameIsInvalidUTF8Exception -> {
                    Toast.makeText(
                        requireContext(),
                        R.string.profile_rename_encoding_error,
                        Toast.LENGTH_LONG
                    ).show()
                }

                is Throwable -> {
                    Toast.makeText(
                        requireContext(),
                        R.string.profile_rename_failure,
                        Toast.LENGTH_LONG
                    ).show()
                }

                else -> {
                    if (parentFragment is EuiccProfilesChangedListener) {
                        (parentFragment as EuiccProfilesChangedListener).onEuiccProfilesChanged()
                    }

                    try {
                        dismiss()
                    } catch (e: IllegalStateException) {
                        // Ignored
                    }
                }
            }
        }
    }
}