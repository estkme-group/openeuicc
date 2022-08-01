package im.angry.openeuicc.ui

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import im.angry.openeuicc.R
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

        toolbar = view.findViewById(R.id.toolbar)
        profileRenameNewName = view.findViewById(R.id.profile_rename_new_name)
        progress = view.findViewById(R.id.progress)

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
            it.window?.requestFeature(Window.FEATURE_NO_TITLE)
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