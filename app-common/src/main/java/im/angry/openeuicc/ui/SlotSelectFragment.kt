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

        fun newInstance(knownChannels: List<EuiccChannel>): SlotSelectFragment {
            return SlotSelectFragment().apply {
                arguments = Bundle().apply {
                    putIntArray("slotIds", knownChannels.map { it.slotId }.toIntArray())
                    putIntArray("logicalSlotIds", knownChannels.map { it.logicalSlotId }.toIntArray())
                    putIntArray("portIds", knownChannels.map { it.portId }.toIntArray())
                }
            }
        }
    }

    interface SlotSelectedListener {
        fun onSlotSelected(slotId: Int, portId: Int)
        fun onSlotSelectCancelled()
    }

    private lateinit var toolbar: Toolbar
    private lateinit var spinner: Spinner
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var slotIds: IntArray
    private lateinit var logicalSlotIds: IntArray
    private lateinit var portIds: IntArray

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_slot_select, container, false)

        toolbar = view.requireViewById(R.id.toolbar)
        toolbar.setTitle(R.string.slot_select)
        toolbar.inflateMenu(R.menu.fragment_slot_select)

        adapter = ArrayAdapter<String>(inflater.context, R.layout.spinner_item)

        spinner = view.requireViewById(R.id.spinner)
        spinner.adapter = adapter

        return view
    }

    override fun onStart() {
        super.onStart()

        slotIds = requireArguments().getIntArray("slotIds")!!
        logicalSlotIds = requireArguments().getIntArray("logicalSlotIds")!!
        portIds = requireArguments().getIntArray("portIds")!!

        logicalSlotIds.forEach { id ->
            adapter.add(getString(R.string.channel_name_format, id))
        }

        toolbar.setNavigationOnClickListener {
            (requireActivity() as SlotSelectedListener).onSlotSelectCancelled()
        }
        toolbar.setOnMenuItemClickListener {
            val slotId = slotIds[spinner.selectedItemPosition]
            val portId = portIds[spinner.selectedItemPosition]
            (requireActivity() as SlotSelectedListener).onSlotSelected(slotId, portId)
            dismiss()
            true
        }
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