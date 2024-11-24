package im.angry.openeuicc.ui.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import im.angry.openeuicc.common.R
import im.angry.openeuicc.service.EuiccChannelManagerService
import im.angry.openeuicc.util.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.typeblog.lpac_jni.ProfileDownloadCallback

class DownloadWizardProgressFragment : DownloadWizardActivity.DownloadWizardStepFragment() {
    companion object {
        /**
         * An array of LPA-side state types, mapping 1:1 to progressItems
         */
        val LPA_PROGRESS_STATES = arrayOf(
            ProfileDownloadCallback.DownloadState.Preparing,
            ProfileDownloadCallback.DownloadState.Connecting,
            ProfileDownloadCallback.DownloadState.Authenticating,
            ProfileDownloadCallback.DownloadState.Downloading,
            ProfileDownloadCallback.DownloadState.Finalizing,
        )
    }

    private enum class ProgressState {
        NotStarted,
        InProgress,
        Done,
        Error
    }

    private data class ProgressItem(
        val titleRes: Int,
        var state: ProgressState
    )

    private val progressItems = arrayOf(
        ProgressItem(R.string.download_wizard_progress_step_preparing, ProgressState.NotStarted),
        ProgressItem(R.string.download_wizard_progress_step_connecting, ProgressState.NotStarted),
        ProgressItem(
            R.string.download_wizard_progress_step_authenticating,
            ProgressState.NotStarted
        ),
        ProgressItem(R.string.download_wizard_progress_step_downloading, ProgressState.NotStarted),
        ProgressItem(R.string.download_wizard_progress_step_finalizing, ProgressState.NotStarted)
    )

    private val adapter = ProgressItemAdapter()

    private var isDone = false

    override val hasNext: Boolean
        get() = isDone
    override val hasPrev: Boolean
        get() = false

    override fun createNextFragment(): DownloadWizardActivity.DownloadWizardStepFragment? =
        if (state.downloadError != null) {
            DownloadWizardDiagnosticsFragment()
        } else {
            null
        }

    override fun createPrevFragment(): DownloadWizardActivity.DownloadWizardStepFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_download_progress, container, false)
        val recyclerView = view.requireViewById<RecyclerView>(R.id.download_progress_list)
        recyclerView.adapter = adapter
        recyclerView.layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL
            )
        )
        return view
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch {
            showProgressBar(-1) // set indeterminate first
            ensureEuiccChannelManager()

            val subscriber = startDownloadOrSubscribe()

            if (subscriber == null) {
                requireActivity().finish()
                return@launch
            }

            subscriber.onEach {
                when (it) {
                    is EuiccChannelManagerService.ForegroundTaskState.Done -> {
                        hideProgressBar()

                        // Change the state of the last InProgress item to Error
                        progressItems.forEachIndexed { index, progressItem ->
                            if (progressItem.state == ProgressState.InProgress) {
                                progressItem.state = ProgressState.Error
                            }

                            adapter.notifyItemChanged(index)
                        }

                        state.downloadError =
                            it.error as? EuiccChannelManagerService.ProfileDownloadException

                        isDone = true
                        refreshButtons()
                    }

                    is EuiccChannelManagerService.ForegroundTaskState.InProgress -> {
                        updateProgress(it.progress)
                    }

                    else -> {}
                }
            }.collect()
        }
    }

    private suspend fun startDownloadOrSubscribe(): EuiccChannelManagerService.ForegroundTaskSubscriberFlow? =
        if (state.downloadStarted) {
            // This will also return null if task ID is -1 (uninitialized), too
            euiccChannelManagerService.recoverForegroundTaskSubscriber(state.downloadTaskID)
        } else {
            euiccChannelManagerService.waitForForegroundTask()

            val (slotId, portId) = euiccChannelManager.withEuiccChannel(state.selectedLogicalSlot) { channel ->
                Pair(channel.slotId, channel.portId)
            }

            // Set started to true even before we start -- in case we get killed in the middle
            state.downloadStarted = true

            val ret = euiccChannelManagerService.launchProfileDownloadTask(
                slotId,
                portId,
                state.smdp,
                state.matchingId,
                state.confirmationCode,
                state.imei
            )

            state.downloadTaskID = ret.taskId

            ret
        }

    private fun updateProgress(progress: Int) {
        showProgressBar(progress)

        val lpaState = ProfileDownloadCallback.lookupStateFromProgress(progress)
        val stateIndex = LPA_PROGRESS_STATES.indexOf(lpaState)

        if (stateIndex > 0) {
            for (i in (0..<stateIndex)) {
                if (progressItems[i].state != ProgressState.Done) {
                    progressItems[i].state = ProgressState.Done
                    adapter.notifyItemChanged(i)
                }
            }
        }

        if (progressItems[stateIndex].state != ProgressState.InProgress) {
            progressItems[stateIndex].state = ProgressState.InProgress
            adapter.notifyItemChanged(stateIndex)
        }
    }

    private inner class ProgressItemHolder(val root: View) : RecyclerView.ViewHolder(root) {
        private val title = root.requireViewById<TextView>(R.id.download_progress_item_title)
        private val progressBar =
            root.requireViewById<ProgressBar>(R.id.download_progress_icon_progress)
        private val icon = root.requireViewById<ImageView>(R.id.download_progress_icon)

        fun bind(item: ProgressItem) {
            title.text = getString(item.titleRes)

            when (item.state) {
                ProgressState.NotStarted -> {
                    progressBar.visibility = View.GONE
                    icon.visibility = View.GONE
                }

                ProgressState.InProgress -> {
                    progressBar.visibility = View.VISIBLE
                    icon.visibility = View.GONE
                }

                ProgressState.Done -> {
                    progressBar.visibility = View.GONE
                    icon.setImageResource(R.drawable.ic_checkmark_outline)
                    icon.visibility = View.VISIBLE
                }

                ProgressState.Error -> {
                    progressBar.visibility = View.GONE
                    icon.setImageResource(R.drawable.ic_error_outline)
                    icon.visibility = View.VISIBLE
                }
            }
        }
    }

    private inner class ProgressItemAdapter : RecyclerView.Adapter<ProgressItemHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressItemHolder {
            val root = LayoutInflater.from(parent.context)
                .inflate(R.layout.download_progress_item, parent, false)
            return ProgressItemHolder(root)
        }

        override fun getItemCount(): Int = progressItems.size

        override fun onBindViewHolder(holder: ProgressItemHolder, position: Int) {
            holder.bind(progressItems[position])
        }
    }
}