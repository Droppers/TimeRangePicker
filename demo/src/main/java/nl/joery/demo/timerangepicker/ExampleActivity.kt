package nl.joery.demo.timerangepicker

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_example.*
import nl.joery.animatedbottombar.AnimatedBottomBar
import nl.joery.timerangepicker.TimeRangePicker

class ExampleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)

        setStyle(R.id.tab_one)
        bottom_bar.setOnTabSelectListener(object: AnimatedBottomBar.OnTabSelectListener {
            override fun onTabSelected(
                lastIndex: Int,
                lastTab: AnimatedBottomBar.Tab?,
                newIndex: Int,
                newTab: AnimatedBottomBar.Tab
            ) {
                setStyle(newTab.id)
            }
        })

        updateTimes()
        updateDuration()

        picker.setOnTimeChangeListener(object : TimeRangePicker.OnTimeChangeListener {
            override fun onStartTimeChange(startTime: TimeRangePicker.Time) {
                updateTimes()
            }

            override fun onEndTimeChange(endTime: TimeRangePicker.Time) {
                updateTimes()
            }

            override fun onDurationChange(duration: TimeRangePicker.TimeDuration) {
                updateDuration()
            }
        })

        picker.setOnDragChangeListener(object : TimeRangePicker.OnDragChangeListener {
            override fun onDragStart(thumb: TimeRangePicker.Thumb): Boolean {
                if(thumb != TimeRangePicker.Thumb.BOTH) {
                    animate(thumb, true)
                }
                return true
            }

            override fun onDragStop(thumb: TimeRangePicker.Thumb) {
                if(thumb != TimeRangePicker.Thumb.BOTH) {
                    animate(thumb, false)
                }

                Log.d(
                    "TimeRangePicker",
                    "Start time: " + picker.startTime
                )
                Log.d(
                    "TimeRangePicker",
                    "End time: " + picker.endTime
                )
                Log.d(
                    "TimeRangePicker",
                    "Total duration: " + picker.duration
                )
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun updateTimes() {
        end_time.text = picker.endTime.toString()
        start_time.text = picker.startTime.toString()
    }

    private fun updateDuration() {
        duration.text = getString(R.string.duration, picker.duration)
    }

    private fun animate(thumb: TimeRangePicker.Thumb, active: Boolean) {
        val activeView = if(thumb == TimeRangePicker.Thumb.START) bedtime_layout else wake_layout
        val inactiveView = if(thumb == TimeRangePicker.Thumb.START) wake_layout else bedtime_layout
        val direction = if(thumb == TimeRangePicker.Thumb.START) 1 else -1

        activeView
            .animate()
            .translationY(if(active) (activeView.measuredHeight / 2f)*direction else 0f)
            .setDuration(300)
            .start()
        inactiveView
            .animate()
            .alpha(if(active) 0f else 1f)
            .setDuration(300)
            .start()
    }

    private fun setStyle(id: Int) {
        when(id) {
            R.id.tab_one -> {
                picker.thumbColorAuto = true
                picker.thumbSize = 28.px
                picker.sliderWidth = 8.px
                picker.sliderColor = Color.rgb(238, 238, 236)
                picker.thumbIconColor = Color.WHITE
                picker.thumbSizeActiveGrow = 1.2f
                picker.sliderRangeGradientStart = Color.parseColor("#8287fe")
                picker.sliderRangeGradientMiddle = Color.parseColor("#b67cc8")
                picker.sliderRangeGradientEnd = Color.parseColor("#ffa301")
                picker.clockFace = TimeRangePicker.ClockFace.SAMSUNG
                picker.hourFormat = TimeRangePicker.HourFormat.FORMAT_24
            }
            R.id.tab_two -> {
                picker.thumbSize = 36.px
                picker.sliderWidth = 40.px
                picker.sliderColor = Color.TRANSPARENT
                picker.thumbColor = Color.WHITE
                picker.sliderRangeGradientStart = Color.parseColor("#F79104")
                picker.sliderRangeGradientMiddle = TimeRangePicker.COLOR_NONE
                picker.sliderRangeGradientEnd = Color.parseColor("#F8C207")
                picker.thumbIconColor = Color.parseColor("#F79104")
                picker.thumbSizeActiveGrow = 1.0f
                picker.clockFace = TimeRangePicker.ClockFace.APPLE
                picker.hourFormat = TimeRangePicker.HourFormat.FORMAT_12
            }
            R.id.tab_three -> {
                picker.thumbSize = 32.px
                picker.sliderWidth = 32.px
                picker.sliderColor = Color.rgb(233, 233, 233)
                picker.thumbColor = Color.TRANSPARENT
                picker.thumbIconColor = Color.WHITE
                picker.sliderRangeGradientStart = Color.parseColor("#5663de")
                picker.sliderRangeGradientMiddle = TimeRangePicker.COLOR_NONE
                picker.sliderRangeGradientEnd = Color.parseColor("#6d7bff")
                picker.thumbSizeActiveGrow = 1.0f
                picker.clockFace = TimeRangePicker.ClockFace.SAMSUNG
                picker.hourFormat = TimeRangePicker.HourFormat.FORMAT_12
            }
        }
    }

    private val Int.px: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()
}