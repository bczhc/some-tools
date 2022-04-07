package pers.zhc.tools.filebrowser

import android.graphics.BitmapFactory
import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.views.ZoomableImageView

/**
 * @author bczhc
 */
class ImageFileBrowser : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        if (!intent.hasExtra(EXTRA_PATH)) {
            return
        }
        val path = intent.getStringExtra(EXTRA_PATH)!!

        val bitmap = BitmapFactory.decodeFile(path)
        if (bitmap == null) {
            ToastUtils.show(this, R.string.diary_file_browser_open_file_failed)
            return
        }

        val imageView = ZoomableImageView(this).apply {
            setBitmap(bitmap)
        }
        setContentView(imageView)
    }

    companion object {
        const val EXTRA_PATH = "path"
    }
}