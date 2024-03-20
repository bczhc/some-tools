package pers.zhc.tools.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.InputStream

class OpenAsResult(
    private val context: Context,
    val uri: Uri,
) {
    fun openInputStream(): InputStream? {
        return context.contentResolver.openInputStream(uri)
    }
}

fun Activity.checkFromOpenAs(): OpenAsResult? {
    val intent = this.intent
    if (intent.action == Intent.ACTION_VIEW && intent?.data != null) {
        val uri = intent.data!!
        return OpenAsResult(
            context = this,
            uri = uri,
        )
    }
    return null
}
