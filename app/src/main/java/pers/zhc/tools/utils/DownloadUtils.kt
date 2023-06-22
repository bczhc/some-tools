package pers.zhc.tools.utils

import android.content.Context
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pers.zhc.tools.MyApplication.Companion.HTTP_CLIENT_DOWNLOAD
import pers.zhc.tools.R
import java.io.File
import java.io.OutputStream

object DownloadUtils {
    suspend fun download(url: Url, out: OutputStream, progress: suspend (length: Long, totalLength: Long?) -> Unit) {
        HTTP_CLIENT_DOWNLOAD.prepareGet(url).execute {
            val contentLength = it.contentLength()

            var offset = 0

            it.bytesFlow().collect { chunk ->
                offset += chunk.size
                withContext(Dispatchers.IO) {
                    out.write(chunk, 0, chunk.size)
                }
                progress(offset.toLong(), contentLength)
            }
        }
    }

    suspend fun startDownloadWithDialog(context: Context, url: Url, file: File) {
        val progressDialog = ProgressDialog(context).apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }.also { it.show() }
        val progressView = progressDialog.getProgressView().apply {
            setTitle(context.getString(R.string.downloading))
            setProgress(0F)
        }

        val writer = file.outputStream().buffered()

        withContext(Dispatchers.IO) {
            download(url, writer) { length, totalLength ->
                withContext(Dispatchers.Main) {
                    if (totalLength == null) {
                        if (!progressView.isIndeterminateMode) {
                            progressView.isIndeterminateMode = true
                        }
                    } else {
                        if (progressView.isIndeterminateMode) {
                            progressView.isIndeterminateMode = false
                        }
                        progressView.setProgressAndText(length.toFloat() / totalLength.toFloat())
                    }
                }
            }
            writer.flush()
            writer.close()
        }

        withContext(Dispatchers.Main) {
            progressDialog.dismiss()
        }
    }
}
