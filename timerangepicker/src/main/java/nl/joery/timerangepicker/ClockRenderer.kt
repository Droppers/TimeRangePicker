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
        val tickLength = _tickLength
        val tickCount = _tickCount
        val hourTick = tickCount / hourTickInterval
        val offset = if(timeRangePicker.clockLabelSize.dp <= 16) 3 else 6
        val anglePerTick = 360f / tickCount

        for (i in 0 until tickCount) {
            val angle = anglePerTick * i
            val angleRadians = Math.toRadians(angle.toDouble())
            val stopRadius = radius - tickLength

            val sinAngle = sin(angleRadians).toFloat()
            val cosAngle = cos(angleRadians).toFloat()

            val startX = _middle.x + radius * cosAngle
            val startY = _middle.y + radius * sinAngle

            val stopX = _middle.x + stopRadius * cosAngle
            val stopY = _middle.y + stopRadius * sinAngle

            if (timeRangePicker.clockFace == TimeRangePicker.ClockFace.SAMSUNG &&
                ((angle >= 90-offset && angle <= 90+offset) ||
                        (angle >= 180-offset && angle <= 180+offset) ||
                        (angle >= 270-offset && angle <= 270+offset) ||
                        angle >= 360-offset ||
                        angle <= 0+offset)) {
                continue
            }

            // Hour tick
            if (i % hourTick == 0) {
                _tickPaint.alpha = 180
                _tickPaint.strokeWidth = _hourTickWidth.toFloat()
            } else {
                _tickPaint.alpha = 100
                _tickPaint.strokeWidth = _minuteTickWidth.toFloat()
            }
            canvas.drawLine(startX, startY, stopX, stopY, _tickPaint)
        }
    }

    private val drawLabelsBounds = Rect()
    private val drawLabelsPosition = PointF()

    private fun drawLabels(canvas: Canvas, radius: Float) {
        val labels = when (timeRangePicker.clockFace) {
            TimeRangePicker.ClockFace.APPLE -> {
                if (timeRangePicker.hourFormat == TimeRangePicker.HourFormat.FORMAT_24) {
                    LABELS_APPLE_24
                } else {
                    LABELS_APPLE_12
                }
            }
            TimeRangePicker.ClockFace.SAMSUNG -> {
                if (timeRangePicker.hourFormat == TimeRangePicker.HourFormat.FORMAT_24) {
                    LABELS_SAMSUNG_24
                } else {
                    LABELS_SAMSUNG_12
                }
            }
        }

        val bounds = drawLabelsBounds
        val position = drawLabelsPosition
        val tickLength = _tickLength.toFloat()

        for (i in labels.indices) {
            val label = labels[i]
            val angle = 360f / labels.size * i - 90f

            _labelPaint.getTextBounds(label, 0, label.length, bounds)
            val offset = when (timeRangePicker.clockFace) {
                TimeRangePicker.ClockFace.APPLE -> tickLength * 2 + bounds.height()
                TimeRangePicker.ClockFace.SAMSUNG -> (if(angle == 0f || angle == 180f) bounds.width() else bounds.height()).toFloat() / 2
            }

            getPositionByAngle(radius - offset, angle, position)
            canvas.drawText(
                label,
                position.x,
                position.y + bounds.height() / 2f,
                _labelPaint
            )
        }
    }

    private fun getPositionByAngle(radius: Float, angle: Float, outPoint: PointF) {
        val angleRadians = Math.toRadians(angle.toDouble())
        outPoint.x = _middle.x + radius * cos(angleRadians).toFloat()
        outPoint.y = _middle.y + radius * sin(angleRadians).toFloat()
    }

    companion object {
        private val LABELS_APPLE_24 = arrayOf("0", "2", "4", "6", "8", "10", "12", "14", "16", "18", "20", "22")
        private val LABELS_APPLE_12 =  arrayOf("12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11")

        private val LABELS_SAMSUNG_24 = arrayOf("0", "6", "12", "18")
        private val LABELS_SAMSUNG_12 = arrayOf("12", "3", "6", "9")
    }
}