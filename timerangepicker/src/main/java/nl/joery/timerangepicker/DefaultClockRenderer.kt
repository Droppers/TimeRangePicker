package nl.joery.timerangepicker

import android.graphics.*
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import nl.joery.timerangepicker.utils.dp
import nl.joery.timerangepicker.utils.px
import kotlin.math.cos
import kotlin.math.sin

@Keep
class DefaultClockRenderer(private val picker: TimeRangePicker): BitmapCachedClockRenderer {
    private val _minuteTickWidth = 1.px
    private val _hourTickWidth = 2.px
    private var _middle: Float = 0f

    private var _bitmapCache: Bitmap? = null
    private var _bitmapCacheCanvas: Canvas? = null

    private var _isBitmapCacheEnabled = true
    override var isBitmapCacheEnabled: Boolean
        get() = _isBitmapCacheEnabled
        set(value) {
            val oldValue = _isBitmapCacheEnabled
            _isBitmapCacheEnabled = value

            if(oldValue != value) {
                invalidateBitmapCache()
            }
        }

    private val _tickLength
        get() = when (picker.clockFace) {
            TimeRangePicker.ClockFace.APPLE -> 6.px
            TimeRangePicker.ClockFace.SAMSUNG -> 4.px
        }

    private val _tickCount: Int
        get() = when (picker.clockFace) {
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

    override fun render(canvas: Canvas) {
        if(isBitmapCacheEnabled) {
            val radius = picker.clockRadius
            val bitmap = _bitmapCache ?: throw RuntimeException("_bitmapCache == null")

            val center = canvas.width / 2
            val bitmapLeft = center - radius
            val bitmapTop = center - radius

            canvas.drawBitmap(bitmap, bitmapLeft, bitmapTop, null)
        } else {
            renderInternal(canvas)
        }
    }

    override fun invalidateBitmapCache() {
        if (isBitmapCacheEnabled) {
            val size = picker.clockRadius.toInt() * 2

            if(size > 0) {
                val bitmap = _bitmapCache

                if (bitmap == null) {
                    resizeBitmapCacheThroughRecreating(size)
                } else {
                    val oldSize = bitmap.width
                    // width & height are always the same, so there is no need to do extra check with height
                    if (oldSize != size) {
                        // reconfiguring works only when new size is smaller or equal to old
                        if (Build.VERSION.SDK_INT >= 19 && size < oldSize) {
                            resizeBitmapCacheThroughReconfiguring(size)
                        } else {
                            resizeBitmapCacheThroughRecreating(size)
                        }
                    }
                }

                val canvas =
                    _bitmapCacheCanvas ?: throw RuntimeException("_bitmapCacheCanvas == null")
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

                renderInternal(canvas)
            }
        } else {
            recycleBitmapCache()
        }
    }

    override fun recycleBitmapCache() {
        _bitmapCache?.recycle()
        _bitmapCache = null
        _bitmapCacheCanvas = null
    }

    private fun resizeBitmapCacheThroughRecreating(newSize: Int) {
        // recycle old cache
        _bitmapCache?.recycle()

        val b = Bitmap.createBitmap(newSize, newSize, Bitmap.Config.ARGB_8888)
        _bitmapCache = b
        _bitmapCacheCanvas = Canvas(b)
    }

    @RequiresApi(19)
    private fun resizeBitmapCacheThroughReconfiguring(newSize: Int) {
        val bitmap = _bitmapCache ?: throw RuntimeException("_bitmapCache == null")
        bitmap.reconfigure(newSize, newSize, Bitmap.Config.ARGB_8888)
        _bitmapCacheCanvas = Canvas(bitmap)
    }

    private fun renderInternal(canvas: Canvas) {
        _middle = (canvas.width / 2).toFloat()

        _tickPaint.color = picker.clockTickColor
        _labelPaint.apply {
            textSize = picker.clockLabelSize.toFloat()
            color = picker.clockLabelColor
        }

        drawTicks(canvas)
        drawLabels(canvas)
    }

    private fun drawTicks(canvas: Canvas) {
        val radius = picker.clockRadius
        val hourTickInterval = if(picker.hourFormat == TimeRangePicker.HourFormat.FORMAT_24) 24 else 12
        val tickLength = _tickLength
        val tickCount = _tickCount
        val hourTick = tickCount / hourTickInterval
        val offset = if(picker.clockLabelSize.dp <= 16) 3 else 6
        val anglePerTick = 360f / tickCount

        for (i in 0 until tickCount) {
            val angle = anglePerTick * i
            val angleRadians = Math.toRadians(angle.toDouble())
            val stopRadius = radius - tickLength

            val sinAngle = sin(angleRadians).toFloat()
            val cosAngle = cos(angleRadians).toFloat()

            val startX = _middle + radius * cosAngle
            val startY = _middle + radius * sinAngle

            val stopX = _middle + stopRadius * cosAngle
            val stopY = _middle + stopRadius * sinAngle

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

    private fun drawLabels(canvas: Canvas) {
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
        val tickLength = _tickLength.toFloat()
        val radius = picker.clockRadius

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
        outPoint.x = _middle + radius * cos(angleRadians).toFloat()
        outPoint.y = _middle + radius * sin(angleRadians).toFloat()
    }

    companion object {
        private val LABELS_APPLE_24 = arrayOf("0", "2", "4", "6", "8", "10", "12", "14", "16", "18", "20", "22")
        private val LABELS_APPLE_12 =  arrayOf("12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11")

        private val LABELS_SAMSUNG_24 = arrayOf("0", "6", "12", "18")
        private val LABELS_SAMSUNG_12 = arrayOf("12", "3", "6", "9")
    }
}