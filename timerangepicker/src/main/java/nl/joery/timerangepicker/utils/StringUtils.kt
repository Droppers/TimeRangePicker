package nl.joery.timerangepicker.utils

internal fun String.parsePositiveInt(start: Int, end: Int): Int {
    var result = 0
    for(i in start until end) {
        val c = this[i].code

        if(c < '0'.code || c > '9'.code) {
            throw IllegalArgumentException("String has invalid format (Illegal character '$c')")
        }

        result = result * 10 + (c - '0'.code)
    }

    return result
}