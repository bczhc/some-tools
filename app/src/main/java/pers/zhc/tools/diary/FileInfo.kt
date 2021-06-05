package pers.zhc.tools.diary

import android.os.Parcel
import android.os.Parcelable

class FileInfo(
    val content: String,
    val additionTimestamp: Long,
    val storageTypeEnumInt: Int,
    val description: String,
    val identifier: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readLong(),
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest!!.apply {
            writeString(content)
            writeLong(additionTimestamp)
            writeInt(storageTypeEnumInt)
            writeString(description)
            writeString(identifier)
        }
    }

    companion object CREATOR : Parcelable.Creator<FileInfo> {
        override fun createFromParcel(parcel: Parcel): FileInfo {
            return FileInfo(parcel)
        }

        override fun newArray(size: Int): Array<FileInfo?> {
            return arrayOfNulls(size)
        }
    }
}