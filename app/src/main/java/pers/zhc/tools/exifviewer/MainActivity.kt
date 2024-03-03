package pers.zhc.tools.exifviewer

import android.os.Bundle
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.databinding.ExifViewerMainBinding
import pers.zhc.tools.jni.JNI

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bindings = ExifViewerMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        bindings.readBtn.setOnClickListener {
            val output = runCatching {
                val path = bindings.tiet.text.toString()
                JNI.Exif.getExifInfo(path)
            }.getOrElse {
                it.message
            }
            bindings.output.text = output
        }
    }
}
