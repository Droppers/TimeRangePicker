package nl.joery.timerangepicker

interface BitmapCachedClockRenderer: ClockRenderer {
    var isBitmapCacheEnabled: Boolean

    fun invalidateBitmapCache()
    fun recycleBitmapCache()
}