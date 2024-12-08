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
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.util.*
import kotlinx.coroutines.launch
import net.typeblog.lpac_jni.impl.DEFAULT_PKID_GSMA_RSP2_ROOT_CI1
import net.typeblog.lpac_jni.impl.PKID_GSMA_TEST_CI

class EuiccInfoActivity : BaseEuiccAccessActivity() {
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var infoList: RecyclerView

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

    private fun refresh() {
        swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            (infoList.adapter!! as EuiccInfoAdapter).euiccInfoItems =
                euiccChannelManager.withEuiccChannel(logicalSlotId, ::buildPairs).map {
                    Pair(getString(it.first), it.second ?: getString(R.string.unknown))
                }

            swipeRefresh.isRefreshing = false
        }
    }

    private fun buildPairs(channel: EuiccChannel) = buildList {
        val forYesNo: (ok: Boolean) -> String =
            { ok -> getString(if (ok) R.string.yes else R.string.no) }
        val forSupport: (supported: Boolean) -> String =
            { supported -> getString(if (supported) R.string.supported else R.string.unsupported) }
        // @formatter:off
        add(Pair(R.string.euicc_info_access_mode, channel.type))
        add(Pair(R.string.euicc_info_removable, forYesNo(channel.port.card.isRemovable)))
        add(Pair(R.string.euicc_info_eid, channel.lpa.eID))
        channel.lpa.euiccInfo2.let { info ->
            add(Pair(R.string.euicc_info_firmware_version, info?.euiccFirmwareVersion))
            add(Pair(R.string.euicc_info_globalplatform_version, info?.globalPlatformVersion))
            add(Pair(R.string.euicc_info_pp_version, info?.ppVersion))
            add(Pair(R.string.euicc_info_sas_accreditation_number, info?.sasAccreditationNumber))
            add(Pair(R.string.euicc_info_free_nvram, info?.freeNvram?.let(::formatFreeSpace)))
        }
        channel.lpa.euiccInfo2?.euiccCiPKIdListForSigning.orEmpty().let { signers ->
            add(Pair(R.string.euicc_info_gsma_prod, forSupport(signers.contains(DEFAULT_PKID_GSMA_RSP2_ROOT_CI1))))
            add(Pair(R.string.euicc_info_gsma_test, forSupport(PKID_GSMA_TEST_CI.any(signers::contains))))
        }
        // @formatter:on
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
        var euiccInfoItems: List<Pair<String, String>> = listOf()
            @SuppressLint("NotifyDataSetChanged")
            set(newVal) {
                field = newVal
                notifyDataSetChanged()
            }

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