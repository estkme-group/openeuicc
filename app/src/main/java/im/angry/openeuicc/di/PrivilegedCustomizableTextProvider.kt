package im.angry.openeuicc.di

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.core.os.ConfigurationCompat
import im.angry.openeuicc.R

class PrivilegedCustomizableTextProvider(private val context: Context) :
    DefaultCustomizableTextProvider(context) {
    override val websiteUri: Uri
        get() {
            val language = ConfigurationCompat.getLocales(context.resources.configuration).get(0)?.toLanguageTag() ?: ""
            return context.getString(R.string.pref_info_website_url).toUri().buildUpon()
                .appendQueryParameter("hl", language) // host language for localized website
                .build()
        }

    override val noEuiccExplanation: String
        get() = context.getString(R.string.no_euicc_priv)
}
