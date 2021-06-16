package nl.joery.timerangepicker

import org.junit.Assert
import org.junit.Test

class TimeRangePickerTests {
    @Test
    fun time_parse_test() {
        fun testCase(timeString: String, expectedHour: Int, expectedMinute: Int) {
            val time = TimeRangePicker.Time.parse(timeString)

            Assert.assertEquals(expectedHour, time.hour)
            Assert.assertEquals(expectedMinute, time.minute)
        }

        testCase("00:00", 0, 0)
        testCase("10:45", 10, 45)
        testCase("02:06", 2, 6)
        testCase("23:59", 23, 59)
        testCase("20:40", 20, 40)
    }
}