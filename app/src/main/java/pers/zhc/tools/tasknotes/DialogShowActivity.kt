package pers.zhc.tools.tasknotes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.databinding.TaskNotesRecordTakingPromptDialogBinding
import pers.zhc.tools.R
import pers.zhc.tools.utils.*
import java.util.*

class DialogShowActivity : BaseActivity() {
    private val database = Database.database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showDialog()
    }

    private fun showDialog() {
        val bindings = TaskNotesRecordTakingPromptDialogBinding.inflate(layoutInflater)
        val descriptionET = bindings.descriptionEt
        bindings.start.isChecked = true
        val markToggleGroup = bindings.btg

        val intent = intent
        intent.getStringExtra(EXTRA_DESCRIPTION_TEXT)?.let { descriptionET.setText(it) }

        MaterialAlertDialogBuilder(this)
            .setView(bindings.root)
            .setPositiveAction { _, _ ->
                val taskMark = when (markToggleGroup.checkedButtonId) {
                    R.id.start -> TaskMark.START
                    R.id.end -> TaskMark.END
                    else -> unreachable()
                }
                val creationTime = System.currentTimeMillis()
                database.insert(
                    Record(
                        descriptionET.text.toString(),
                        taskMark,
                        Time(Date(creationTime)),
                        creationTime
                    )
                )
                ToastUtils.show(this, R.string.adding_succeeded)
                setResult(0, Intent().apply {
                    putExtra(EXTRA_TIMESTAMP, creationTime)
                })
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

    /**
     * I: nullable default task description
     * O: timestamp of the created record; null indicates the task isn't created
     */
    class DialogShowActivityContract : ActivityResultContract<String?, Long?>() {
        override fun createIntent(context: Context, input: String?): Intent {
            return Intent(context, DialogShowActivity::class.java).apply {
                putExtra(EXTRA_DESCRIPTION_TEXT, input)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Long? {
            return intent.nullMap { it.getLongExtraOrNull(EXTRA_TIMESTAMP) }
        }
    }

    companion object {
        /**
         * nullable string intent extra
         * the default description text
         */
        const val EXTRA_DESCRIPTION_TEXT = "description"

        /**
         * long intent extra
         * used as the result of this activity
         * timestamp of the created task
         */
        const val EXTRA_TIMESTAMP = "timestamp"
    }
}