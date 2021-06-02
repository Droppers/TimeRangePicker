package nl.joery.timerangepicker

import android.graphics.*
import nl.joery.timerangepicker.utils.dp
import nl.joery.timerangepicker.utils.px
import kotlin.math.cos
import kotlin.math.sin


internal class ClockRenderer(private val timeRangePicker: TimeRangePicker) {
    private val _minuteTickWidth = 1.px
    private val _hourTickWidth = 2.px
    private val _middle = PointF(0f, 0f)

    private val _tickLength: Int
        get() = when (timeRangePicker.clockFace) {
            TimeRangePicker.ClockFace.APPLE -> 6.px
            TimeRangePicker.ClockFace.SAMSUNG -> 4.px
        }
    private val _tickCount: Int
        get() = when (timeRangePicker.clockFace) {
            TimeRangePicker.ClockFace.APPLE -> 48
            TimeRangePicker.ClockFace.SAMSUNG -> 120
        }

    private val _tickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val _labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun updatePaint() {
        _tickPaint.apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = timeRangePicker.clockTickColor
        }

        _labelPaint.apply {
            textSize = timeRangePicker.clockLabelSize.toFloat()
            color = timeRangePicker.clockLabelColor
            textAlign = Paint.Align.CENTER
        }
    }

    fun render(canvas: Canvas, radius: Float) {
        _middle.x = canvas.width / 2f
        _middle.y = canvas.width / 2f

        drawTicks(canvas, radius)
        drawLabels(canvas, radius)
    }

    private fun drawTicks(canvas: Canvas, radius: Float) {
        val hourTickInterval = if(timeRangePicker.hourFormat == TimeRangePicker.HourFormat.FORMAT_24) 24 else 12

        for (i in 0 until _tickCount) {
            val angle = 360f / _tickCount * i

            val start = getPositionByAngle(radius, angle)
            val stop = getPositionByAngle(radius - _tickLength, angle)

            val offset = if(timeRangePicker.clockLabelSize.dp <= 16) 3 else 6
            if (timeRangePicker.clockFace == TimeRangePicker.ClockFace.SAMSUNG &&
                ((angle >= 90-offset && angle <= 90+offset) ||
                (angle >= 180-offset && angle <= 180+offset) ||
                (angle >= 270-offset && angle <= 270+offset) ||
                angle >= 360-offset ||
                angle <= 0+offset)) {
                continue
            }

            // Hour tick
            if (i % (_tickCount / hourTickInterval) == 0) {
                _tickPaint.alpha = 180
                _tickPaint.strokeWidth = _hourTickWidth.toFloat()
            } else {
                _tickPaint.alpha = 100
                _tickPaint.strokeWidth = _minuteTickWidth.toFloat()
            }
            canvas.drawLine(start.x, start.y, stop.x, stop.y, _tickPaint)
        }
    }

    private fun drawLabels(canvas: Canvas, radius: Float) {
        val labels = when (timeRangePicker.clockFace) {
            TimeRangePicker.ClockFace.APPLE -> {
                if (timeRangePicker.hourFormat == TimeRangePicker.HourFormat.FORMAT_24) {
                    arrayOf("0", "2", "4", "6", "8", "10", "12", "14", "16", "18", "20", "22")
                } else {
                    arrayOf("12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11")
                }
            }
            TimeRangePicker.ClockFace.SAMSUNG -> {
                if (timeRangePicker.hourFormat == TimeRangePicker.HourFormat.FORMAT_24) {
                    arrayOf("0", "6", "12", "18")
                } else {
                    arrayOf("12", "3", "6", "9")
                }
            }
        }

        for (i in labels.indices) {
            val label = labels[i]
            val angle = 360f / labels.size * i - 90f

            val bounds = Rect()
            _labelPaint.getTextBounds(label, 0, label.length, bounds)
            val offset = when (timeRangePicker.clockFace) {
                TimeRangePicker.ClockFace.APPLE -> _tickLength.toFloat() * 2 + bounds.height()
                TimeRangePicker.ClockFace.SAMSUNG -> (if(angle == 0f || angle == 180f) bounds.width() else bounds.height()).toFloat() / 2
            }
            val position =
                getPositionByAngle(radius - offset, angle)
            canvas.drawText(
                label,
                position.x,
                position.y + bounds.height() / 2f,
                _labelPaint
            )
        }
    }

    private fun getPositionByAngle(
        radius: Float,
        angle: Float
    ): PointF {
        return PointF(
            (_middle.x + radius * cos(Math.toRadians(angle.toDouble()))).toFloat(),
            (_middle.y + radius * sin(Math.toRadians(angle.toDouble()))).toFloat()
        )
    }
}