package pers.zhc.tools.tasknotes

import android.content.Context
import android.view.LayoutInflater
import android.view.WindowManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pers.zhc.tools.R
import pers.zhc.tools.databinding.TaskNotesRecordTakingPromptDialogBinding
import pers.zhc.tools.utils.setNegativeAction
import pers.zhc.tools.utils.setPositiveAction
import pers.zhc.tools.utils.unreachable
import java.util.*

object Dialog {
    fun createRecordAddingDialog(context: Context, description: String = "", onFinished: (record: Record?) -> Unit) {
        val bindings = TaskNotesRecordTakingPromptDialogBinding.inflate(LayoutInflater.from(context))
        val descriptionET = bindings.descriptionEt.also { it.setText(description) }
        bindings.start.isChecked = true
        val markToggleGroup = bindings.btg

        MaterialAlertDialogBuilder(context)
            .setView(bindings.root)
            .setPositiveAction { _, _ ->
                val taskMark = when (markToggleGroup.checkedButtonId) {
                    R.id.start -> TaskMark.START
                    R.id.end -> TaskMark.END
                    else -> unreachable()
                }
                val creationTime = System.currentTimeMillis()
                val record = Record(
                    descriptionET.text.toString(),
                    taskMark,
                    Time(Date(creationTime)),
                    creationTime
                )
                onFinished(record)
            }.setNegativeAction { _, _ ->
                onFinished(null)
            }
            .setOnDismissListener {
                onFinished(null)
            }
            .setCancelable(true)
            .setOnCancelListener {
                onFinished(null)
            }
            .show()
            .also {
                val window = it.window
                window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            }

        bindings.descriptionEt.requestFocus()
    }
}
