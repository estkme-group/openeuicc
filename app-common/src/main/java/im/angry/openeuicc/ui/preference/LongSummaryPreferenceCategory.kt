package im.angry.openeuicc.ui.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder

@Suppress("unused")
class LongSummaryPreferenceCategory: PreferenceCategory {
    constructor(ctx: Context): super(ctx)
    constructor(ctx: Context, attrs: AttributeSet): super(ctx, attrs)
    constructor(ctx: Context, attrs: AttributeSet, defStyle: Int): super(ctx, attrs, defStyle)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val summaryText = holder.findViewById(android.R.id.summary) as TextView
        summaryText.isSingleLine = false
        summaryText.maxLines = 10
    }
}
