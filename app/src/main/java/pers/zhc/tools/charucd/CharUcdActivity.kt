package pers.zhc.tools.charucd

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.char_ucd_activity.*
import kotlinx.android.synthetic.main.char_ucd_table_row.view.*
import org.json.JSONArray
import pers.zhc.jni.JNI.Struct.packInt
import pers.zhc.jni.JNI.Struct.packShort
import pers.zhc.jni.struct.Struct
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.test.UnicodeTable
import pers.zhc.tools.utils.CharUtils
import pers.zhc.util.Assertion
import java.util.*

/**
 * @author bczhc
 */
class CharUcdActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        if (!intent.hasExtra(EXTRA_CODEPOINT)) {
            return
        }
        val codepoint = intent.getIntExtra(EXTRA_CODEPOINT, 0)

        setContentView(R.layout.char_ucd_activity)

        EncodingsTextViews(this).apply {
            utf8.text = getUtf8String(codepoint)
            utf16.text = getUtf16String(codepoint)
            utf16le.text = getUtf16BytesString(codepoint, Struct.Endianness.LITTLE)
            utf16be.text = getUtf16BytesString(codepoint, Struct.Endianness.BIG)
            utf32.text = getUtf32String(codepoint)
            utf32le.text = getUtf32BytesString(codepoint, Struct.Endianness.LITTLE)
            utf32be.text = getUtf32BytesString(codepoint, Struct.Endianness.BIG)
        }

        val ucdTL = ucd_prop_tl!!
        char_tv.text = JNI.Utf8.codepoint2str(codepoint)
        unicode_tv!!.text =
            getString(R.string.char_ucd_unicode_codepoint_tv, UnicodeTable.codepoint2unicodeStr(codepoint))
        decimal_tv!!.text = getString(R.string.char_ucd_unicode_decimal_tv, codepoint)

        val ucdContentPlaceholder = ucd_content_placeholder!!
        ucdContentPlaceholder.text = getString(
            if (CharLookupActivity.UCD_DATABASE_PATH.exists()) {
                R.string.char_ucd_querying_msg_text
            } else {
                R.string.char_ucd_database_missing_msg
            }
        )

        Thread {
            val ucdDatabasePath = CharLookupActivity.UCD_DATABASE_PATH
            if (!ucdDatabasePath.exists()) return@Thread

            val database = UcdDatabase(ucdDatabasePath)
            val properties = database.query(codepoint)
            database.close()

            if (properties == null) {
                ucdContentPlaceholder.text = getString(R.string.char_ucd_ucd_properties_not_found_msg)
                return@Thread
            }

            runOnUiThread {
                ucdTL.removeAllViews()
                for (i in 0 until properties.length()) {
                    val obj = properties.getJSONObject(i)
                    val key = run {
                        Assertion.doAssertion(obj.length() == 1)
                        obj.keys().next()!!
                    }
                    val value = obj.get(key)
                    val valueString = if (key != "alias") {
                        value as String
                    } else {
                        val aliasArray = value as JSONArray
                        aliasArray.join(", ")
                    }

                    val inflate = View.inflate(this, R.layout.char_ucd_table_row, null)
                    inflate.key_tv!!.text = key
                    inflate.value_tv!!.text = valueString
                    ucdTL.addView(inflate)
                }
            }
        }.start()
    }

    companion object {
        /**
         * Int intent extra
         */
        const val EXTRA_CODEPOINT = "codepoint"
    }

    private class EncodingsTextViews(activity: CharUcdActivity) {
        val utf8 = activity.enc_utf8_tv!!
        val utf16 = activity.enc_utf16_tv!!
        val utf32 = activity.enc_utf32_tv!!
        val utf16le = activity.enc_utf16le_tv!!
        val utf16be = activity.enc_utf16be_tv!!
        val utf32le = activity.enc_utf32le_tv!!
        val utf32be = activity.enc_utf32be_tv!!
    }

    private fun completeString(s: String, length: Int): String {
        return if (s.length < length) {
            "${"0".repeat(length - s.length)}$s"
        } else s
    }

    private fun getCompletedUppercaseString(s: String, length: Int): String {
        return completeString(s, length).uppercase(Locale.US)
    }

    private fun String.getCompletedUppercase(length: Int): String {
        return getCompletedUppercaseString(this, length)
    }

    private fun getUtf8String(codepoint: Int): String {
        return CharUtils.getEncodedUtf8(codepoint).joinToString(" ") {
            it.toUByte().toString(16).getCompletedUppercase(2)
        }
    }

    private fun getUtf16String(codepoint: Int): String {
        return CharUtils.getEncodedUtf16(codepoint).joinToString(" ") {
            it.toUShort().toString(16).getCompletedUppercase(4)
        }
    }

    private fun getUtf32String(codepoint: Int): String {
        return codepoint.toUInt().toString(16).getCompletedUppercase(8)
    }

    private fun Short.pack(endianness: Struct.Endianness): ByteArray {
        val buf = ByteArray(Short.SIZE_BYTES)
        packShort(this, buf, 0, endianness.ordinal)
        return buf
    }

    private fun Int.pack(endianness: Struct.Endianness): ByteArray {
        val buf = ByteArray(Int.SIZE_BYTES)
        packInt(this, buf, 0, endianness.ordinal)
        return buf
    }

    private fun getUtf16BytesString(codepoint: Int, endianness: Struct.Endianness): String {
        return CharUtils.getEncodedUtf16(codepoint).joinToString(" ") {
            it.pack(endianness).joinToString(" ") { b ->
                b.toUByte().toString(16).getCompletedUppercase(2)
            }
        }
    }

    private fun getUtf32BytesString(codepoint: Int, endianness: Struct.Endianness): String {
        return codepoint.pack(endianness).joinToString(" ") {
            it.toUByte().toString(16).getCompletedUppercase(2)
        }
    }
}
