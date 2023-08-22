package pers.zhc.tools.filepicker

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

class FilePickerActivityContract(
    private val pickerType: FilePickerType,
    private val showFilenameET: Boolean,
    private val defaultFilename: String = ""
) :
    ActivityResultContract<Unit, FilePickerActivityContract.Result?>() {
    enum class FilePickerType(val enumInt: Int) {
        PICK_FILE(FilePicker.PICK_FILE),
        PICK_FOLDER(FilePicker.PICK_FOLDER);
    }

    class Result(
        val path: String,
        val filename: String?
    )

    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(context, FilePicker::class.java).apply {
            putExtra(FilePicker.EXTRA_OPTION, pickerType.enumInt)
            putExtra(FilePicker.EXTRA_ENABLE_FILENAME, showFilenameET)
            putExtra(FilePicker.EXTRA_DEFAULT_FILENAME, defaultFilename)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Result? {
        intent ?: return null
        val path = intent.getStringExtra(FilePicker.EXTRA_RESULT)!!
        val filename = intent.getStringExtra(FilePicker.EXTRA_FILENAME_RESULT)
        return Result(path, filename)
    }
}
