package im.angry.openeuicc.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import im.angry.easyeuicc.R

class UnprivilegedNoEuiccPlaceholderFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.fragment_no_euicc_placeholder_unprivileged,
            container,
            false
        )

        view.findViewById<View>(R.id.compatibility_check).setOnClickListener {
            startActivity(Intent(requireContext(), CompatibilityCheckActivity::class.java))
        }

        return view
    }
}