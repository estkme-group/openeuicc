package im.angry.openeuicc.ui.wizard

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import im.angry.openeuicc.common.R
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.util.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch

class DownloadWizardSlotSelectFragment : DownloadWizardActivity.DownloadWizardStepFragment() {
    companion object {
        const val LOW_NVRAM_THRESHOLD =
            30 * 1024 // < 30 KiB, alert about potential download failure
    }

    private data class SlotInfo(
        val logicalSlotId: Int,
        val isRemovable: Boolean,
        val hasMultiplePorts: Boolean,
        val portId: Int,
        val eID: String,
        val freeSpace: Int,
        val imei: String,
        val enabledProfileName: String?,
        val intrinsicChannelName: String?,
    )

    private var loaded = false

    private val adapter = SlotInfoAdapter()

    override val hasNext: Boolean
        get() = loaded && adapter.slots.isNotEmpty()
    override val hasPrev: Boolean
        get() = true

    override fun createNextFragment(): DownloadWizardActivity.DownloadWizardStepFragment =
        if (state.skipMethodSelect) {
            DownloadWizardDetailsFragment()
        } else {
            DownloadWizardMethodSelectFragment()
        }

    override fun createPrevFragment(): DownloadWizardActivity.DownloadWizardStepFragment? = null

    override fun beforeNext() {
        super.beforeNext()

        if (adapter.selected.freeSpace < LOW_NVRAM_THRESHOLD) {
            val activity = requireActivity()

            AlertDialog.Builder(requireContext()).apply {
                setTitle(R.string.profile_download_low_nvram_title)
                setMessage(R.string.profile_download_low_nvram_message)
                setCancelable(true)
                setPositiveButton(android.R.string.ok, null)
                setNegativeButton(android.R.string.cancel) { _, _ ->
                    activity.finish()
                }
                show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_download_slot_select, container, false)
        val recyclerView = view.requireViewById<RecyclerView>(R.id.download_slot_list)
        recyclerView.adapter = adapter
        recyclerView.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        recyclerView.addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        return view
    }

    override fun onStart() {
        super.onStart()
        if (!loaded) {
            lifecycleScope.launch { init() }
        }
    }

    @SuppressLint("NotifyDataSetChanged", "MissingPermission")
    private suspend fun init() {
        ensureEuiccChannelManager()
        showProgressBar(-1)
        val slots = euiccChannelManager.flowAllOpenEuiccPorts().map { (slotId, portId) ->
            euiccChannelManager.withEuiccChannel(slotId, portId) { channel ->
                SlotInfo(
                    channel.logicalSlotId,
                    channel.port.card.isRemovable,
                    channel.port.card.ports.size > 1,
                    channel.portId,
                    channel.lpa.eID,
                    channel.lpa.euiccInfo2?.freeNvram ?: 0,
                    try {
                        telephonyManager.getImei(channel.logicalSlotId) ?: ""
                    } catch (e: Exception) {
                        ""
                    },
                    channel.lpa.profiles.enabled?.displayName,
                    channel.intrinsicChannelName,
                )
            }
        }.toList().sortedBy { it.logicalSlotId }
        adapter.slots = slots

        // Ensure we always have a selected slot by default
        val selectedIdx = slots.indexOfFirst { it.logicalSlotId == state.selectedLogicalSlot }
        adapter.currentSelectedIdx = if (selectedIdx > 0) {
            selectedIdx
        } else {
            if (slots.isNotEmpty()) {
                state.selectedLogicalSlot = slots[0].logicalSlotId
            }
            0
        }

        if (slots.isNotEmpty()) {
            state.imei = slots[adapter.currentSelectedIdx].imei
        }

        adapter.notifyDataSetChanged()
        hideProgressBar()
        loaded = true
        refreshButtons()
    }

    private inner class SlotItemHolder(val root: View) : ViewHolder(root) {
        private val title = root.requireViewById<TextView>(R.id.slot_item_title)
        private val type = root.requireViewById<TextView>(R.id.slot_item_type)
        private val eID = root.requireViewById<TextView>(R.id.slot_item_eid)
        private val activeProfile = root.requireViewById<TextView>(R.id.slot_item_active_profile)
        private val freeSpace = root.requireViewById<TextView>(R.id.slot_item_free_space)
        private val checkBox = root.requireViewById<CheckBox>(R.id.slot_checkbox)

        private var curIdx = -1

        init {
            root.setOnClickListener(this::onSelect)
            checkBox.setOnClickListener(this::onSelect)
        }

        @Suppress("UNUSED_PARAMETER")
        fun onSelect(view: View) {
            if (curIdx < 0) return
            checkBox.isChecked = true
            if (adapter.currentSelectedIdx == curIdx) return
            val lastIdx = adapter.currentSelectedIdx
            adapter.currentSelectedIdx = curIdx
            adapter.notifyItemChanged(lastIdx)
            adapter.notifyItemChanged(curIdx)
            // Selected index isn't logical slot ID directly, needs a conversion
            state.selectedLogicalSlot = adapter.slots[adapter.currentSelectedIdx].logicalSlotId
            state.imei = adapter.slots[adapter.currentSelectedIdx].imei
        }

        fun bind(item: SlotInfo, idx: Int) {
            curIdx = idx

            type.text = if (item.isRemovable) {
                root.context.getString(R.string.download_wizard_slot_type_removable)
            } else if (!item.hasMultiplePorts) {
                root.context.getString(R.string.download_wizard_slot_type_internal)
            } else {
                root.context.getString(
                    R.string.download_wizard_slot_type_internal_port,
                    item.portId
                )
            }

            title.text = if (item.logicalSlotId == EuiccChannelManager.USB_CHANNEL_ID) {
                item.intrinsicChannelName ?: root.context.getString(R.string.channel_type_usb)
            } else {
                appContainer.customizableTextProvider.formatInternalChannelName(item.logicalSlotId)
            }
            eID.text = item.eID
            activeProfile.text = item.enabledProfileName ?: root.context.getString(R.string.profile_no_enabled_profile)
            freeSpace.text = formatFreeSpace(item.freeSpace)
            checkBox.isChecked = adapter.currentSelectedIdx == idx
        }
    }

    private inner class SlotInfoAdapter : RecyclerView.Adapter<SlotItemHolder>() {
        var slots: List<SlotInfo> = listOf()
        var currentSelectedIdx = -1

        val selected: SlotInfo
            get() = slots[currentSelectedIdx]

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotItemHolder {
            val root = LayoutInflater.from(parent.context).inflate(R.layout.download_slot_item, parent, false)
            return SlotItemHolder(root)
        }

        override fun getItemCount(): Int = slots.size

        override fun onBindViewHolder(holder: SlotItemHolder, position: Int) {
            holder.bind(slots[position], position)
        }
    }
}