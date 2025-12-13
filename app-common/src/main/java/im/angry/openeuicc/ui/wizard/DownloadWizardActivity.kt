package im.angry.openeuicc.ui.wizard

import android.app.assist.AssistContent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import im.angry.openeuicc.common.R
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.ui.BaseEuiccAccessActivity
import im.angry.openeuicc.util.LPAString
import im.angry.openeuicc.util.OpenEuiccContextMarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.typeblog.lpac_jni.LocalProfileAssistant

class DownloadWizardActivity : BaseEuiccAccessActivity() {
    companion object {
        const val TAG = "DownloadWizardActivity"

        private const val FIELD_LOGICAL_SLOT_ID = "selectedLogicalSlot"

        fun newIntent(
            context: Context,
            logicalSlotId: Int = 0,
            seId: EuiccChannel.SecureElementId = EuiccChannel.SecureElementId.DEFAULT
        ) = Intent(context, DownloadWizardActivity::class.java).apply {
            val selectedSyntheticSlotId = DownloadWizardSlotSelectFragment
                .encodeSyntheticSlotId(logicalSlotId, seId)
            putExtra(FIELD_LOGICAL_SLOT_ID, selectedSyntheticSlotId)
        }
    }

    data class DownloadWizardState(
        var currentStepFragmentClassName: String?,
        var selectedSyntheticSlotId: Int,
        var smdp: String,
        var matchingId: String?,
        var confirmationCode: String?,
        var imei: String?,
        var downloadStarted: Boolean,
        var downloadTaskID: Long,
        var downloadError: LocalProfileAssistant.ProfileDownloadException?,
        var skipMethodSelect: Boolean,
        var confirmationCodeRequired: Boolean,
    )

    private lateinit var state: DownloadWizardState

    private lateinit var progressBar: ProgressBar
    private lateinit var nextButton: Button
    private lateinit var prevButton: Button

    private var currentFragment: DownloadWizardStepFragment? = null
        set(value) {
            if (this::state.isInitialized) {
                state.currentStepFragmentClassName = value?.javaClass?.name
            }
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_wizard)
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Make back == prev
                onPrevPressed()
            }
        })

        state = DownloadWizardState(
            currentStepFragmentClassName = null,
            selectedSyntheticSlotId = intent.getIntExtra(FIELD_LOGICAL_SLOT_ID, 0),
            smdp = "",
            matchingId = null,
            confirmationCode = null,
            imei = null,
            downloadStarted = false,
            downloadTaskID = -1,
            downloadError = null,
            skipMethodSelect = false,
            confirmationCodeRequired = false,
        )

        handleDeepLink()

        progressBar = requireViewById(R.id.progress)
        nextButton = requireViewById(R.id.download_wizard_next)
        prevButton = requireViewById(R.id.download_wizard_back)

        nextButton.setOnClickListener {
            onNextPressed()
        }

        prevButton.setOnClickListener {
            onPrevPressed()
        }

        val navigation = requireViewById<View>(R.id.download_wizard_navigation)
        val origHeight = navigation.layoutParams.height
        val fragmentRoot = requireViewById<View>(R.id.step_fragment_container)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView.rootView) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
                    or WindowInsetsCompat.Type.ime()
            )

            fragmentRoot.updatePadding(bars.left, bars.top, bars.right, 0)
            navigation.updatePadding(bars.left, 0, bars.right, bars.bottom)
            val newNavParams = navigation.layoutParams
            newNavParams.height = origHeight + bars.bottom
            navigation.layoutParams = newNavParams
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun handleDeepLink() {
        // If we get an LPA string from deep-link intents, extract from there.
        // Note that `onRestoreInstanceState` could override this with user input,
        // but that _is_ the desired behavior.
        val uri = intent.data ?: return
        if (uri.scheme.contentEquals("lpa", ignoreCase = true)) {
            val parsed = LPAString.parse(uri.schemeSpecificPart)
            state.smdp = parsed.address
            state.matchingId = parsed.matchingId
            state.confirmationCodeRequired = parsed.confirmationCodeRequired
            state.skipMethodSelect = true
        }
    }

    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)
        outContent?.webUri = try {
            val activationCode = LPAString(
                state.smdp,
                state.matchingId,
                null,
                state.confirmationCode != null,
            )
            "LPA:$activationCode".toUri()
        } catch (_: Exception) {
            null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentStepFragmentClassName", state.currentStepFragmentClassName)
        outState.putInt("selectedLogicalSlot", state.selectedSyntheticSlotId)
        outState.putString("smdp", state.smdp)
        outState.putString("matchingId", state.matchingId)
        outState.putString("confirmationCode", state.confirmationCode)
        outState.putString("imei", state.imei)
        outState.putBoolean("downloadStarted", state.downloadStarted)
        outState.putLong("downloadTaskID", state.downloadTaskID)
        outState.putBoolean("confirmationCodeRequired", state.confirmationCodeRequired)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        state.currentStepFragmentClassName = savedInstanceState.getString(
            "currentStepFragmentClassName",
            state.currentStepFragmentClassName
        )
        state.selectedSyntheticSlotId =
            savedInstanceState.getInt("selectedSyntheticSlotId", state.selectedSyntheticSlotId)
        state.smdp = savedInstanceState.getString("smdp", state.smdp)
        state.matchingId = savedInstanceState.getString("matchingId", state.matchingId)
        state.imei = savedInstanceState.getString("imei", state.imei)
        state.downloadStarted =
            savedInstanceState.getBoolean("downloadStarted", state.downloadStarted)
        state.downloadTaskID = savedInstanceState.getLong("downloadTaskID", state.downloadTaskID)
        state.confirmationCode =
            savedInstanceState.getString("confirmationCode", state.confirmationCode)
        state.confirmationCodeRequired = savedInstanceState.getBoolean(
            "confirmationCodeRequired",
            state.confirmationCodeRequired
        )
    }

    private fun onPrevPressed() {
        hideIme()

        if (currentFragment?.hasPrev == true) {
            val prevFrag = currentFragment?.createPrevFragment()
            if (prevFrag == null) {
                finish()
            } else {
                showFragment(prevFrag, R.anim.slide_in_left, R.anim.slide_out_right)
            }
        }
    }

    private fun onNextPressed() {
        hideIme()

        nextButton.isEnabled = false
        progressBar.visibility = View.VISIBLE
        progressBar.isIndeterminate = true

        lifecycleScope.launch(Dispatchers.Main) {
            if (state.selectedSyntheticSlotId >= 0) {
                try {
                    val (slotId, seId) = DownloadWizardSlotSelectFragment.decodeSyntheticSlotId(
                        state.selectedSyntheticSlotId
                    )
                    // This is run on IO by default
                    euiccChannelManager.withEuiccChannel(slotId, seId) { channel ->
                        // Be _very_ sure that the channel we got is valid
                        if (!channel.valid) throw EuiccChannelManager.EuiccChannelNotFoundException()
                    }
                } catch (e: EuiccChannelManager.EuiccChannelNotFoundException) {
                    Toast.makeText(
                        this@DownloadWizardActivity,
                        R.string.download_wizard_slot_removed,
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }

            progressBar.visibility = View.GONE
            nextButton.isEnabled = true

            if (currentFragment?.hasNext == true) {
                currentFragment?.beforeNext()
                val nextFrag = currentFragment?.createNextFragment()
                if (nextFrag == null) {
                    finish()
                } else {
                    showFragment(nextFrag, R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
        }
    }

    override fun onInit() {
        progressBar.visibility = View.GONE

        if (state.currentStepFragmentClassName != null) {
            val clazz = Class.forName(state.currentStepFragmentClassName!!)
            showFragment(clazz.getDeclaredConstructor().newInstance() as DownloadWizardStepFragment)
        } else {
            showFragment(DownloadWizardSlotSelectFragment())
        }
    }

    private fun showFragment(
        nextFrag: DownloadWizardStepFragment,
        enterAnim: Int = 0,
        exitAnim: Int = 0
    ) {
        currentFragment = nextFrag
        supportFragmentManager.beginTransaction().setCustomAnimations(enterAnim, exitAnim)
            .replace(R.id.step_fragment_container, nextFrag)
            .commit()

        // Sync screen on state
        if (nextFrag.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        refreshButtons()
    }

    private fun refreshButtons() {
        currentFragment?.let {
            nextButton.visibility = if (it.hasNext) {
                View.VISIBLE
            } else {
                View.GONE
            }
            prevButton.visibility = if (it.hasPrev) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun hideIme() {
        currentFocus?.let {
            val imm = getSystemService(InputMethodManager::class.java)
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    abstract class DownloadWizardStepFragment : Fragment(), OpenEuiccContextMarker {
        protected val state: DownloadWizardState
            get() = (requireActivity() as DownloadWizardActivity).state

        open val keepScreenOn = false

        abstract val hasNext: Boolean
        abstract val hasPrev: Boolean
        abstract fun createNextFragment(): DownloadWizardStepFragment?
        abstract fun createPrevFragment(): DownloadWizardStepFragment?

        protected fun gotoNextFragment(next: DownloadWizardStepFragment? = null) {
            val realNext = next ?: createNextFragment()
            (requireActivity() as DownloadWizardActivity).showFragment(
                realNext!!,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
        }

        protected fun hideProgressBar() {
            (requireActivity() as DownloadWizardActivity).progressBar.visibility = View.GONE
        }

        protected fun showProgressBar(progressValue: Int) {
            (requireActivity() as DownloadWizardActivity).progressBar.apply {
                visibility = View.VISIBLE
                if (progressValue >= 0) {
                    isIndeterminate = false
                    progress = progressValue
                } else {
                    isIndeterminate = true
                }
            }
        }

        protected fun refreshButtons() {
            (requireActivity() as DownloadWizardActivity).refreshButtons()
        }

        open fun beforeNext() {}
    }
}
