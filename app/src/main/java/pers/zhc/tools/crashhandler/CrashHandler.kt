package pers.zhc.tools.crashhandler

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import pers.zhc.tools.utils.Common
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author bczhc
 */
class CrashHandler private constructor(private val ctx: Context) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
        val intent = Intent()
        val stackTraceString = getExceptionStackTraceString(t, e)
        intent.setClass(ctx, CrashReportActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val currentTimeMillis = System.currentTimeMillis()
        val date = Date(currentTimeMillis)
        @SuppressLint("SimpleDateFormat") val dateString = SimpleDateFormat("yy-MM-dd-HH-mm-ss").format(date)
        val filename = "crash-$dateString-$currentTimeMillis.txt"
        intent.putExtra("exception", stackTraceString)
        intent.putExtra("filename", filename)
        ctx.startActivity(intent)
        System.exit(1)
    }

    private fun getExceptionStackTraceString(t: Thread, e: Throwable): String {
        val sb = StringBuilder()
        getExceptionStackTraceString(sb, t, e)
        return sb.toString()
    }

    private fun getExceptionStackTraceString(sb: StringBuilder, t: Thread, e: Throwable) {
        val ses = e.stackTrace
        sb.append("Exception in thread \"").append(t.name).append("\" ").append(e).append('\n')
        for (se in ses) {
            sb.append("\tat ").append(se).append('\n')
        }
        val ec = e.cause
        ec?.let { getExceptionStackTraceString(sb, t, it) }
    }

    companion object {
        fun install(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context))
        }

        fun save2File(ctx: Context?, filename: String?, content: String) {
            val crashDir = File(Common.getAppMainExternalStoragePath(ctx) + File.separatorChar + "crash")
            if (!crashDir.exists()) {
                Log.d(CrashHandler::class.java.name, "save2File: " + crashDir.mkdirs())
            }
            val file = File(crashDir, filename)
            println("crashDir.getPath() = " + crashDir.path)
            println("file.getPath() = " + file.path)
            var os: OutputStream? = null
            try {
                os = FileOutputStream(file, false)
                os.write(content.toByteArray(charset("UTF-8")))
                os.flush()
                os.close()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                if (os != null) {
                    try {
                        os.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
