package im.angry.openeuicc.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.truphone.lpa.LocalProfileInfo
import com.truphone.lpad.progress.Progress
import im.angry.openeuicc.R
import im.angry.openeuicc.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

class EuiccManagementFragment : Fragment(), EuiccFragmentMarker, EuiccProfilesChangedListener {
    companion object {
        const val TAG = "EuiccManagementFragment"

        fun newInstance(slotId: Int): EuiccManagementFragment =
            newInstanceEuicc(EuiccManagementFragment::class.java, slotId)
    }

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var fab: FloatingActionButton
    private lateinit var profileList: RecyclerView

    private val adapter = EuiccProfileAdapter(listOf())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_euicc, container, false)

        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        fab = view.findViewById(R.id.fab)
        profileList = view.findViewById(R.id.profile_list)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefresh.setOnRefreshListener { refresh() }
        profileList.adapter = adapter
        profileList.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        fab.setOnClickListener {
            ProfileDownloadFragment.newInstance(slotId)
                .show(childFragmentManager, ProfileDownloadFragment.TAG)
        }
    }

    override fun onStart() {
        super.onStart()
        refresh()
    }

    override fun onEuiccProfilesChanged() {
        refresh()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refresh() {
        swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            val profiles = withContext(Dispatchers.IO) {
                openEuiccApplication.subscriptionManager.tryRefreshCachedEuiccInfo(channel.cardId)
                channel.lpa.profiles
            }

            withContext(Dispatchers.Main) {
                adapter.profiles = profiles.operational
                adapter.notifyDataSetChanged()
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun enableOrDisableProfile(iccid: String, enable: Boolean) {
        swipeRefresh.isRefreshing = true
        swipeRefresh.isEnabled = false
        fab.isEnabled = false

        lifecycleScope.launch {
            try {
                if (enable) {
                    doEnableProfile(iccid)
                } else {
                    doDisableProfile(iccid)
                }
                Toast.makeText(context, R.string.toast_profile_enabled, Toast.LENGTH_LONG).show()
                // The APDU channel will be invalid when the SIM reboots. For now, just exit the app
                euiccChannelManager.invalidate()
                requireActivity().finish()
            } catch (e: Exception) {
                Log.d(TAG, "Failed to enable / disable profile $iccid")
                Log.d(TAG, Log.getStackTraceString(e))
                fab.isEnabled = true
                swipeRefresh.isEnabled = true
                Toast.makeText(context, R.string.toast_profile_enable_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun doEnableProfile(iccid: String) =
        withContext(Dispatchers.IO) {
            channel.lpa.enableProfile(iccid, Progress())
        }

    private suspend fun doDisableProfile(iccid: String) =
        withContext(Dispatchers.IO) {
            channel.lpa.disableProfile(iccid, Progress())
        }

    inner class ViewHolder(private val root: View) : RecyclerView.ViewHolder(root) {
        private val iccid: TextView = root.findViewById(R.id.iccid)
        private val name: TextView = root.findViewById(R.id.name)
        private val state: TextView = root.findViewById(R.id.state)
        private val provider: TextView = root.findViewById(R.id.provider)
        private val profileMenu: ImageButton = root.findViewById(R.id.profile_menu)

        init {
            iccid.setOnClickListener {
                if (iccid.transformationMethod == null) {
                    iccid.transformationMethod = PasswordTransformationMethod.getInstance()
                } else {
                    iccid.transformationMethod = null
                }
            }

            profileMenu.setOnClickListener { showOptionsMenu() }
        }

        private lateinit var profile: LocalProfileInfo

        fun setProfile(profile: LocalProfileInfo) {
            this.profile = profile
            name.text = profile.displayName

            state.setText(
                if (isEnabled()) {
                    R.string.enabled
                } else {
                    R.string.disabled
                }
            )
            provider.text = profile.providerName
            iccid.text = profile.iccid
            iccid.transformationMethod = PasswordTransformationMethod.getInstance()
        }

        private fun isEnabled(): Boolean =
            profile.state == LocalProfileInfo.State.Enabled

        private fun showOptionsMenu() {
            PopupMenu(root.context, profileMenu).apply {
                setOnMenuItemClickListener(::onMenuItemClicked)
                inflate(R.menu.profile_options)
                if (isEnabled()) {
                    menu.findItem(R.id.enable).isVisible = false
                    menu.findItem(R.id.delete).isVisible = false
                } else {
                    menu.findItem(R.id.disable).isVisible = false
                }
                show()
            }
        }

        private fun onMenuItemClicked(item: MenuItem): Boolean =
            when (item.itemId) {
                R.id.enable -> {
                    enableOrDisableProfile(profile.iccid, true)
                    true
                }
                R.id.disable -> {
                    enableOrDisableProfile(profile.iccid, false)
                    true
                }
                R.id.rename -> {
                    ProfileRenameFragment.newInstance(slotId, profile.iccid, profile.displayName)
                        .show(childFragmentManager, ProfileRenameFragment.TAG)
                    true
                }
                R.id.delete -> {
                    ProfileDeleteFragment.newInstance(slotId, profile.iccid, profile.displayName)
                        .show(childFragmentManager, ProfileDeleteFragment.TAG)
                    true
                }
                else -> false
            }
    }

    inner class EuiccProfileAdapter(var profiles: List<LocalProfileInfo>) : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.euicc_profile, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.setProfile(profiles[position])
        }

        override fun getItemCount(): Int = profiles.size
    }
}