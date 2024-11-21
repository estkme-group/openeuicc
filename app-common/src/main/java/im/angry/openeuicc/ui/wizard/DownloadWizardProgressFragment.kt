package im.angry.openeuicc.ui.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import im.angry.openeuicc.common.R

class DownloadWizardProgressFragment : DownloadWizardActivity.DownloadWizardStepFragment() {
    private enum class ProgressState {
        NotStarted,
        InProgress,
        Done,
        Error
    }

    private data class ProgressItem(
        val titleRes: Int,
        val state: ProgressState
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

    override val hasNext: Boolean
        get() = false
    override val hasPrev: Boolean
        get() = false

    override fun createNextFragment(): DownloadWizardActivity.DownloadWizardStepFragment? = null

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