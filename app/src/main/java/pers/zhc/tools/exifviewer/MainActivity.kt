package pers.zhc.tools.exifviewer

import android.content.Intent
import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.databinding.ExifViewerMainBinding
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.nullMap

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bindings = ExifViewerMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val updateResult = {
            val output = runCatching {
                val path = bindings.tiet.text.toString()
                JNI.Exif.getExifInfo(path)
            }.getOrElse {
                it.message
            }
            bindings.output.text = output
        }

        // launched from the "Open as" dialog
        if (intent.action == Intent.ACTION_VIEW) {
            // TODO: workaround; should use content provider but not direct path
            intent.data.nullMap { it.path }?.let {path ->
                bindings.tiet.setText(path)
                updateResult()
            }
        }

        bindings.readBtn.setOnClickListener {
            updateResult()
        }
    }
}
