package im.angry.openeuicc.ui.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import im.angry.openeuicc.common.R

class DownloadWizardDetailsFragment : DownloadWizardActivity.DownloadWizardStepFragment() {
    override val hasNext: Boolean
        get() = false
    override val hasPrev: Boolean
        get() = true

    override fun createNextFragment(): DownloadWizardActivity.DownloadWizardStepFragment? {
        TODO("Not yet implemented")
    }

    override fun createPrevFragment(): DownloadWizardActivity.DownloadWizardStepFragment =
        DownloadWizardMethodSelectFragment()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_download_details, container, false)
        return view
    }
}