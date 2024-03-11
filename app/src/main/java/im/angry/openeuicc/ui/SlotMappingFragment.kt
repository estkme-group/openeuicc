package im.angry.openeuicc.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.telephony.TelephonyManager
import android.telephony.UiccSlotMapping
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import im.angry.openeuicc.OpenEuiccApplication
import im.angry.openeuicc.R
import im.angry.openeuicc.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SlotMappingFragment: BaseMaterialDialogFragment(),
    OnMenuItemClickListener, OpenEuiccContextMarker {
    companion object {
        const val TAG = "SlotMappingFragment"
    }

    private val ports: List<UiccPortInfoCompat> by lazy {
        telephonyManager.uiccCardsInfoCompat.flatMap { it.ports }
    }

    private val portsDesc: List<String> by lazy {
        ports.map { getString(R.string.slot_mapping_port, it.card.physicalSlotIndex, it.portIndex) }
    }

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SlotMappingAdapter
    private lateinit var helpTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_slot_mapping, container, false)
        toolbar = view.requireViewById(R.id.toolbar)
        toolbar.inflateMenu(R.menu.fragment_slot_mapping)
        recyclerView = view.requireViewById(R.id.mapping_list)
        recyclerView.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        helpTextView = view.requireViewById(R.id.mapping_help)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.title = getString(R.string.slot_mapping)
        toolbar.setNavigationOnClickListener { dismiss() }
        toolbar.setOnMenuItemClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        setWidthPercent(85)
        init()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun init() {
        lifecycleScope.launch(Dispatchers.Main) {
            val mapping = withContext(Dispatchers.IO) {
                telephonyManager.simSlotMapping
            }

            adapter = SlotMappingAdapter(mapping.toMutableList().apply {
                sortBy { it.logicalSlotIndex }
            })
            recyclerView.adapter = adapter
            adapter.notifyDataSetChanged()

            helpTextView.text = buildHelpText()
        }
    }

    private fun commit() {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                withContext(Dispatchers.IO) {
                    // Use the utility method from PrivilegedTelephonyUtils to ensure
                    // unmapped ports have all profiles disabled
                    telephonyManager.updateSimSlotMapping(euiccChannelManager, adapter.mappings)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.slot_mapping_failure, Toast.LENGTH_LONG).show()
                return@launch
            }
            Toast.makeText(requireContext(), R.string.slot_mapping_completed, Toast.LENGTH_LONG).show()
            euiccChannelManager.invalidate()
            requireActivity().finish()
        }
    }

    private suspend fun buildHelpText() = withContext(Dispatchers.IO) {
        val nLogicalSlots = adapter.mappings.size

        val cards = telephonyManager.uiccCardsInfoCompat

        val nPhysicalSlots = cards.size
        var idxMepCard = -1
        var nMepPorts = 0

        for (card in cards) {
            if (card.isMultipleEnabledProfilesSupported) {
                idxMepCard = card.physicalSlotIndex
                nMepPorts = card.ports.size
            }
        }

        val mayEnableDSDS =
            telephonyManager.supportsDSDS && !telephonyManager.dsdsEnabled
        val extraText =
            if (nLogicalSlots == 1 && mayEnableDSDS) {
                getString(R.string.slot_mapping_help_dsds)
            } else if (idxMepCard != -1) {
                getString(R.string.slot_mapping_help_mep, idxMepCard, nMepPorts)
            } else {
                ""
            }

        getString(R.string.slot_mapping_help, nLogicalSlots, nPhysicalSlots, extraText)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean =
        when (item!!.itemId) {
            R.id.ok -> {
                commit()
                true
            }
            else -> false
        }

    inner class ViewHolder(root: View): RecyclerView.ViewHolder(root), OnItemSelectedListener {
        private val textViewLogicalSlot: TextView = root.requireViewById(R.id.slot_mapping_logical_slot)
        private val spinnerPorts: Spinner = root.requireViewById(R.id.slot_mapping_ports)

        init {
            spinnerPorts.adapter = ArrayAdapter(requireContext(), im.angry.openeuicc.common.R.layout.spinner_item, portsDesc)
            spinnerPorts.onItemSelectedListener = this
        }

        private lateinit var mappings: MutableList<UiccSlotMapping>
        private var mappingId: Int = -1

        fun attachView(mappings: MutableList<UiccSlotMapping>, mappingId: Int) {
            this.mappings = mappings
            this.mappingId = mappingId

            textViewLogicalSlot.text = getString(R.string.slot_mapping_logical_slot, mappings[mappingId].logicalSlotIndex)
            spinnerPorts.setSelection(ports.indexOfFirst {
                it.card.physicalSlotIndex == mappings[mappingId].physicalSlotIndex
                        && it.portIndex == mappings[mappingId].portIndex
            })
        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            check(this::mappings.isInitialized) { "mapping not assigned" }
            mappings[mappingId] =
                UiccSlotMapping(
                    ports[position].portIndex, ports[position].card.physicalSlotIndex, mappings[mappingId].logicalSlotIndex)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {

        }
    }

    inner class SlotMappingAdapter(val mappings: MutableList<UiccSlotMapping>): RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_slot_mapping_item, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = mappings.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.attachView(mappings, position)
        }
    }
}