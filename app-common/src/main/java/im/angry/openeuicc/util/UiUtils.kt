package im.angry.openeuicc.util

import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import im.angry.openeuicc.common.R
import java.io.FileOutputStream

// Source: <https://stackoverflow.com/questions/12478520/how-to-set-dialogfragments-width-and-height>
/**
 * Call this method (in onActivityCreated or later) to set
 * the width of the dialog to a percentage of the current
 * screen width.
 */
fun DialogFragment.setWidthPercent(percentage: Int) {
    val percent = percentage.toFloat() / 100
    val dm = Resources.getSystem().displayMetrics
    val rect = dm.run { Rect(0, 0, widthPixels, heightPixels) }
    val percentWidth = rect.width() * percent
    dialog?.window?.setLayout(percentWidth.toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
}

/**
 * Call this method (in onActivityCreated or later)
 * to make the dialog near-full screen.
 */
fun DialogFragment.setFullScreen() {
    dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
}

fun AppCompatActivity.setupToolbarInsets() {
    val spacer = requireViewById<View>(R.id.toolbar_spacer)
    ViewCompat.setOnApplyWindowInsetsListener(requireViewById(R.id.toolbar)) { v, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
        )

        v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = bars.top
        }
        v.updatePadding(bars.left, v.paddingTop, bars.right, v.paddingBottom)

        spacer.updateLayoutParams {
            height = v.top
        }

        WindowInsetsCompat.CONSUMED
    }
}

fun setupRootViewInsets(view: ViewGroup) {
    // Disable clipToPadding to make sure content actually display
    view.clipToPadding = false
    ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
        )

        v.updatePadding(bars.left, v.paddingTop, bars.right, bars.bottom)

        WindowInsetsCompat.CONSUMED
    }
}

fun <T : ActivityResultCaller> T.setupLogSaving(
    getLogFileName: () -> String,
    getLogText: () -> String
): () -> Unit {
    val launchSaveIntent =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri == null) return@registerForActivityResult

            val contentResolver = when (this@setupLogSaving) {
                is Context -> contentResolver
                is Fragment -> requireContext().contentResolver
                else -> throw IllegalArgumentException("Must be either Context or Fragment!")
            }

            contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { os ->
                    os.write(getLogText().encodeToByteArray())
                }
            }
        }

    return {
        launchSaveIntent.launch(getLogFileName())
    }
}