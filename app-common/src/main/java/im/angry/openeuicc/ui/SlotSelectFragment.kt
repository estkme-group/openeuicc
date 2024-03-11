package im.angry.openeuicc.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.widget.Toolbar
import im.angry.openeuicc.common.R
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.util.*

class SlotSelectFragment : BaseMaterialDialogFragment(), OpenEuiccContextMarker {
    companion object {
        const val TAG = "SlotSelectFragment"

        fun newInstance(): SlotSelectFragment {
            return SlotSelectFragment()
        }
    }

    interface SlotSelectedListener {
        fun onSlotSelected(slotId: Int, portId: Int)
        fun onSlotSelectCancelled()
    }

    private lateinit var toolbar: Toolbar
    private lateinit var spinner: Spinner
    private val channels: List<EuiccChannel> by lazy {
        euiccChannelManager.knownChannels.sortedBy { it.logicalSlotId }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_slot_select, container, false)

        toolbar = view.requireViewById(R.id.toolbar)
        toolbar.setTitle(R.string.slot_select)
        toolbar.inflateMenu(R.menu.fragment_slot_select)

        val adapter = ArrayAdapter<String>(inflater.context, R.layout.spinner_item)

        spinner = view.requireViewById(R.id.spinner)
        spinner.adapter = adapter

        channels.forEach { channel ->
            adapter.add(getString(R.string.channel_name_format, channel.logicalSlotId))
        }

        toolbar.setNavigationOnClickListener {
            (requireActivity() as SlotSelectedListener).onSlotSelectCancelled()
        }
        toolbar.setOnMenuItemClickListener {
            val channel = channels[spinner.selectedItemPosition]
            (requireActivity() as SlotSelectedListener).onSlotSelected(channel.slotId, channel.portId)
            dismiss()
            true
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        setWidthPercent(75)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        (requireActivity() as SlotSelectedListener).onSlotSelectCancelled()
    }
}