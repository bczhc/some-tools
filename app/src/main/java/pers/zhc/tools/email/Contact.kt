package pers.zhc.tools.email

/**
 * @author bczhc
 */
data class Contact(
    var name: String,
    var email: String
) {
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
}