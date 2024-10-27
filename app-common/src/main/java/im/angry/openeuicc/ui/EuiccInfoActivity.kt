package im.angry.openeuicc.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import im.angry.openeuicc.common.R
import im.angry.openeuicc.util.*
import kotlinx.coroutines.launch
import net.typeblog.lpac_jni.impl.DEFAULT_PKID_GSMA_RSP2_ROOT_CI1
import net.typeblog.lpac_jni.impl.PKID_GSMA_TEST_CI

class EuiccInfoActivity : BaseEuiccAccessActivity() {
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var infoList: RecyclerView

    private val euiccInfoItems: MutableList<Pair<String, String>> = mutableListOf()

    private var logicalSlotId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_euicc_info)
        setSupportActionBar(requireViewById(R.id.toolbar))
        setupToolbarInsets()
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        swipeRefresh = requireViewById(R.id.swipe_refresh)
        infoList = requireViewById(R.id.recycler_view)

        infoList.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        infoList.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
        infoList.adapter = EuiccInfoAdapter()

        logicalSlotId = intent.getIntExtra("logicalSlotId", 0)

        title = getString(
            R.string.euicc_info_activity_title,
            getString(R.string.channel_name_format, logicalSlotId)
        )

        swipeRefresh.setOnRefreshListener { refresh() }

        setupRootViewInsets(infoList)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onInit() {
        refresh()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refresh() {
        swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            euiccInfoItems.clear()

            val unknownStr = getString(R.string.unknown)

            euiccInfoItems.add(
                Pair(
                    getString(R.string.euicc_info_access_mode),
                    euiccChannelManager.withEuiccChannel(logicalSlotId) { channel -> channel.type }
                )
            )

            euiccInfoItems.add(
                Pair(
                    getString(R.string.euicc_info_removable),
                    if (euiccChannelManager.withEuiccChannel(logicalSlotId) { channel -> channel.port.card.isRemovable }) {
                        getString(R.string.yes)
                    } else {
                        getString(R.string.no)
                    }
                )
            )

            euiccInfoItems.add(
                Pair(
                    getString(R.string.euicc_info_eid),
                    euiccChannelManager.withEuiccChannel(logicalSlotId) { channel -> channel.lpa.eID }
                )
            )

            val euiccInfo2 = euiccChannelManager.withEuiccChannel(logicalSlotId) { channel ->
                channel.lpa.euiccInfo2
            }

            euiccInfoItems.add(
                Pair(
                    getString(R.string.euicc_info_firmware_version),
                    euiccInfo2?.euiccFirmwareVersion ?: unknownStr
                )
            )

            euiccInfoItems.add(
                Pair(
                    getString(R.string.euicc_info_globalplatform_version),
                    euiccInfo2?.globalPlatformVersion ?: unknownStr
                )
            )

            euiccInfoItems.add(
                Pair(
                    getString(R.string.euicc_info_pp_version),
                    euiccInfo2?.ppVersion ?: unknownStr
                )
            )

            euiccInfoItems.add(
                Pair(
                    getString(R.string.euicc_info_sas_accreditation_number),
                    euiccInfo2?.sasAccreditationNumber ?: unknownStr
                )
            )

            euiccInfoItems.add(Pair(
                getString(R.string.euicc_info_free_nvram),
                euiccInfo2?.freeNvram?.let { formatFreeSpace(it) } ?: unknownStr
            ))

            euiccInfoItems.add(
                Pair(
                    getString(R.string.euicc_info_gsma_prod),
                    if (euiccInfo2?.euiccCiPKIdListForSigning?.contains(
                            DEFAULT_PKID_GSMA_RSP2_ROOT_CI1
                        ) == true
                    ) {
                        getString(R.string.supported)
                    } else {
                        getString(R.string.unsupported)
                    }
                )
            )

            euiccInfoItems.add(
                Pair(
                    getString(R.string.euicc_info_gsma_test),
                    if (PKID_GSMA_TEST_CI.any { euiccInfo2?.euiccCiPKIdListForSigning?.contains(it) == true }) {
                        getString(R.string.supported)
                    } else {
                        getString(R.string.unsupported)
                    }
                )
            )

            infoList.adapter!!.notifyDataSetChanged()

            swipeRefresh.isRefreshing = false
        }
    }

    inner class EuiccInfoViewHolder(root: View) : ViewHolder(root) {
        private val title: TextView = root.requireViewById(R.id.euicc_info_title)
        private val content: TextView = root.requireViewById(R.id.euicc_info_content)

        fun bind(item: Pair<String, String>) {
            title.text = item.first
            content.text = item.second
        }
    }

    inner class EuiccInfoAdapter : RecyclerView.Adapter<EuiccInfoViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EuiccInfoViewHolder {
            val root = LayoutInflater.from(parent.context)
                .inflate(R.layout.euicc_info_item, parent, false)
            return EuiccInfoViewHolder(root)
        }

        override fun getItemCount(): Int = euiccInfoItems.size

        override fun onBindViewHolder(holder: EuiccInfoViewHolder, position: Int) {
            holder.bind(euiccInfoItems[position])
        }
    }
}