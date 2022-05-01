package im.angry.openeuicc.ui

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import im.angry.openeuicc.R
import im.angry.openeuicc.databinding.FragmentProfileRenameBinding
import im.angry.openeuicc.util.setWidthPercent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.lang.RuntimeException

class ProfileRenameFragment : DialogFragment(), EuiccFragmentMarker {
    companion object {
        const val TAG = "ProfileRenameFragment"

        fun newInstance(slotId: Int, iccid: String, currentName: String): ProfileRenameFragment {
            val instance = newInstanceEuicc(ProfileRenameFragment::class.java, slotId)
            instance.requireArguments().apply {
                putString("iccid", iccid)
                putString("currentName", currentName)
            }
            return instance
        }
    }

    private var _binding: FragmentProfileRenameBinding? = null
    private val binding get() = _binding!!

    private var renaming = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileRenameBinding.inflate(inflater, container, false)
        binding.toolbar.inflateMenu(R.menu.fragment_profile_rename)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.apply {
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
        binding.profileRenameNewName.editText!!.setText(requireArguments().getString("currentName"))
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

    private fun rename() {
        val name = binding.profileRenameNewName.editText!!.text.toString().trim()
        if (name.length >= 64) {
            Toast.makeText(context, R.string.toast_profile_name_too_long, Toast.LENGTH_LONG).show()
            return
        }

        renaming = true
        binding.progress.isIndeterminate = true
        binding.progress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                doRename(name)
            } catch (e: Exception) {
                Log.d(TAG, "Failed to rename profile")
                Log.d(TAG, Log.getStackTraceString(e))
            } finally {
                if (parentFragment is EuiccProfilesChangedListener) {
                    (parentFragment as EuiccProfilesChangedListener).onEuiccProfilesChanged()
                }
                dismiss()
            }
        }
    }

    private suspend fun doRename(name: String) = withContext(Dispatchers.IO) {
        if (!channel.lpa.setNickname(requireArguments().getString("iccid"), name)) {
            throw RuntimeException("Profile nickname not changed")
        }
    }
}