package nl.joery.timerangepicker

import android.graphics.Canvas
import nl.joery.timerangepicker.utils.createClockRenderer
import org.junit.Assert
import org.junit.Test

class ReflectionUtilsTests {
    class ClockRenderer_NoPublicConstructor_NoInstance {
        private constructor()
        constructor(someArgs: Int)
    }

    class ClockRenderer_DoNotExtendClockRenderer
    class ClockRenderer_InstanceWithInvalidMods_1 private constructor() {
        companion object {
            @JvmField
            var INSTANCE = Any() // non-final for Java
        }
    }
    class ClockRenderer_InstanceWithInvalidMods_2 private constructor() {
        val INSTANCE = Any() // non-static for Java
    }
    class ClockRenderer_InstanceWithInvalidMods_3 private constructor() {
        @Volatile
        var INSTANCE = Any() // non-final, non-static and volatile is not expected here
    }

    class ClockRenderer_InstanceIsNull private constructor() {
        companion object {
            @JvmField
            val INSTANCE: Any? = null
        }
    }

    class ClockRenderer_InstanceDoNotExtendClockRenderer private constructor() {
        companion object {
            @JvmField
            val INSTANCE = Any()
        }
    }

    object ClockRenderer_Valid_GeneratedByKotlin: ClockRenderer {
        override fun render(canvas: Canvas, picker: TimeRangePicker, radius: Float) {
            throw NotImplementedError()
        }
    }

    class ClockRenderer_Valid_Manual private constructor() : ClockRenderer {
        override fun render(canvas: Canvas, picker: TimeRangePicker, radius: Float) {
            throw NotImplementedError()
        }

        companion object {
            @JvmField
            val INSTANCE = ClockRenderer_Valid_Manual()
        }
    }

    private inline fun<reified T : Any, reified TException : Exception> createClockRendererThrowsWhen() {
        try {
            val className = T::class.java.name
            createClockRenderer(className)
            Assert.fail("Exception wasn't thrown")
        } catch (e: Exception) {
            val eClass = e.javaClass
            if(eClass !== TException::class.java) {
                Assert.fail("Throws but exception class is $eClass, but expected ${TException::class.java}")
            }
        }
    }

    @Test
    fun createClockRenderer_throwsWhenInvalidClassNameGiven() {
        try {
            createClockRenderer("123")
            Assert.fail("Exception wasn't thrown")
        } catch (e: ClassNotFoundException) {
        } catch (e: Exception) {
            Assert.fail("ClassCastException was expected to be thrown. Actual: ${e.javaClass.name}")
        }
    }

    @Test
    fun createClockRenderer_throwsWhenNoPublicConstructor_NoInstance() {
        createClockRendererThrowsWhen<ClockRenderer_NoPublicConstructor_NoInstance, RuntimeException>()
    }

    @Test
    fun createClockRenderer_throwsWhenDoNotExtendClockRenderer() {
        createClockRendererThrowsWhen<ClockRenderer_DoNotExtendClockRenderer, ClassCastException>()
    }

    @Test
    fun createClockRenderer_throwsWhenInstanceWithInvalidMods() {
        createClockRendererThrowsWhen<ClockRenderer_InstanceWithInvalidMods_1, RuntimeException>()
        createClockRendererThrowsWhen<ClockRenderer_InstanceWithInvalidMods_2, RuntimeException>()
        createClockRendererThrowsWhen<ClockRenderer_InstanceWithInvalidMods_3, RuntimeException>()
    }

    @Test
    fun createClockRenderer_throwsWhenInstanceIsNull() {
        createClockRendererThrowsWhen<ClockRenderer_InstanceIsNull, NullPointerException>()
    }

    @Test
    fun createClockRenderer_throwsWhenInstanceDoNotExtendClockRenderer() {
        createClockRendererThrowsWhen<ClockRenderer_InstanceDoNotExtendClockRenderer, ClassCastException>()
    }

    @Test
    fun createClockRenderer_successWhenRequirementsAreMet() {
        createClockRenderer(ClockRenderer_Valid_GeneratedByKotlin::class.java.name)
        createClockRenderer(ClockRenderer_Valid_Manual::class.java.name)
    }
}