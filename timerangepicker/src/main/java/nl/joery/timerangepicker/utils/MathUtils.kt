package nl.joery.timerangepicker.utils

import nl.joery.timerangepicker.TimeRangePicker
import kotlin.math.*

internal object MathUtils {
    private const val R2D = 180f / PI.toFloat()

    fun differenceBetweenAngles(a1: Float, a2: Float): Float {
        val angle1 = Math.toRadians(a1.toDouble())
        val angle2 = Math.toRadians(a2.toDouble())

        val sinAngle1 = sin(angle1).toFloat()
        val cosAngle1 = cos(angle1).toFloat()

        val sinAngle2 = sin(angle2).toFloat()
        val cosAngle2 = cos(angle2).toFloat()

        return atan2(
                cosAngle1 * sinAngle2 - sinAngle1 * cosAngle2,
                cosAngle1 * cosAngle2 + sinAngle1 * sinAngle2
        ) * R2D
    }

    fun angleTo360(angle: Float): Float {
        var result = angle % 360
        if (result < 0) {
            result += 360.0f
        }
        return result
    }

    fun angleTo720(angle: Float): Float {
        var result = angle % 720
        if (result < 0) {
            result += 720.0f
        }
        return result
    }

    fun simpleMinutesToAngle(minutes: Int, hourFormat: TimeRangePicker.HourFormat): Float {
        return if (hourFormat == TimeRangePicker.HourFormat.FORMAT_12) {
            minutes / (12 * 60.0f) * 360.0f
        } else {
            minutes / (24 * 60.0f) * 360.0f
        }
    }

    fun minutesToAngle(minutes: Int, hourFormat: TimeRangePicker.HourFormat): Float {
        return angleTo720(90 - simpleMinutesToAngle(minutes, hourFormat))
    }

    fun angleToPreciseMinutes(angle: Float, hourFormat: TimeRangePicker.HourFormat): Float {
        return if (hourFormat == TimeRangePicker.HourFormat.FORMAT_12) {
            (angleTo720(90 - angle) / 360 * 12 * 60) % (24 * 60)
        } else {
            (angleTo720(90 - angle) / 360 * 24 * 60) % (24 * 60)
        }
    }

    fun angleToMinutes(angle: Float, hourFormat: TimeRangePicker.HourFormat): Int {
        return if (hourFormat == TimeRangePicker.HourFormat.FORMAT_12) {
            (angleTo720(90 - angle) / 360 * 12 * 60).roundToInt() % (24 * 60)
        } else {
            (angleTo720(90 - angle) / 360 * 24 * 60).roundToInt() % (24 * 60)
        }
    }

    fun snapMinutes(minutes: Int, step: Int): Int {
        return minutes / step * step + 2 * (minutes % step) / step * step
    }

    fun isPointInCircle(
        x: Float,
        y: Float,
        cx: Float,
        cy: Float,
        radius: Float
    ): Boolean {
        return sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy)) < radius
    }

    fun distanceBetweenPoints(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float
    ): Float {
        val deltaX = x1 - x2
        val deltaY = y1 - y2
        return sqrt(deltaX * deltaX + deltaY * deltaY)
    }

    fun durationBetweenMinutes(startMinutes: Float, endMinutes: Float): Float {
        return if (startMinutes > endMinutes) {
            24f * 60f - (startMinutes - endMinutes)
        } else {
            endMinutes - startMinutes
        }
    }
}