package com.openbrows.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.openbrows.app.R

/**
 * Translucent rounded "glass" bar. The frosted look comes from the translucent,
 * theme-aware background drawable (blurring the view itself would blur its icons).
 */
class GlassmorphicToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        setBackgroundResource(R.drawable.bg_glassmorphic)
    }
}
