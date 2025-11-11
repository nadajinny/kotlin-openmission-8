package com.example.tagmoa.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class BottomNavItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    init {
        scaleType = ScaleType.CENTER_INSIDE
        isClickable = true
        isFocusable = true
    }

    fun setMenuIcon(drawable: android.graphics.drawable.Drawable) {
        setImageDrawable(drawable)
    }
}
