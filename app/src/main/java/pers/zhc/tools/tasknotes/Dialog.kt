package pers.zhc.tools.tasknotes

import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.databinding.TaskNotesRecordEditDialogBinding
import pers.zhc.tools.utils.setNegativeAction
import pers.zhc.tools.utils.setPositiveAction
import pers.zhc.tools.utils.unreachable
import java.util.*

object Dialog {
    fun createRecordEditDialog(
        activity: BaseActivity,
        initRecord: Record? = null,
        recreateMode: Boolean = false,
        onFinished: (record: Record?) -> Unit
    ) {
        val bindings = TaskNotesRecordEditDialogBinding.inflate(LayoutInflater.from(activity))
        val buttonToggleGroup = bindings.btg
        val timeTV = bindings.timeTv
        val descriptionET = bindings.descriptionEt

        var time = Time(Date())
        buttonToggleGroup.check(R.id.start)

        if (initRecord != null) {
            descriptionET.setText(initRecord.description)
            // in recreateMode, time and ButtonToggleGroup will use the default
            // ([current timestamp] and [R.id.start] respectively)
            // otherwise use values provided by initRecord
            if (!recreateMode) {
                time = initRecord.time
                buttonToggleGroup.check(
                    when (initRecord.mark) {
                        TaskMark.START -> R.id.start
                        TaskMark.END -> R.id.end
                    }
                )
            }
        }

        timeTV.text = time.format()
        bindings.timeButton.setOnClickListener {
            MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(time.hour)
                .setMinute(time.minute)
                .build().apply {
                    addOnPositiveButtonClickListener {
                        time = Time(hour, minute)
                        bindings.timeTv.text = time.format()
                    }
                }
                .show(activity.supportFragmentManager, "Time Picker")
        }

        MaterialAlertDialogBuilder(activity)
            .setView(bindings.root)
            .setNegativeAction { _, _ ->
                onFinished(null)
            }
            .setPositiveAction { _, _ ->
                val taskMark = when (buttonToggleGroup.checkedButtonId) {
                    R.id.start -> TaskMark.START
                    R.id.end -> TaskMark.END
                    else -> unreachable()
                }
                // always use the current timestamp
                val creationTime = System.currentTimeMillis()
                val record = Record(
                    descriptionET.text!!.toString(),
                    taskMark,
                    time,
                    creationTime
                )
                onFinished(record)
            }
            .setCancelable(true)
            .setOnCancelListener {
                onFinished(null)
            }
            .show()
    }
}
