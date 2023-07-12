package pers.zhc.tools.crashhandler

import android.content.Context
import android.content.Intent
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.format
import pers.zhc.tools.utils.requireMkdirs
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.system.exitProcess

/**
 * @author bczhc
 */
class CrashHandler private constructor(private val context: Context) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
        val stackTraceString = getExceptionStackTraceString(t, e)
        val currentTimeMillis = System.currentTimeMillis()
        val dateString = Date(currentTimeMillis).format("yyyy-MM-dd_HH-mm-ss", Locale.ENGLISH)
        val filename = "crash_${dateString}_$currentTimeMillis.txt"

        val intent = Intent(context, CrashReportActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(CrashReportActivity.EXTRA_EXCEPTION_TEXT, stackTraceString)
            putExtra(CrashReportActivity.EXTRA_FILENAME, filename)
        }
        context.startActivity(intent)
        exitProcess(1)
    }

    private fun getExceptionStackTraceString(t: Thread, e: Throwable): String {
        val sb = StringBuilder()
        getExceptionStackTraceString(sb, t, e)
        return sb.toString()
    }

    private fun getExceptionStackTraceString(sb: StringBuilder, t: Thread, e: Throwable) {
        val ses = e.stackTrace
        sb.appendLine("""Exception in thread "${t.name}" $e""")
        for (se in ses) {
            sb.appendLine("\tat $se")
        }
        val ec = e.cause
        ec?.let { getExceptionStackTraceString(sb, t, it) }
    }

    companion object {
        fun install(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context))
        }

        @Throws(IOException::class)
        fun save2File(context: Context, filename: String, content: String) {
            val crashDir = File(Common.getAppMainExternalStoragePath(context) + File.separatorChar + "crash")
            if (!crashDir.exists()) {
                runCatching { crashDir.requireMkdirs() }
            }
            val file = File(crashDir, filename)

            file.writer().use { it.write(content) }
        }
    }
}
