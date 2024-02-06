package pers.zhc.tools.charucd

import android.os.Bundle
import pers.zhc.jni.JNI.Struct.packInt
import pers.zhc.jni.JNI.Struct.packShort
import pers.zhc.jni.struct.Struct
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.MyApplication
import pers.zhc.tools.R
import pers.zhc.tools.databinding.CharUcdActivityBinding
import pers.zhc.tools.databinding.CharUcdTableRowBinding
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.test.UnicodeTable
import pers.zhc.tools.utils.CharUtils
import pers.zhc.tools.utils.ClipboardUtils
import pers.zhc.tools.utils.thread
import java.util.*

/**
 * @author bczhc
 */
class CharUcdActivity : BaseActivity() {
    private lateinit var bindings: CharUcdActivityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        if (!intent.hasExtra(EXTRA_CODEPOINT)) {
            return
        }
        val codepoint = intent.getIntExtra(EXTRA_CODEPOINT, 0)

        bindings = CharUcdActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        EncodingsTextViews(this).apply {
            runCatching {
                utf8.text = getUtf8String(codepoint)
                utf16.text = getUtf16String(codepoint)
                utf16le.text = getUtf16BytesString(codepoint, Struct.Endianness.LITTLE)
                utf16be.text = getUtf16BytesString(codepoint, Struct.Endianness.BIG)
                utf32.text = getUtf32String(codepoint)
                utf32le.text = getUtf32BytesString(codepoint, Struct.Endianness.LITTLE)
                utf32be.text = getUtf32BytesString(codepoint, Struct.Endianness.BIG)
            }
        }

        runCatching {
            val charString = JNI.Unicode.Codepoint.codepoint2str(codepoint)
            bindings.charTv.text = charString
            bindings.charTv.setOnLongClickListener {
                ClipboardUtils.putWithToast(this, charString)
                true
            }
        }
        val ucdTL = bindings.ucdPropTl
        bindings.unicodeTv.text =
            getString(R.string.char_ucd_unicode_codepoint_tv, UnicodeTable.codepoint2unicodeStr(codepoint))
        bindings.decimalTv.text = getString(R.string.char_ucd_unicode_decimal_tv, codepoint)

        val ucdContentPlaceholder = bindings.ucdContentPlaceholder
        ucdContentPlaceholder.text = getString(
            if (UcdDatabase.databaseFile.exists()) {
                R.string.char_ucd_querying_msg_text
            } else {
                R.string.char_ucd_database_missing_msg
            }
        )

        thread {
            if (!UcdDatabase.databaseFile.exists()) return@thread

            val properties = UcdDatabase.useDatabase {
                it.query(codepoint)
            }

            if (properties == null) {
                ucdContentPlaceholder.text = getString(R.string.char_ucd_ucd_properties_not_found_msg)
                return@thread
            }

            runOnUiThread {
                ucdTL.removeAllViews()
                for (entry in properties) {
                    val key = entry[0]
                    val value = entry[1]

                    val valueString = if (key != "alias") {
                        value
                    } else {
                        val aliasArray = MyApplication.GSON.fromJson(value, Array<String>::class.java)
                        aliasArray.joinToString()
                    }

                    val bindings = CharUcdTableRowBinding.inflate(layoutInflater)
                    bindings.keyTv.text = key
                    bindings.valueTv.text = valueString
                    ucdTL.addView(bindings.root)
                }
            }
        }
    }

    companion object {
        /**
         * Int intent extra
         */
        const val EXTRA_CODEPOINT = "codepoint"
    }

    private class EncodingsTextViews(activity: CharUcdActivity) {
        val utf8 = activity.bindings.encUtf8Tv
        val utf16 = activity.bindings.encUtf16Tv
        val utf32 = activity.bindings.encUtf32Tv
        val utf16le = activity.bindings.encUtf16leTv
        val utf16be = activity.bindings.encUtf16beTv
        val utf32le = activity.bindings.encUtf32leTv
        val utf32be = activity.bindings.encUtf32beTv
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
