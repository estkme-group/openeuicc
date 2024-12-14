package im.angry.openeuicc.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import im.angry.openeuicc.common.R
import im.angry.openeuicc.util.*

class NoEuiccPlaceholderFragment : Fragment(), OpenEuiccContextMarker {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_no_euicc_placeholder, container, false)
        val textView = view.requireViewById<TextView>(R.id.no_euicc_placeholder)
        textView.text = appContainer.customizableTextProvider.noEuiccExplanation
        return view
    }
}