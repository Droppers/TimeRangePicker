package nl.joery.timerangepicker

import android.graphics.*
import nl.joery.timerangepicker.utils.dp
import nl.joery.timerangepicker.utils.px
import kotlin.math.cos
import kotlin.math.sin

object DefaultClockRenderer: ClockRenderer {
    private val LABELS_APPLE_24 = arrayOf("0", "2", "4", "6", "8", "10", "12", "14", "16", "18", "20", "22")
    private val LABELS_APPLE_12 =  arrayOf("12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11")

    private val LABELS_SAMSUNG_24 = arrayOf("0", "6", "12", "18")
    private val LABELS_SAMSUNG_12 = arrayOf("12", "3", "6", "9")

    private val _minuteTickWidth = 1.px
    private val _hourTickWidth = 2.px
    private val _middle = PointF(0f, 0f)

    private val TimeRangePicker._tickLength
        get() = when (clockFace) {
            TimeRangePicker.ClockFace.APPLE -> 6.px
            TimeRangePicker.ClockFace.SAMSUNG -> 4.px
        }

    private val TimeRangePicker._tickCount: Int
        get() = when (clockFace) {
            TimeRangePicker.ClockFace.APPLE -> 48
            TimeRangePicker.ClockFace.SAMSUNG -> 120
        }

    private val _tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val _labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    override fun render(canvas: Canvas, picker: TimeRangePicker, radius: Float) {
        _middle.x = canvas.width / 2f
        _middle.y = canvas.width / 2f

        _tickPaint.color = picker.clockTickColor
        _labelPaint.apply {
            textSize = picker.clockLabelSize.toFloat()
            color = picker.clockLabelColor
        }

        drawTicks(canvas, picker, radius)
        drawLabels(canvas, picker, radius)
    }

    private fun drawTicks(canvas: Canvas, picker: TimeRangePicker, radius: Float) {
        val hourTickInterval = if(picker.hourFormat == TimeRangePicker.HourFormat.FORMAT_24) 24 else 12
        val tickLength = picker._tickLength
        val tickCount = picker._tickCount
        val hourTick = tickCount / hourTickInterval
        val offset = if(picker.clockLabelSize.dp <= 16) 3 else 6
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

            if (picker.clockFace == TimeRangePicker.ClockFace.SAMSUNG &&
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

    private val _drawLabelsBounds = Rect()
    private val _drawLabelsPosition = PointF()

    private fun drawLabels(canvas: Canvas, picker: TimeRangePicker, radius: Float) {
        val labels = when (picker.clockFace) {
            TimeRangePicker.ClockFace.APPLE -> {
                if (picker.hourFormat == TimeRangePicker.HourFormat.FORMAT_24) {
                    LABELS_APPLE_24
                } else {
                    LABELS_APPLE_12
                }
            }
            TimeRangePicker.ClockFace.SAMSUNG -> {
                if (picker.hourFormat == TimeRangePicker.HourFormat.FORMAT_24) {
                    LABELS_SAMSUNG_24
                } else {
                    LABELS_SAMSUNG_12
                }
            }
        }

        val bounds = _drawLabelsBounds
        val position = _drawLabelsPosition
        val tickLength = picker._tickLength.toFloat()

        for (i in labels.indices) {
            val label = labels[i]
            val angle = 360f / labels.size * i - 90f

            _labelPaint.getTextBounds(label, 0, label.length, bounds)
            val offset = when (picker.clockFace) {
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
}