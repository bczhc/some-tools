package pers.zhc.tools.tasknotes

import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.databinding.TaskNotesRecordTakingPromptDialogBinding
import pers.zhc.tools.utils.setNegativeAction
import pers.zhc.tools.utils.setPositiveAction
import pers.zhc.tools.R
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.unreachable

class DialogShowActivity : BaseActivity() {
    private val database = Database.database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showDialog()
    }

    private fun showDialog() {
        val bindings = TaskNotesRecordTakingPromptDialogBinding.inflate(layoutInflater)
        bindings.start.isChecked = true
        val markToggleGroup = bindings.btg

        MaterialAlertDialogBuilder(this)
            .setView(bindings.root)
            .setPositiveAction { _, _ ->
                val taskMark = when (markToggleGroup.checkedButtonId) {
                    R.id.start -> TaskMark.START
                    R.id.end -> TaskMark.END
                    else -> unreachable()
                }
                database.insert(
                    Record(
                        bindings.descriptionEt.text.toString(),
                        taskMark,
                        System.currentTimeMillis()
                    )
                )
                ToastUtils.show(this, R.string.adding_succeeded)
                finish()
            }.setNegativeAction { _, _ ->
                finish()
            }
            .setOnDismissListener {
                finish()
            }
            .setCancelable(true)
            .setOnCancelListener {
                finish()
            }
            .show()
    }
}