package im.angry.openeuicc.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import im.angry.easyeuicc.R
import im.angry.openeuicc.di.UnprivilegedUiComponentFactory
import im.angry.openeuicc.util.OpenEuiccContextMarker

class QuickCompatibilityActivity : AppCompatActivity(), OpenEuiccContextMarker {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_quick_compatibility)

        val quickAvailabilityFragment =
            (appContainer.uiComponentFactory as UnprivilegedUiComponentFactory)
                .createQuickAvailabilityFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.quick_availability_container, quickAvailabilityFragment)
            .commit()
    }
}
