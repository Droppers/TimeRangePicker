package nl.joery.timerangepicker

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Parcelable
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import nl.joery.timerangepicker.utils.*
import nl.joery.timerangepicker.utils.MathUtils
import nl.joery.timerangepicker.utils.MathUtils.differenceBetweenAngles
import nl.joery.timerangepicker.utils.MathUtils.angleTo360
import nl.joery.timerangepicker.utils.MathUtils.angleTo720
import nl.joery.timerangepicker.utils.MathUtils.angleToMinutes
import nl.joery.timerangepicker.utils.MathUtils.angleToPreciseMinutes
import nl.joery.timerangepicker.utils.MathUtils.durationBetweenMinutes
import nl.joery.timerangepicker.utils.MathUtils.minutesToAngle
import nl.joery.timerangepicker.utils.MathUtils.simpleMinutesToAngle
import nl.joery.timerangepicker.utils.MathUtils.snapMinutes
import nl.joery.timerangepicker.utils.getColorResCompat
import nl.joery.timerangepicker.utils.px
import nl.joery.timerangepicker.utils.sp
import java.time.Duration
import java.time.LocalTime
import java.util.*
import javax.xml.datatype.DatatypeFactory
import kotlin.math.*
import kotlin.properties.Delegates


class TimeRangePicker @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val _clockRenderer = ClockRenderer(this)

    private val _thumbStartPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val _thumbEndPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val _sliderRangePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val _sliderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val _gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val _sliderRect: RectF = RectF()
    private val _sliderCapRect: RectF = RectF()

    private var _sliderWidth: Int = 8.px
    private var _sliderColor by Delegates.notNull<Int>()
    private var _sliderRangeColor by Delegates.notNull<Int>()
    private var _sliderRangeGradientStart: Int? = null
    private var _sliderRangeGradientMiddle: Int? = null
    private var _sliderRangeGradientEnd: Int? = null

    private var _thumbSize: Int = 28.px
    private var _thumbSizeActiveGrow: Float = 1.2f
    private var _thumbIconStart: Drawable? = null
    private var _thumbIconEnd: Drawable? = null
    private var _thumbColor by Delegates.notNull<Int>()
    private var _thumbColorAuto: Boolean = true
    private var _thumbIconColor: Int? = null
    private var _thumbIconSize: Int? = null

    private var _clockVisible: Boolean = true
    private var _clockFace: ClockFace = ClockFace.APPLE
    private var _clockLabelSize = 15.sp
    private var _clockLabelColor by Delegates.notNull<Int>()
    private var _clockTickColor by Delegates.notNull<Int>()

    private var _minDurationMinutes: Int = 0
    private var _maxDurationMinutes: Int = 24 * 60
    private var _timeStepMinutes = 10

    private var onTimeChangeListener: OnTimeChangeListener? = null
    private var onDragChangeListener: OnDragChangeListener? = null

    private val _radius: Float
        get() = (min(width, height) / 2f - max(
                    max(
                        _thumbSize,
                        (_thumbSize * _thumbSizeActiveGrow).toInt()
                    ), _sliderWidth
                ) / 2f) - max(
                    max(paddingTop, paddingLeft),
                    max(paddingBottom, paddingRight)
                )

    private var _middlePoint = PointF(0f, 0f)

    private var _hourFormat = HourFormat.FORMAT_12
    private var _angleStart: Float = 0f
    private var _angleEnd: Float = 0f

    private var _activeThumb: Thumb? = null
    private var _touchOffsetAngle: Float = 0.0f

    private val _isGradientSlider: Boolean
        get() = _sliderRangeGradientStart != null && _sliderRangeGradientEnd != null

    init {
        initColors()
        initAttributes(attrs)

        updateMiddlePoint()
        _clockRenderer.updatePaint()
        updatePaint()
    }

    private fun initColors() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            _sliderRangeColor = context.getColorResCompat(android.R.attr.colorPrimary)
            _thumbColor = context.getColorResCompat(android.R.attr.colorPrimary)
        } else {
            _sliderRangeColor = Color.BLUE
            _thumbColor = Color.BLUE
        }
        _sliderColor = Color.parseColor("#E1E1E1")
        _clockTickColor = context.getTextColor(android.R.attr.textColorPrimary)
        _clockLabelColor = context.getTextColor(android.R.attr.textColorPrimary)
    }

    private fun initAttributes(
        attributeSet: AttributeSet?
    ) {
        val attr: TypedArray =
            context.obtainStyledAttributes(attributeSet, R.styleable.TimeRangePicker, 0, 0)
        try {
            // Time
            hourFormat = HourFormat.fromId(
                attr.getInt(
                    R.styleable.TimeRangePicker_trp_hourFormat,
                    _hourFormat.id
                )
            ) ?: _hourFormat // Sets public property to determine 24 / 12 format automatically
            _angleStart = minutesToAngle(
                attr.getInt(
                    R.styleable.TimeRangePicker_trp_endTimeMinutes,
                    angleToMinutes(minutesToAngle(Time(0, 0).totalMinutes, _hourFormat), _hourFormat)
                ),
                _hourFormat
            )
            _angleEnd = minutesToAngle(
                attr.getInt(
                    R.styleable.TimeRangePicker_trp_startTimeMinutes,
                    angleToMinutes(minutesToAngle(Time(8, 0).totalMinutes, _hourFormat), _hourFormat)
                ),
                _hourFormat
            )
            val startTime = attr.getString(R.styleable.TimeRangePicker_trp_startTime)
            if (startTime != null) {
                _angleStart = minutesToAngle(parseTimeString(startTime).totalMinutes, _hourFormat)
            }
            val endTime = attr.getString(R.styleable.TimeRangePicker_trp_endTime)
            if (endTime != null) {
                _angleEnd = minutesToAngle(parseTimeString(endTime).totalMinutes, _hourFormat)
            }

            // Duration
            minDurationMinutes = attr.getInt(R.styleable.TimeRangePicker_trp_minDurationMinutes, _minDurationMinutes)
            maxDurationMinutes = attr.getInt(R.styleable.TimeRangePicker_trp_maxDurationMinutes, _maxDurationMinutes)

            val minDuration = attr.getString(R.styleable.TimeRangePicker_trp_minDuration)
            if (minDuration != null) {
                minDurationMinutes = parseTimeString(minDuration).totalMinutes
            }
            val maxDuration = attr.getString(R.styleable.TimeRangePicker_trp_maxDuration)
            if (maxDuration != null) {
                maxDurationMinutes = parseTimeString(maxDuration).totalMinutes
            }

            _timeStepMinutes = attr.getInt(
                R.styleable.TimeRangePicker_trp_stepTimeMinutes,
                _timeStepMinutes
            )

            // Slider
            _sliderWidth = attr.getDimension(
                R.styleable.TimeRangePicker_trp_sliderWidth,
                _sliderWidth.toFloat()
            ).toInt()
            _sliderColor = attr.getColor(R.styleable.TimeRangePicker_trp_sliderColor, _sliderColor)
            _sliderRangeColor =
                attr.getColor(R.styleable.TimeRangePicker_trp_sliderRangeColor, _sliderRangeColor)

            // Slider gradient
            val gradientStart =
                attr.getColor(R.styleable.TimeRangePicker_trp_sliderRangeGradientStart, -1)
            val gradientMiddle =
                attr.getColor(R.styleable.TimeRangePicker_trp_sliderRangeGradientMiddle, -1)
            val gradientEnd =
                attr.getColor(R.styleable.TimeRangePicker_trp_sliderRangeGradientEnd, -1)
            if (gradientStart != -1 && gradientEnd != -1) {
                _sliderRangeGradientStart = gradientStart
                _sliderRangeGradientMiddle = gradientMiddle
                _sliderRangeGradientEnd = gradientEnd
            }

            // Thumb
            _thumbSize = attr.getDimension(
                R.styleable.TimeRangePicker_trp_thumbSize,
                _thumbSize.toFloat()
            ).toInt()
            _thumbSizeActiveGrow = attr.getFloat(R.styleable.TimeRangePicker_trp_thumbSizeActiveGrow, _thumbSizeActiveGrow)

            val thumbColor = attr.getColor(R.styleable.TimeRangePicker_trp_thumbColor, 0)
            _thumbColor = if(thumbColor == 0) _thumbColor else thumbColor
            _thumbColorAuto = thumbColor == 0
            val iconColor = attr.getColor(R.styleable.TimeRangePicker_trp_thumbIconColor, 0)
            _thumbIconColor = if(iconColor == 0) null else iconColor
            val iconSize = attr.getDimension(R.styleable.TimeRangePicker_trp_thumbIconSize, -1f)
            _thumbIconSize = if(iconSize == -1f) null else iconSize.toInt()
            _thumbIconStart = attr.getDrawable(R.styleable.TimeRangePicker_trp_thumbIconStart)?.mutate()
            _thumbIconEnd = attr.getDrawable(R.styleable.TimeRangePicker_trp_thumbIconEnd)?.mutate()

            // Clock
            _clockVisible =
                attr.getBoolean(R.styleable.TimeRangePicker_trp_clockVisible, _clockVisible)
            _clockFace = ClockFace.fromId(
                attr.getInt(
                    R.styleable.TimeRangePicker_trp_clockFace,
                    _clockFace.id
                )
            ) ?: _clockFace

            _clockLabelSize = attr.getDimensionPixelSize(
                R.styleable.TimeRangePicker_trp_clockLabelSize,
                _clockLabelSize
            )
            _clockLabelColor =
                attr.getColor(R.styleable.TimeRangePicker_trp_clockLabelColor, _clockLabelColor)
            _clockTickColor = attr.getColor(
                R.styleable.TimeRangePicker_trp_clockTickColor,
                _clockTickColor
            )
        } finally {
            attr.recycle()
        }
    }

    private fun updateMiddlePoint() {
        _middlePoint.set(width / 2f, height / 2f)
    }

    private fun updatePaint() {
        _thumbStartPaint.apply {
            style = Paint.Style.FILL
            color = if (_thumbColorAuto && _isGradientSlider) _sliderRangeGradientStart!! else _thumbColor
        }
        _thumbEndPaint.apply {
            style = Paint.Style.FILL
            color = if (_thumbColorAuto && _isGradientSlider) _sliderRangeGradientEnd!! else _thumbColor
        }

        _sliderPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = _sliderWidth.toFloat()
            color = _sliderColor
        }
        _sliderRangePaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = _sliderWidth.toFloat()
            color = _sliderRangeColor
        }

        if (_isGradientSlider) {
            updateGradient()
        } else {
            _sliderRangePaint.shader = null
        }

        postInvalidate()
    }

    private fun updateGradient() {
        if (!_isGradientSlider) {
            return
        }

        val sweepAngle =
            angleTo360(_angleStart - _angleEnd)

        val positions = if (_sliderRangeGradientMiddle == null) {
            floatArrayOf(0f, sweepAngle / 360f)
        } else {
            floatArrayOf(0f, (sweepAngle / 360f) / 2, sweepAngle / 360f)
        }
        val colors = if (_sliderRangeGradientMiddle == null) {
            intArrayOf(_sliderRangeGradientStart!!, _sliderRangeGradientEnd!!)
        } else {
            intArrayOf(_sliderRangeGradientStart!!, _sliderRangeGradientMiddle!!, _sliderRangeGradientEnd!!)
        }

        val gradient: Shader =
            SweepGradient(_middlePoint.x, _middlePoint.y, colors, positions)
        val gradientMatrix = Matrix()
        gradientMatrix.preRotate(-_angleStart, _middlePoint.x, _middlePoint.y)
        gradient.setLocalMatrix(gradientMatrix)
        _sliderRangePaint.shader = gradient
    }

    private fun updateThumbIconColors() {
        if (_thumbIconColor != null) {
            if(_thumbIconStart != null) {
                DrawableCompat.setTint(_thumbIconStart!!, _thumbIconColor!!)
            }
            if(_thumbIconEnd != null) {
                DrawableCompat.setTint(_thumbIconEnd!!, _thumbIconColor!!)
            }
        }

        postInvalidate()
    }

    public override fun onSaveInstanceState(): Parcelable {
        return SavedState(super.onSaveInstanceState()).apply {
            angleStart = _angleStart
            angleEnd = _angleEnd
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            _angleStart = state.angleStart
            _angleEnd = state.angleEnd
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(w, h, oldWidth, oldHeight)

        updateMiddlePoint()
        updateGradient()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val squareSize = min(measuredWidth, measuredHeight)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(squareSize, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(squareSize, MeasureSpec.EXACTLY)
        )
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (_clockVisible) {
            _clockRenderer.render(canvas, _radius - max(_thumbSize, _sliderWidth) / 2f - 8.px)
        }

        _sliderRect.set(
            _middlePoint.x - _radius,
            _middlePoint.y - _radius,
            _middlePoint.x + _radius,
            _middlePoint.y + _radius
        )

        val sweepAngle =
            angleTo360(_angleStart - _angleEnd)

        canvas.drawCircle(
            _middlePoint.x,
            _middlePoint.y,
            _radius,
            _sliderPaint
        )

        val startThumb = getThumbPosition(
            angleTo360(_angleStart)
        )
        val endThumb = getThumbPosition(_angleEnd)

        // Draw start thumb
        canvas.drawArc(
            _sliderRect,
            -_angleStart - 0.25f,
            sweepAngle / 2f + 0.5f,
            false,
            _sliderRangePaint
        )
        drawRangeCap(
            canvas,
            startThumb,
            0f,
            if (_isGradientSlider) _sliderRangeGradientStart!! else _sliderRangeColor
        )
        drawThumb(canvas, _thumbStartPaint, _thumbIconStart, _activeThumb == Thumb.START, startThumb.x, startThumb.y)

        // Draw end thumb
        canvas.drawArc(
            _sliderRect,
            -_angleStart + sweepAngle / 2f - 0.25f,
            sweepAngle / 2f + 0.5f,
            false,
            _sliderRangePaint
        )
        drawRangeCap(
            canvas,
            endThumb,
            180f,
            if (_isGradientSlider) _sliderRangeGradientEnd!! else _sliderRangeColor
        )
        drawThumb(canvas, _thumbEndPaint, _thumbIconEnd, _activeThumb == Thumb.END, endThumb.x, endThumb.y)
    }

    private fun drawRangeCap(
        canvas: Canvas,
        position: PointF,
        rotation: Float, @ColorInt color: Int
    ) {
        val capAngle = Math.toDegrees(
            atan2(
                _middlePoint.x - position.x,
                position.y - _middlePoint.y
            ).toDouble()
        ).toFloat()
        _gradientPaint.color = color

        _sliderCapRect.set(
            position.x - _sliderWidth / 2f,
            position.y - _sliderWidth / 2f,
            position.x + _sliderWidth / 2f,
            position.y + _sliderWidth / 2f
        )
        canvas.drawArc(
            _sliderCapRect,
            capAngle - 90 + rotation,
            180f,
            true,
            _gradientPaint
        )
    }

    private fun drawThumb(canvas: Canvas, paint: Paint, icon: Drawable?, active: Boolean, x: Float, y: Float) {
        val grow = if(active) _thumbSizeActiveGrow else 1f
        val thumbRadius = (_thumbSize.toFloat() * grow) / 2f
        canvas.drawCircle(
            x,
            y,
            thumbRadius,
            paint
        )

        if (icon != null) {
            val iconSize =
                _thumbIconSize?.toFloat() ?: min(24.px.toFloat(), _thumbSize * 0.625f)
            icon.setBounds(
                (x - iconSize / 2).toInt(),
                (y - iconSize / 2).toInt(),
                (x + iconSize / 2).toInt(),
                (y + iconSize / 2f).toInt()
            )
            icon.draw(canvas)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchAngle = Math.toDegrees(
            atan2(
                _middlePoint.y - event.y,
                event.x - _middlePoint.x
            ).toDouble()
        ).toFloat()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                _activeThumb = getClosestThumb(event.x, event.y)
                return if (_activeThumb != Thumb.NONE) {
                    val targetAngleRad = if (_activeThumb == Thumb.END) _angleEnd else _angleStart
                    _touchOffsetAngle = differenceBetweenAngles(
                        targetAngleRad,
                        touchAngle
                    )

                    postInvalidate()

                    return onDragChangeListener?.onDragStart(_activeThumb!!) ?: true
                } else {
                    false
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (_activeThumb == Thumb.START || _activeThumb == Thumb.BOTH) {
                    val difference =
                        differenceBetweenAngles(_angleStart, touchAngle) - _touchOffsetAngle
                    val newStartAngle = angleTo720(_angleStart + difference)
                    val newDurationMinutes = durationBetweenMinutes(
                        angleToPreciseMinutes(newStartAngle, _hourFormat),
                        angleToPreciseMinutes(_angleEnd, _hourFormat)
                    )

                    if (_activeThumb == Thumb.BOTH) {
                        _angleStart = newStartAngle
                        _angleEnd = angleTo720(_angleEnd + difference)
                    } else {
                        _angleStart = when {
                            newDurationMinutes < _minDurationMinutes -> _angleEnd + simpleMinutesToAngle(
                                _minDurationMinutes,
                                _hourFormat
                            )
                            newDurationMinutes > _maxDurationMinutes -> _angleEnd + simpleMinutesToAngle(
                                _maxDurationMinutes,
                                _hourFormat
                            )
                            else -> newStartAngle
                        }
                    }
                } else if (_activeThumb == Thumb.END) {
                    val difference =
                        differenceBetweenAngles(_angleEnd, touchAngle) - _touchOffsetAngle
                    val newEndAngle = angleTo720(_angleEnd + difference)
                    val newDurationMinutes = durationBetweenMinutes(
                        angleToPreciseMinutes(_angleStart, _hourFormat),
                        angleToPreciseMinutes(newEndAngle, _hourFormat)
                    )

                    _angleEnd = when {
                        newDurationMinutes < _minDurationMinutes -> _angleStart - simpleMinutesToAngle(
                            _minDurationMinutes,
                            _hourFormat
                        )
                        newDurationMinutes > _maxDurationMinutes -> _angleStart - simpleMinutesToAngle(
                            _maxDurationMinutes,
                            _hourFormat
                        )
                        else -> newEndAngle
                    }
                }

                anglesChanged(_activeThumb!!)
                postInvalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                _angleStart = minutesToAngle(
                    startTimeMinutes,
                    _hourFormat
                )
                _angleEnd = minutesToAngle(
                    endTimeMinutes,
                    _hourFormat
                )

                updateGradient()
                postInvalidate()

                onDragChangeListener?.onDragStop(_activeThumb!!)
                _activeThumb = Thumb.NONE
                return true
            }
        }

        return false
    }

    private fun anglesChanged(thumb: Thumb) {
        updateGradient()

        if (onTimeChangeListener != null) {
            if (thumb == Thumb.START || thumb == Thumb.BOTH) {
                onTimeChangeListener?.onStartTimeChange(startTime)
            }
            if (thumb == Thumb.END || thumb == Thumb.BOTH) {
                onTimeChangeListener?.onEndTimeChange(endTime)
            }
            if (thumb == Thumb.START || thumb == Thumb.END) {
                onTimeChangeListener?.onDurationChange(duration)
            }
        }
    }

    private fun getClosestThumb(touchX: Float, touchY: Float): Thumb {
        val startThumb = getThumbPosition(
            angleTo360(_angleStart)
        )
        val endThumb = getThumbPosition(_angleEnd)
        val distanceFromMiddle =
            MathUtils.distanceBetweenPoints(_middlePoint.x, _middlePoint.y, touchX, touchY)
        if (MathUtils.isPointInCircle(
                touchX,
                touchY,
                endThumb.x,
                endThumb.y,
                _thumbSize * 2f
            )
        ) {
            return Thumb.END
        } else if (MathUtils.isPointInCircle(
                touchX,
                touchY,
                startThumb.x,
                startThumb.y,
                _thumbSize * 2f
            )
        ) {
            return Thumb.START
        } else if (distanceFromMiddle > _radius - _sliderWidth * 2 && distanceFromMiddle < _radius + _sliderWidth * 2) {
            return Thumb.BOTH
        }

        return Thumb.NONE
    }

    private fun getThumbPosition(
        angle: Float
    ): PointF {
        return PointF(
            (_middlePoint.x + _radius * cos(Math.toRadians(-angle.toDouble()))).toFloat(),
            (_middlePoint.y + _radius * sin(Math.toRadians(-angle.toDouble()))).toFloat()
        )
    }

    private fun parseTimeString(time: String): Time {
        if (!time.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]\$".toRegex())) {
            throw IllegalArgumentException("Format of time value '$time' is invalid, expected format hh:mm.")
        }

        val split = time.split(":")
        return Time(split[0].toInt(), split[1].toInt())
    }

    fun setOnTimeChangeListener(onTimeChangeListener: OnTimeChangeListener) {
        this.onTimeChangeListener = onTimeChangeListener
    }

    fun setOnDragChangeListener(onDragChangeListener: OnDragChangeListener) {
        this.onDragChangeListener = onDragChangeListener
    }

    // Time
    var hourFormat
        get() = _hourFormat
        set(value) {
            val prevFormat = _hourFormat

            _hourFormat = if (value == HourFormat.FORMAT_SYSTEM) {
                if (DateFormat.is24HourFormat(context)) HourFormat.FORMAT_24 else HourFormat.FORMAT_12
            } else {
                value
            }

            _angleStart = minutesToAngle(angleToMinutes(_angleStart, prevFormat), _hourFormat)
            _angleEnd = minutesToAngle(angleToMinutes(_angleEnd, prevFormat), _hourFormat)

            updateGradient()
            postInvalidate()
        }

    var startTime: Time
        get() = Time(
            startTimeMinutes
        )
        set(value) {
            _angleStart = minutesToAngle(value.totalMinutes, _hourFormat)
            postInvalidate()
        }

    var startTimeMinutes: Int
        get() = snapMinutes(
            angleToMinutes(_angleStart, _hourFormat), _timeStepMinutes
        )
        set(value) {
            _angleStart = minutesToAngle(value, _hourFormat)
            postInvalidate()
        }

    var endTime: Time
        get() = Time(
            endTimeMinutes
        )
        set(value) {
            _angleStart = minutesToAngle(value.totalMinutes, _hourFormat)
            postInvalidate()
        }

    var endTimeMinutes: Int
        get() = snapMinutes(
            angleToMinutes(_angleEnd, _hourFormat), _timeStepMinutes
        )
        set(value) {
            _angleEnd = minutesToAngle(value, _hourFormat)
            postInvalidate()
        }

    val duration: TimeDuration
        get() = TimeDuration(startTime, endTime)

    val durationMinutes: Int
        get() = duration.durationMinutes

    var minDuration: Time
        get() = Time(_minDurationMinutes)
        set(value) {
            minDurationMinutes = value.totalMinutes
        }

    var minDurationMinutes: Int
        get() = _minDurationMinutes
        set(value) {
            if (value < 0 || value > 24 * 60) {
                throw java.lang.IllegalArgumentException("Minimum duration has to be between 00:00 and 24:00");
            }

            if (value > _maxDurationMinutes) {
                throw IllegalArgumentException("Minimum duration cannot be greater than the maximum duration.");
            }

            _minDurationMinutes = value

            if (durationMinutes < _minDurationMinutes) {
                _angleEnd = minutesToAngle(
                    endTimeMinutes + abs(durationMinutes - _maxDurationMinutes),
                    _hourFormat
                )
                postInvalidate()
            }
        }

    var maxDuration: Time
        get() = Time(_maxDurationMinutes)
        set(value) {
            maxDurationMinutes = value.totalMinutes
        }

    var maxDurationMinutes: Int
        get() = _maxDurationMinutes
        set(value) {
            if (value < 0 || value > 24 * 60) {
                throw java.lang.IllegalArgumentException("Maximum duration has to be between 00:00 and 24:00");
            }

            if (value < _minDurationMinutes) {
                throw IllegalArgumentException("Maximum duration cannot be less than the minimum duration.");
            }

            _maxDurationMinutes = value

            if (durationMinutes > _maxDurationMinutes) {
                _angleEnd = minutesToAngle(
                    endTimeMinutes - abs(durationMinutes - _maxDurationMinutes),
                    _hourFormat
                )
                postInvalidate()
            }
        }

    var timeStepMinutes
        get() = _timeStepMinutes
        set(value) {
            if (value > 24 * 60) {
                throw IllegalArgumentException("Minutes per step cannot be above 24 hours (24 * 60).")
            }

            _timeStepMinutes = value
            postInvalidate()
        }

    // Slider
    var sliderWidth
        get() = _sliderWidth
        set(@ColorInt value) {
            _sliderWidth = value
            updatePaint()
        }

    var sliderColor
        @ColorInt
        get() = _sliderColor
        set(@ColorInt value) {
            _sliderColor = value
            updatePaint()
        }

    var sliderColorRes
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        get() = 0
        set(@ColorRes value) {
            sliderColor = ContextCompat.getColor(context, value)
        }

    var sliderRangeColor
        @ColorInt
        get() = _sliderRangeColor
        set(@ColorInt value) {
            _sliderRangeGradientStart = null
            _sliderRangeGradientEnd = null
            _sliderRangeColor = value
            updatePaint()
        }

    var sliderRangeColorRes
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        get() = 0
        set(@ColorRes value) {
            sliderRangeColor = ContextCompat.getColor(context, value)
        }

    var sliderRangeGradientStart
        @ColorInt
        get() = _sliderRangeGradientStart
        set(@ColorInt value) {
            _sliderRangeGradientStart = value
            updatePaint()
        }

    var sliderRangeGradientStartRes
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        get() = 0
        set(@ColorRes value) {
            sliderRangeGradientStart = ContextCompat.getColor(context, value)
        }

    var sliderRangeGradientMiddle
        @ColorInt
        get() = _sliderRangeGradientMiddle
        set(@ColorInt value) {
            _sliderRangeGradientMiddle = value
            updatePaint()
        }

    var sliderRangeGradientMiddleRes
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        get() = 0
        set(@ColorRes value) {
            sliderRangeGradientMiddle = ContextCompat.getColor(context, value)
        }

    var sliderRangeGradientEnd
        @ColorInt
        get() = _sliderRangeGradientEnd
        set(@ColorInt value) {
            _sliderRangeGradientEnd = value
            updatePaint()
        }

    var sliderRangeGradientEndRes
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        get() = 0
        set(@ColorRes value) {
            sliderRangeGradientEnd = ContextCompat.getColor(context, value)
        }

    // Thumb
    var thumbSize
        get() = _thumbSize
        set(@ColorInt value) {
            _thumbSize = value
            updatePaint()
        }

    var thumbSizeActiveGrow
        get() = _thumbSizeActiveGrow
        set(value) {
            _thumbSizeActiveGrow = value
            postInvalidate()
        }

    var thumbColor
        @ColorInt
        get() = _thumbColor
        set(@ColorInt value) {
            _thumbColor = value
            _thumbColorAuto = false
            updatePaint()
        }

    var thumbColorRes
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        get() = 0
        set(@ColorRes value) {
            thumbColor = ContextCompat.getColor(context, value)
        }

    var thumbColorAuto
        get() = _thumbColorAuto
        set(value) {
            _thumbColorAuto = value
            updatePaint()
        }

    var thumbIconStart
        get() = _thumbIconStart
        set(value) {
            _thumbIconStart = value?.mutate()
            updateThumbIconColors()
        }

    var thumbIconStartRes
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        get() = 0
        set(@DrawableRes value) {
            thumbIconStart = ContextCompat.getDrawable(context, value)
        }

    var thumbIconEnd
        get() = _thumbIconEnd
        set(value) {
            _thumbIconEnd = value?.mutate()
            updateThumbIconColors()
        }

    var thumbIconEndRes
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        get() = 0
        set(@DrawableRes value) {
            thumbIconEnd = ContextCompat.getDrawable(context, value)
        }

    var thumbIconSize
        get() = _thumbIconSize
        set(@ColorInt value) {
            _thumbIconSize = value
            postInvalidate()
        }

    var thumbIconColor
        @ColorInt
        get() = _thumbIconColor
        set(@ColorInt value) {
            _thumbIconColor = value
            updateThumbIconColors()
        }

    var thumbIconColorRes
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        get() = 0
        set(@ColorRes value) {
            thumbIconColor = ContextCompat.getColor(context, value)
        }

    // Clock
    var clockVisible
        get() = _clockVisible
        set(value) {
            _clockVisible = value
            postInvalidate()
        }

    var clockFace
        get() = _clockFace
        set(value) {
            _clockFace = value
            postInvalidate()
        }

    var clockTickColor
        @ColorInt
        get() = _clockTickColor
        set(@ColorInt value) {
            _clockTickColor = value
            _clockRenderer.updatePaint()
            postInvalidate()
        }

    var clockTickColorRes
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        get() = 0
        set(@ColorRes value) {
            clockTickColor = ContextCompat.getColor(context, value)
        }

    var clockLabelColor
        @ColorInt
        get() = _clockLabelColor
        set(@ColorInt value) {
            _clockLabelColor = value
            _clockRenderer.updatePaint()
            postInvalidate()
        }

    var clockLabelColorRes
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        get() = 0
        set(@ColorRes value) {
            clockLabelColor = ContextCompat.getColor(context, value)
        }

    var clockLabelSize
        @Dimension
        get() = _clockLabelSize
        set(@Dimension value) {
            _clockLabelSize = value
            _clockRenderer.updatePaint()
            postInvalidate()
        }

    open class Time(val totalMinutes: Int) {
        constructor(hr: Int, min: Int) : this(hr * 60 + min)

        val hour: Int
            get() = totalMinutes / 60 % 24
        val minute: Int
            get() = totalMinutes % 60

        val localTime: LocalTime
            @RequiresApi(Build.VERSION_CODES.O)
            get() = LocalTime.of(hour, minute)

        val calendar: Calendar
            get() = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
            }

        override fun toString(): String {
            return "$hour:${minute.toString().padStart(2, '0')}"
        }
    }

    open class TimeDuration(val start: Time, val end: Time) {
        val durationMinutes: Int
            get() {
                return if (start.totalMinutes > end.totalMinutes) {
                    24 * 60 - (start.totalMinutes - end.totalMinutes)
                } else {
                    end.totalMinutes - start.totalMinutes
                }
            }

        val hour: Int
            get() = durationMinutes / 60 % 24
        val minute: Int
            get() = durationMinutes % 60

        val duration: Duration
            @RequiresApi(Build.VERSION_CODES.O)
            get() = Duration.ofMinutes(durationMinutes.toLong())

        val classicDuration: javax.xml.datatype.Duration
            @RequiresApi(Build.VERSION_CODES.FROYO)
            get() =
                DatatypeFactory.newInstance().newDuration(true, 0, 0, 0, hour, minute, 0)

        override fun toString(): String {
            return "$hour:${minute.toString().padStart(2, '0')}"
        }
    }

    enum class Thumb {
        NONE, START, END, BOTH
    }

    enum class HourFormat(val id: Int) {
        FORMAT_SYSTEM(0),
        FORMAT_12(1),
        FORMAT_24(2);

        companion object {
            fun fromId(id: Int): HourFormat? {
                for (f in values()) {
                    if (f.id == id) return f
                }
                throw IllegalArgumentException()
            }
        }
    }

    enum class ClockFace(val id: Int) {
        APPLE(0),
        SAMSUNG(1);

        companion object {
            fun fromId(id: Int): ClockFace? {
                for (f in values()) {
                    if (f.id == id) return f
                }
                throw IllegalArgumentException()
            }
        }
    }

    interface OnTimeChangeListener {
        fun onStartTimeChange(startTime: Time)
        fun onEndTimeChange(endTime: Time)
        fun onDurationChange(duration: TimeDuration)
    }

    interface OnDragChangeListener {
        fun onDragStart(thumb: Thumb): Boolean
        fun onDragStop(thumb: Thumb)
    }
}