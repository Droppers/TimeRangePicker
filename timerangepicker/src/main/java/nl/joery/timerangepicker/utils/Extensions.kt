package nl.joery.timerangepicker.utils

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat

private val displayMetrics = Resources.getSystem().displayMetrics
private val density = displayMetrics.density
private val invDensity = 1f / density
private val sDensity = displayMetrics.scaledDensity

fun dpToPx(value: Float): Float = value * density
fun pxToDp(value: Float): Float = value * invDensity
fun spToPx(value: Float): Float = value * sDensity

@ColorInt
internal fun Context.getColorResCompat(@AttrRes id: Int): Int {
    return ContextCompat.getColor(this, getResourceId(id))
}

@ColorInt
internal fun Context.getTextColor(@AttrRes id: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(id, typedValue, true)
    val arr = obtainStyledAttributes(
        typedValue.data, intArrayOf(
            id
        )
    )
    val color = arr.getColor(0, -1)
    arr.recycle()
    return color
}

internal fun Context.getResourceId(id: Int): Int {
    val resolvedAttr = TypedValue()
    theme.resolveAttribute(id, resolvedAttr, true)
    return resolvedAttr.run { if (resourceId != 0) resourceId else data }
}