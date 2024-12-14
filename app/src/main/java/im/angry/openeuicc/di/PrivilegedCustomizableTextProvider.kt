package im.angry.openeuicc.di

import android.content.Context
import im.angry.openeuicc.R

class PrivilegedCustomizableTextProvider(private val context: Context) :
    DefaultCustomizableTextProvider(context) {
    override val noEuiccExplanation: String
        get() = context.getString(R.string.no_euicc_priv)
}