package nl.joery.timerangepicker

import android.graphics.Canvas

interface ClockRenderer {
    fun render(canvas: Canvas, picker: TimeRangePicker, radius: Float)
}