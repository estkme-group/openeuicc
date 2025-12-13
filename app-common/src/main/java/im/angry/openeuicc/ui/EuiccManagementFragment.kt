package im.angry.openeuicc.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
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
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import im.angry.openeuicc.common.R
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.service.EuiccChannelManagerService
import im.angry.openeuicc.service.EuiccChannelManagerService.Companion.waitDone
import im.angry.openeuicc.ui.wizard.DownloadWizardActivity
import im.angry.openeuicc.util.EuiccChannelFragmentMarker
import im.angry.openeuicc.util.EuiccProfilesChangedListener
import im.angry.openeuicc.util.displayName
import im.angry.openeuicc.util.enabled
import im.angry.openeuicc.util.ensureEuiccChannelManager
import im.angry.openeuicc.util.euiccChannelManager
import im.angry.openeuicc.util.euiccChannelManagerService
import im.angry.openeuicc.util.isEnabled
import im.angry.openeuicc.util.isUsb
import im.angry.openeuicc.util.mainViewPaddingInsetHandler
import im.angry.openeuicc.util.newInstanceEuicc
import im.angry.openeuicc.util.operational
import im.angry.openeuicc.util.portId
import im.angry.openeuicc.util.seId
import im.angry.openeuicc.util.setupRootViewSystemBarInsets
import im.angry.openeuicc.util.slotId
import im.angry.openeuicc.util.withEuiccChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.typeblog.lpac_jni.LocalProfileInfo

open class EuiccManagementFragment : Fragment(), EuiccProfilesChangedListener,
    EuiccChannelFragmentMarker {
    companion object {
        const val TAG = "EuiccManagementFragment"

        fun newInstance(
            slotId: Int,
            portId: Int,
            seId: EuiccChannel.SecureElementId
        ): EuiccManagementFragment =
            newInstanceEuicc(EuiccManagementFragment::class.java, slotId, portId, seId)
    }

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var fab: FloatingActionButton
    private lateinit var profileList: RecyclerView
    private var logicalSlotId: Int = -1
    private lateinit var eid: String
    private var enabledProfile: LocalProfileInfo? = null

    private val adapter = EuiccProfileAdapter()

    // Marker for when this fragment might enter an invalid state
    // e.g. after a failed enable / disable operation
    private var invalid = false

    // Subscribe to settings we care about outside of coroutine contexts while initializing
    // This gives us access to the "latest" state without having to launch coroutines
    private lateinit var disableSafeguardFlow: StateFlow<Boolean>

    private lateinit var unfilteredProfileListFlow: StateFlow<Boolean>

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

        val origFabMarginRight = (fab.layoutParams as ViewGroup.MarginLayoutParams).rightMargin
        val origFabMarginBottom = (fab.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin

        setupRootViewSystemBarInsets(
            view, arrayOf(
            mainViewPaddingInsetHandler(profileList),
            { insets ->
                fab.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    rightMargin = origFabMarginRight + insets.right
                    bottomMargin = origFabMarginBottom + insets.bottom
                }
            }
        ))

        profileList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(view: RecyclerView, newState: Int) =
                if (newState == RecyclerView.SCROLL_STATE_IDLE) fab.show() else fab.hide()
        })

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefresh.setOnRefreshListener { refresh() }
        profileList.adapter = adapter
        profileList.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)

        fab.setOnClickListener {
            val intent = DownloadWizardActivity.newIntent(requireContext(), slotId, seId)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        refresh()
    }

    override fun onEuiccProfilesChanged() {
        refresh()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_euicc, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.show_notifications).isVisible =
            logicalSlotId != -1
        menu.findItem(R.id.euicc_info).isVisible =
            logicalSlotId != -1
        menu.findItem(R.id.euicc_memory_reset).isVisible =
            enabledProfile == null
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.show_notifications -> {
            Intent(requireContext(), NotificationsActivity::class.java).apply {
                putExtra("logicalSlotId", logicalSlotId)
                putExtra("seId", seId)
                startActivity(this)
            }
            true
        }

        R.id.euicc_info -> {
            Intent(requireContext(), EuiccInfoActivity::class.java).apply {
                putExtra("logicalSlotId", logicalSlotId)
                putExtra("seId", seId)
                startActivity(this)
            }
            true
        }

        R.id.euicc_memory_reset -> {
            EuiccMemoryResetFragment.newInstance(slotId, portId, seId, eid)
                .show(childFragmentManager, EuiccMemoryResetFragment.TAG)
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

    private fun refresh() {
        if (invalid) return
        swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            doRefresh()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    protected open suspend fun doRefresh() {
        ensureEuiccChannelManager()
        euiccChannelManagerService.waitForForegroundTask()

        if (!::disableSafeguardFlow.isInitialized) {
            disableSafeguardFlow =
                preferenceRepository.disableSafeguardFlow.stateIn(lifecycleScope)
        }
        if (!::unfilteredProfileListFlow.isInitialized) {
            unfilteredProfileListFlow =
                preferenceRepository.unfilteredProfileListFlow.stateIn(lifecycleScope)
        }

        val profiles = withEuiccChannel { channel ->
            logicalSlotId = channel.logicalSlotId
            eid = channel.lpa.eID
            enabledProfile = channel.lpa.profiles.enabled
            euiccChannelManager.notifyEuiccProfilesChanged(channel.logicalSlotId)
            if (unfilteredProfileListFlow.value)
                channel.lpa.profiles
            else
                channel.lpa.profiles.operational
        }

        withContext(Dispatchers.Main) {
            adapter.profiles = profiles
            adapter.footerViews = onCreateFooterViews(profileList, profiles)
            adapter.notifyDataSetChanged()
            swipeRefresh.isRefreshing = false
        }
    }

    private suspend fun showSwitchFailureText() = withContext(Dispatchers.Main) {
        val resId = R.string.toast_profile_enable_failed
        Toast.makeText(context, resId, Toast.LENGTH_LONG).show()
    }

    private fun enableOrDisableProfile(iccid: String, enable: Boolean) {
        swipeRefresh.isRefreshing = true
        fab.isEnabled = false

        lifecycleScope.launch {
            ensureEuiccChannelManager()
            euiccChannelManagerService.waitForForegroundTask()

            val err = euiccChannelManagerService
                .launchProfileSwitchTask(
                    slotId, portId, seId, iccid, enable,
                    reconnectTimeoutMillis = 30 * 1000
                )
                .waitDone()

            when (err) {
                null -> {}
                is EuiccChannelManagerService.SwitchingProfilesRefreshException -> {
                    // This is only really fatal for internal eSIMs
                    if (!isUsb) {
                        withContext(Dispatchers.Main) {
                            AlertDialog.Builder(requireContext()).apply {
                                setMessage(R.string.profile_switch_did_not_refresh)
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
                    }
                }

                is TimeoutCancellationException -> {
                    withContext(Dispatchers.Main) {
                        // Prevent this Fragment from being used again
                        invalid = true
                        // Timed out waiting for SIM to come back online, we can no longer assume that the LPA is still valid
                        AlertDialog.Builder(requireContext()).apply {
                            setMessage(appContainer.customizableTextProvider.profileSwitchingTimeoutMessage)
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
                }

                else -> showSwitchFailureText()
            }

            refresh()
            fab.isEnabled = true
        }
    }

    protected open fun populatePopupWithProfileActions(
        popup: PopupMenu,
        profile: LocalProfileInfo
    ) {
        popup.inflate(R.menu.profile_options)
        if (!profile.isEnabled) return
        popup.menu.findItem(R.id.enable).isVisible = false
        popup.menu.findItem(R.id.delete).isVisible = false

        // We hide the disable option by default to avoid "bricking" some cards that won't get
        // recognized again by the phone's modem. However, we don't have that worry if we are
        // accessing it through a USB card reader, or when the user explicitly opted in
        if (!isUsb && !disableSafeguardFlow.value) return
        popup.menu.findItem(R.id.disable).isVisible = true
    }

    sealed class ViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        enum class Type(val value: Int) {
            PROFILE(0),
            FOOTER(1);

            companion object {
                fun fromInt(value: Int) =
                    entries.first { it.value == value }
            }
        }
    }

    inner class FooterViewHolder : ViewHolder(FrameLayout(requireContext())) {
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
        private val profileClassLabel: TextView = root.requireViewById(R.id.profile_class_label)
        private val profileClass: TextView = root.requireViewById(R.id.profile_class)
        private val profileMenu: ImageButton = root.requireViewById(R.id.profile_menu)
        private val profileSeqNumber: TextView = root.requireViewById(R.id.profile_sequence_number)

        init {
            iccid.setOnClickListener {
                if (iccid.transformationMethod == null) {
                    iccid.transformationMethod = PasswordTransformationMethod.getInstance()
                } else {
                    iccid.transformationMethod = null
                }
            }

            iccid.setOnLongClickListener {
                requireContext().getSystemService(ClipboardManager::class.java)!!
                    .setPrimaryClip(ClipData.newPlainText("iccid", iccid.text))
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) Toast
                    .makeText(requireContext(), R.string.toast_iccid_copied, Toast.LENGTH_SHORT)
                    .show()
                true
            }

            profileMenu.setOnClickListener {
                showOptionsMenu()
            }
        }

        private lateinit var profile: LocalProfileInfo
        private var canEnable: Boolean = false

        fun setProfile(profile: LocalProfileInfo) {
            this.profile = profile
            name.text = profile.displayName

            state.setText(
                if (profile.isEnabled) {
                    R.string.profile_state_enabled
                } else {
                    R.string.profile_state_disabled
                }
            )
            provider.text = profile.providerName
            profileClassLabel.isVisible = unfilteredProfileListFlow.value
            profileClass.isVisible = unfilteredProfileListFlow.value
            profileClass.setText(
                when (profile.profileClass) {
                    LocalProfileInfo.Clazz.Testing -> R.string.profile_class_testing
                    LocalProfileInfo.Clazz.Provisioning -> R.string.profile_class_provisioning
                    LocalProfileInfo.Clazz.Operational -> R.string.profile_class_operational
                }
            )
            iccid.text = profile.iccid
            iccid.transformationMethod = PasswordTransformationMethod.getInstance()
        }

        fun setProfileSequenceNumber(index: Int) {
            profileSeqNumber.text = root.context.getString(
                R.string.profile_sequence_number_format,
                index,
            )
        }

        fun setEnabledProfile(enabledProfile: LocalProfileInfo?) {
            // cannot cross profile class enable profile
            // e.g: testing -> operational or operational -> testing
            canEnable = enabledProfile == null ||
                enabledProfile.profileClass == profile.profileClass
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
                    if (canEnable) {
                        enableOrDisableProfile(profile.iccid, true)
                    } else {
                        val resId = R.string.toast_profile_enable_cross_class
                        Toast.makeText(requireContext(), resId, Toast.LENGTH_LONG)
                            .show()
                    }
                    true
                }

                R.id.disable -> {
                    enableOrDisableProfile(profile.iccid, false)
                    true
                }

                R.id.rename -> {
                    ProfileRenameFragment.newInstance(
                        slotId,
                        portId,
                        seId,
                        profile.iccid,
                        profile.displayName
                    )
                        .show(childFragmentManager, ProfileRenameFragment.TAG)
                    true
                }

                R.id.delete -> {
                    ProfileDeleteFragment.newInstance(
                        slotId,
                        portId,
                        seId,
                        profile.iccid,
                        profile.displayName
                    )
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
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.euicc_profile, parent, false)
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
                    holder.setEnabledProfile(profiles.enabled)
                    holder.setProfileSequenceNumber(position + 1)
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
