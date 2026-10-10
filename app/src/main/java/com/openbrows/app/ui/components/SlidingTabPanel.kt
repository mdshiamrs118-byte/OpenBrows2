package com.openbrows.app.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.openbrows.app.R

/** Glass panel that slides in from the right edge of its parent. */
class SlidingTabPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var isOpen = false
        private set

    init {
        setBackgroundResource(R.drawable.bg_glassmorphic)
    }

    private val slideDistance: Float
        get() = resources.getDimension(R.dimen.sliding_panel_width)

    fun open() {
        if (isOpen) return
        isOpen = true
        animate().cancel()
        translationX = slideDistance
        visibility = View.VISIBLE
        animate()
            .translationX(0f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun close() {
        if (!isOpen) return
        isOpen = false
        animate().cancel()
        animate()
            .translationX(slideDistance)
            .setDuration(180)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { if (!isOpen) visibility = View.GONE }
            .start()
    }

    fun toggle() {
        if (isOpen) close() else open()
    }
}
