package nl.joery.demo.timerangepicker

import android.graphics.Color
import androidx.annotation.ColorInt

object Utils {
    fun colorToString(@ColorInt color: Int): String {
        return if(Color.alpha(color) == 255) {
            "#%06X".format(0xFFFFFF and color)
        } else {
            "#%08X".format(color)
        }
    }
}