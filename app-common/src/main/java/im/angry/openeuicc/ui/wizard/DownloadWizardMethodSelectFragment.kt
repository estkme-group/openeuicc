package im.angry.openeuicc.ui.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import im.angry.openeuicc.common.R

class DownloadWizardMethodSelectFragment : DownloadWizardActivity.DownloadWizardStepFragment() {
    data class DownloadMethod(
        val iconRes: Int,
        val titleRes: Int,
        val onClick: () -> Unit
    )

    val downloadMethods = arrayOf(
        DownloadMethod(R.drawable.ic_scan_black, R.string.download_wizard_method_qr_code) {

        },
        DownloadMethod(R.drawable.ic_gallery_black, R.string.download_wizard_method_gallery) {

        },
        DownloadMethod(R.drawable.ic_edit, R.string.download_wizard_method_manual) {
            gotoNextFragment(DownloadWizardDetailsFragment())
        }
    )

    override val hasNext: Boolean
        get() = false
    override val hasPrev: Boolean
        get() = true

    override fun createNextFragment(): DownloadWizardActivity.DownloadWizardStepFragment? {
        TODO("Not yet implemented")
    }

    override fun createPrevFragment(): DownloadWizardActivity.DownloadWizardStepFragment =
        DownloadWizardSlotSelectFragment()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_download_method_select, container, false)
        val recyclerView = view.requireViewById<RecyclerView>(R.id.download_method_list)
        recyclerView.adapter = DownloadMethodAdapter()
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

    private class DownloadMethodViewHolder(private val root: View) : ViewHolder(root) {
        private val icon = root.requireViewById<ImageView>(R.id.download_method_icon)
        private val title = root.requireViewById<TextView>(R.id.download_method_title)

        fun bind(item: DownloadMethod) {
            icon.setImageResource(item.iconRes)
            title.setText(item.titleRes)
            root.setOnClickListener { item.onClick() }
        }
    }

    private inner class DownloadMethodAdapter : RecyclerView.Adapter<DownloadMethodViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): DownloadMethodViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.download_method_item, parent, false)
            return DownloadMethodViewHolder(view)
        }

        override fun getItemCount(): Int = downloadMethods.size

        override fun onBindViewHolder(holder: DownloadMethodViewHolder, position: Int) {
            holder.bind(downloadMethods[position])
        }

    }
}