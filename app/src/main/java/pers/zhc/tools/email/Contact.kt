package pers.zhc.tools.email

import android.os.Parcel
import android.os.Parcelable

/**
 * @author bczhc
 */
data class Contact(
    var name: String,
    var email: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!
    )

    fun set(name: String, email: String) {
        this.name = name
        this.email = email
    }

    fun set(contact: Contact) {
        set(contact.name, contact.email)
    }

    override fun toString(): String {
        return "$name <$email>"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(email)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Contact> {
        override fun createFromParcel(parcel: Parcel): Contact {
            return Contact(parcel)
        }

        override fun newArray(size: Int): Array<Contact?> {
            return arrayOfNulls(size)
        }
    }
}