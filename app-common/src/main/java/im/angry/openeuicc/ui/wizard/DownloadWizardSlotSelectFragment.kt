package im.angry.openeuicc.ui.wizard

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import im.angry.openeuicc.common.R
import im.angry.openeuicc.util.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import net.typeblog.lpac_jni.LocalProfileInfo

class DownloadWizardSlotSelectFragment : DownloadWizardActivity.DownloadWizardStepFragment() {
    private data class SlotInfo(
        val logicalSlotId: Int,
        val eID: String,
        val enabledProfileName: String?
    )

    private var loaded = false

    private val adapter = SlotInfoAdapter()

    override val hasNext: Boolean
        get() = loaded
    override val hasPrev: Boolean
        get() = false

    override fun createNextFragment(): DownloadWizardActivity.DownloadWizardStepFragment {
        TODO("Not yet implemented")
    }

    override fun createPrevFragment(): DownloadWizardActivity.DownloadWizardStepFragment {
        TODO("Not yet implemented")
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

    @SuppressLint("NotifyDataSetChanged")
    private suspend fun init() {
        showProgressBar(-1)
        val slots = euiccChannelManager.flowEuiccPorts().map { (slotId, portId) ->
            euiccChannelManager.withEuiccChannel(slotId, portId) { channel ->
                SlotInfo(
                    channel.logicalSlotId,
                    channel.lpa.eID,
                    channel.lpa.profiles.find { it.state == LocalProfileInfo.State.Enabled }?.displayName
                )
            }
        }.toList()
        adapter.slots = slots
        adapter.notifyDataSetChanged()
        hideProgressBar()
        refreshButtons()
    }

    private class SlotItemHolder(val adapter: SlotInfoAdapter, val root: View) : ViewHolder(root) {
        private val title = root.requireViewById<TextView>(R.id.slot_item_title)
        private val eID = root.requireViewById<TextView>(R.id.slot_item_eid)
        private val activeProfile = root.requireViewById<TextView>(R.id.slot_item_active_profile)
        private val checkBox = root.requireViewById<CheckBox>(R.id.slot_checkbox)

        private var curIdx = -1

        init {
            root.setOnClickListener(this::onSelect)
            checkBox.setOnClickListener(this::onSelect)
        }

        @Suppress("UNUSED_PARAMETER")
        fun onSelect(view: View) {
            if (curIdx < 0) return
            if (adapter.currentSelectedIdx == curIdx) return
            val lastIdx = adapter.currentSelectedIdx
            adapter.currentSelectedIdx = curIdx
            adapter.notifyItemChanged(lastIdx)
            adapter.notifyItemChanged(curIdx)
        }

        fun bind(item: SlotInfo, idx: Int) {
            curIdx = idx
            title.text = root.context.getString(R.string.download_wizard_slot_title, item.logicalSlotId)
            eID.text = item.eID
            activeProfile.text = item.enabledProfileName ?: root.context.getString(R.string.unknown)
            checkBox.isChecked = adapter.currentSelectedIdx == idx
        }
    }

    private class SlotInfoAdapter : RecyclerView.Adapter<SlotItemHolder>() {
        var slots: List<SlotInfo> = listOf()
        var currentSelectedIdx = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotItemHolder {
            val root = LayoutInflater.from(parent.context).inflate(R.layout.download_slot_item, parent, false)
            return SlotItemHolder(this, root)
        }

        override fun getItemCount(): Int = slots.size

        override fun onBindViewHolder(holder: SlotItemHolder, position: Int) {
            holder.bind(slots[position], position)
        }
    }
}