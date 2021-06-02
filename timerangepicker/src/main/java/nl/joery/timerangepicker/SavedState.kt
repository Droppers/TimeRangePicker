package nl.joery.timerangepicker

import android.os.Parcel
import android.os.Parcelable
import android.view.View

internal class SavedState : View.BaseSavedState {
    var angleStart: Float = 0f
    var angleEnd: Float = 0f

    constructor(source: Parcel) : super(source) {
        angleStart = source.readFloat()
        angleEnd = source.readFloat()
    }

    constructor(superState: Parcelable?) : super(superState)

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeFloat(angleStart)
        out.writeFloat(angleEnd)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel): SavedState {
                return SavedState(source)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }
}