package im.angry.openeuicc.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import net.typeblog.lpac_jni.LocalProfileInfo
import im.angry.openeuicc.common.R
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class EuiccManagementFragment : Fragment(), EuiccProfilesChangedListener,
    EuiccChannelFragmentMarker {
    companion object {
        const val TAG = "EuiccManagementFragment"

        fun newInstance(slotId: Int, portId: Int): EuiccManagementFragment =
            newInstanceEuicc(EuiccManagementFragment::class.java, slotId, portId)
    }

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var fab: FloatingActionButton
    private lateinit var profileList: RecyclerView

    private val adapter = EuiccProfileAdapter()

    // Marker for when this fragment might enter an invalid state
    // e.g. after a failed enable / disable operation
    private var invalid = false

    // Subscribe to settings we care about outside of coroutine contexts while initializing
    // This gives us access to the "latest" state without having to launch coroutines
    private lateinit var disableSafeguardFlow: StateFlow<Boolean>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_euicc, container, false)

        swipeRefresh = view.requireViewById(R.id.swipe_refresh)
        fab = view.requireViewById(R.id.fab)
        profileList = view.requireViewById(R.id.profile_list)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefresh.setOnRefreshListener { refresh() }
        profileList.adapter = adapter
        profileList.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        fab.setOnClickListener {
            ProfileDownloadFragment.newInstance(slotId, portId)
                .show(childFragmentManager, ProfileDownloadFragment.TAG)
        }

        refresh()
    }

    override fun onEuiccProfilesChanged() {
        refresh()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_euicc, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.show_notifications -> {
                Intent(requireContext(), NotificationsActivity::class.java).apply {
                    putExtra("logicalSlotId", channel.logicalSlotId)
                    startActivity(this)
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }

    protected open suspend fun onCreateFooterViews(
        parent: ViewGroup,
        profiles: List<LocalProfileInfo>
    ): List<View> =
        if (profiles.isEmpty()) {
            val view = layoutInflater.inflate(R.layout.footer_no_profile, parent, false)
            listOf(view)
        } else {
            listOf()
        }

    @SuppressLint("NotifyDataSetChanged")
    private fun refresh() {
        if (invalid) return
        swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            if (!this@EuiccManagementFragment::disableSafeguardFlow.isInitialized) {
                disableSafeguardFlow =
                    preferenceRepository.disableSafeguardFlow.stateIn(lifecycleScope)
            }

            val profiles = withContext(Dispatchers.IO) {
                euiccChannelManager.notifyEuiccProfilesChanged(channel.logicalSlotId)
                channel.lpa.profiles.operational
            }

            withContext(Dispatchers.Main) {
                adapter.profiles = profiles
                adapter.footerViews = onCreateFooterViews(profileList, profiles)
                adapter.notifyDataSetChanged()
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun enableOrDisableProfile(iccid: String, enable: Boolean) {
        swipeRefresh.isRefreshing = true
        fab.isEnabled = false

        lifecycleScope.launch {
            beginTrackedOperation {
                val (res, refreshed) =
                    if (!channel.lpa.switchProfile(iccid, enable, refresh = true)) {
                        // Sometimes, we *can* enable or disable the profile, but we cannot
                        // send the refresh command to the modem because the profile somehow
                        // makes the modem "busy". In this case, we can still switch by setting
                        // refresh to false, but then the switch cannot take effect until the
                        // user resets the modem manually by toggling airplane mode or rebooting.
                        Pair(channel.lpa.switchProfile(iccid, enable, refresh = false), false)
                    } else {
                        Pair(true, true)
                    }

                if (!res) {
                    Log.d(TAG, "Failed to enable / disable profile $iccid")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            R.string.toast_profile_enable_failed,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@beginTrackedOperation false
                }

                if (!refreshed && !isUsb) {
                    withContext(Dispatchers.Main) {
                        AlertDialog.Builder(requireContext()).apply {
                            setMessage(R.string.switch_did_not_refresh)
                            setPositiveButton(android.R.string.ok) { dialog, _ ->
                                dialog.dismiss()
                                requireActivity().finish()
                            }
                            setOnDismissListener { _ ->
                                requireActivity().finish()
                            }
                            show()
                        }
                    }
                    return@beginTrackedOperation true
                }

                if (!isUsb) {
                    try {
                        euiccChannelManager.waitForReconnect(
                            slotId,
                            portId,
                            timeoutMillis = 30 * 1000
                        )
                    } catch (e: TimeoutCancellationException) {
                        withContext(Dispatchers.Main) {
                            // Prevent this Fragment from being used again
                            invalid = true
                            // Timed out waiting for SIM to come back online, we can no longer assume that the LPA is still valid
                            AlertDialog.Builder(requireContext()).apply {
                                setMessage(R.string.enable_disable_timeout)
                                setPositiveButton(android.R.string.ok) { dialog, _ ->
                                    dialog.dismiss()
                                    requireActivity().finish()
                                }
                                setOnDismissListener { _ ->
                                    requireActivity().finish()
                                }
                                show()
                            }
                        }
                        return@beginTrackedOperation false
                    }
                }

                preferenceRepository.notificationSwitchFlow.first()
            }
            refresh()
            fab.isEnabled = true
        }
    }

    protected open fun populatePopupWithProfileActions(popup: PopupMenu, profile: LocalProfileInfo) {
        popup.inflate(R.menu.profile_options)
        if (profile.isEnabled) {
            popup.menu.findItem(R.id.enable).isVisible = false
            popup.menu.findItem(R.id.delete).isVisible = false

            // We hide the disable option by default to avoid "bricking" some cards that won't get
            // recognized again by the phone's modem. However we don't have that worry if we are
            // accessing it through a USB card reader, or when the user explicitly opted in
            if (isUsb || disableSafeguardFlow.value) {
                popup.menu.findItem(R.id.disable).isVisible = true
            }
        }
    }

    sealed class ViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        enum class Type(val value: Int) {
            PROFILE(0),
            FOOTER(1);

            companion object {
                fun fromInt(value: Int) =
                    Type.values().first { it.value == value }
            }
        }
    }

    inner class FooterViewHolder: ViewHolder(FrameLayout(requireContext())) {
        init {
            itemView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        fun attach(view: View) {
            view.parent?.let { (it as ViewGroup).removeView(view) }
            (itemView as FrameLayout).addView(view)
        }

        fun detach() {
            (itemView as FrameLayout).removeAllViews()
        }
    }

    inner class ProfileViewHolder(private val root: View) : ViewHolder(root) {
        private val iccid: TextView = root.requireViewById(R.id.iccid)
        private val name: TextView = root.requireViewById(R.id.name)
        private val state: TextView = root.requireViewById(R.id.state)
        private val provider: TextView = root.requireViewById(R.id.provider)
        private val profileMenu: ImageButton = root.requireViewById(R.id.profile_menu)

        init {
            iccid.setOnClickListener {
                if (iccid.transformationMethod == null) {
                    iccid.transformationMethod = PasswordTransformationMethod.getInstance()
                } else {
                    iccid.transformationMethod = null
                }
            }

            iccid.setOnLongClickListener {
                requireContext().getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText("iccid", iccid.text))
                Toast.makeText(requireContext(), R.string.toast_iccid_copied, Toast.LENGTH_SHORT)
                    .show()
                true
            }

            profileMenu.setOnClickListener { showOptionsMenu() }
        }

        private lateinit var profile: LocalProfileInfo

        fun setProfile(profile: LocalProfileInfo) {
            this.profile = profile
            name.text = profile.displayName

            state.setText(
                if (profile.isEnabled) {
                    R.string.enabled
                } else {
                    R.string.disabled
                }
            )
            provider.text = profile.providerName
            iccid.text = profile.iccid
            iccid.transformationMethod = PasswordTransformationMethod.getInstance()
        }

        private fun showOptionsMenu() {
            // Prevent users from doing multiple things at once
            if (invalid || swipeRefresh.isRefreshing) return

            PopupMenu(root.context, profileMenu).apply {
                setOnMenuItemClickListener(::onMenuItemClicked)
                populatePopupWithProfileActions(this, profile)
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
                    ProfileRenameFragment.newInstance(slotId, portId, profile.iccid, profile.displayName)
                        .show(childFragmentManager, ProfileRenameFragment.TAG)
                    true
                }
                R.id.delete -> {
                    ProfileDeleteFragment.newInstance(slotId, portId, profile.iccid, profile.displayName)
                        .show(childFragmentManager, ProfileDeleteFragment.TAG)
                    true
                }
                else -> false
            }
    }

    inner class EuiccProfileAdapter : RecyclerView.Adapter<ViewHolder>() {
        var profiles: List<LocalProfileInfo> = listOf()
        var footerViews: List<View> = listOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            when (ViewHolder.Type.fromInt(viewType)) {
                ViewHolder.Type.PROFILE -> {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.euicc_profile, parent, false)
                    ProfileViewHolder(view)
                }
                ViewHolder.Type.FOOTER -> {
                    FooterViewHolder()
                }
            }

        override fun getItemViewType(position: Int): Int =
            when {
                position < profiles.size -> {
                    ViewHolder.Type.PROFILE.value
                }
                position >= profiles.size && position < profiles.size + footerViews.size -> {
                    ViewHolder.Type.FOOTER.value
                }
                else -> -1
            }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            when (holder) {
                is ProfileViewHolder -> {
                    holder.setProfile(profiles[position])
                }
                is FooterViewHolder -> {
                    holder.attach(footerViews[position - profiles.size])
                }
            }
        }

        override fun onViewRecycled(holder: ViewHolder) {
            if (holder is FooterViewHolder) {
                holder.detach()
            }
        }

        override fun getItemCount(): Int = profiles.size + footerViews.size
    }
}