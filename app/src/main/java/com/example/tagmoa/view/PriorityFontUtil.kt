package com.example.tagmoa.view

import android.graphics.Typeface
import android.util.SparseArray
import android.widget.TextView
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import com.example.tagmoa.R

/**
 * Applies the appropriate Paperlogy typeface to a TextView based on subtask priority.
 * Low priority -> lighter weight, High priority -> bold weight.
 */
object PriorityFontUtil {

    private val cache = SparseArray<Typeface?>()

    fun apply(textView: TextView, priority: Int) {
        val fontRes = fontResFor(priority)
        var typeface = cache[fontRes]
        if (typeface == null) {
            typeface = ResourcesCompat.getFont(textView.context, fontRes)
            cache.put(fontRes, typeface)
        }
        typeface?.let { textView.typeface = it }
    }

    @FontRes
    private fun fontResFor(priority: Int): Int = when (priority) {
        2 -> R.font.paperlogy_7bold
        1 -> R.font.paperlogy_5medium
        else -> R.font.paperlogy_3light
    }
}
