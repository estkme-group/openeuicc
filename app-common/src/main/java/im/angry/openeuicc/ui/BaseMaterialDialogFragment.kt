package im.angry.openeuicc.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.DialogFragment
import com.google.android.material.color.DynamicColors
import im.angry.openeuicc.common.R

abstract class BaseMaterialDialogFragment: DialogFragment() {
    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        val wrappedContext = ContextThemeWrapper(requireContext(), R.style.Theme_OpenEUICC)
        val dynamicWrappedContext = DynamicColors.wrapContextIfAvailable(wrappedContext)
        return inflater.cloneInContext(dynamicWrappedContext)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).also {
            it.window?.requestFeature(Window.FEATURE_NO_TITLE)
            it.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        }
    }
}