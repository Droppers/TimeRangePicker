package nl.joery.timerangepicker.utils

import nl.joery.timerangepicker.ClockRenderer
import nl.joery.timerangepicker.TimeRangePicker
import java.lang.reflect.Field
import java.lang.reflect.Modifier

private const val KSINGLETION_EXPECTED_MODS =
    Modifier.PUBLIC or Modifier.STATIC or Modifier.FINAL

internal fun createClockRenderer(name: String): ClockRenderer {
    val c = Class.forName(name, true, TimeRangePicker::class.java.classLoader)

    // try to find only public with no arguments
    for (constructor in c.constructors) {
        if (constructor.parameterTypes.isEmpty()) {
            val raw = constructor.newInstance()
            val renderer: ClockRenderer
            try {
                renderer = raw as ClockRenderer
            } catch (e: ClassCastException) {
                throw ClassCastException("Class '$name' is set as clock renderer but it does not extend '${TimeRangePicker::class.java.name}'")
            }

            return renderer
        }
    }

    // maybe it's singleton
    val instanceField: Field
    try {
        instanceField = c.getField("INSTANCE")
    } catch (e: NoSuchFieldException) {
        throw RuntimeException("Clock renderer ($name) does not contain any public constructor with no parameters or INSTANCE field")
    }

    val mods = instanceField.modifiers
    if ((mods and KSINGLETION_EXPECTED_MODS) != KSINGLETION_EXPECTED_MODS) {
        // we checked whether class contains constructors with no parameters
        // so programmer may expect that library picks value from INSTANCE field,
        // which is typical for singletons.
        // The field has invalid modifiers and we should inform that something is wrong
        // with the field.
        throw RuntimeException("Field INSTANCE has invalid modifiers (expected: public static final)")
    }

    val instanceRaw: Any = instanceField.get(null) ?: throw NullPointerException("Field INSTANCE in '$name' is null")

    val renderer: ClockRenderer
    try {
        renderer = instanceRaw as ClockRenderer
    } catch(e: ClassCastException) {
        throw ClassCastException("Field INSTANCE in '$name' does not extend ${TimeRangePicker::class.java.name}")
    }

    return renderer
}