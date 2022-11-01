package pers.zhc.tools.test

import android.content.DialogInterface
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pers.zhc.tools.BaseActivity

/**
 * @author bczhc
 */
class Demo : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MaterialAlertDialogBuilder(this)
            .setTitle("Title")
            .setSingleChoiceItems(('A'..'Z').map { it.toString() }.toTypedArray(), 1) { _, _ ->

            }
            .setPositiveButton("OK") { _, _ ->

            }
            .show()
    }
}