package nl.joery.timerangepicker.utils

import nl.joery.timerangepicker.ClockRenderer
import nl.joery.timerangepicker.TimeRangePicker

internal fun createClockRenderer(name: String, picker: TimeRangePicker): ClockRenderer {
    val c = Class.forName(name, true, TimeRangePicker::class.java.classLoader)

    // try to find only public with no arguments
    for (constructor in c.constructors) {
        val params = constructor.parameterTypes

        if(params.size == 1 && params[0] == TimeRangePicker::class.java) {
            val raw = constructor.newInstance(picker)

            try {
                return raw as ClockRenderer
            } catch (e: ClassCastException) {
                throw ClassCastException("Class '$name' is set as clock renderer but it does not extend '${TimeRangePicker::class.java.name}'")
            }
        }
    }

    throw RuntimeException("Clock renderer ($name) does not contain any public constructor with one parameter: ${TimeRangePicker::class.java.name}")
}