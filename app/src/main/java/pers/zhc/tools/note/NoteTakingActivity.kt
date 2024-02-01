package pers.zhc.tools.note

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import pers.zhc.tools.R
import pers.zhc.tools.databinding.TakeNoteActivityBinding
import pers.zhc.tools.utils.DialogUtils
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.androidAssert
import java.io.Serializable

class NoteTakingActivity : NoteBaseActivity() {
    /**
     * After creating a new note, this will be [Type.UPDATE]
     */
    private lateinit var onSaveAction: OnSaveAction
    private lateinit var contentET: EditText
    private lateinit var titleET: EditText
    private var modified = false

    /**
     * The timestamp of the current note.
     *
     * If `Intent` [EXTRA_TYPE] is [Type.UPDATE], this will be set, or,
     * after creating a new note, this will be set.
     */
    private var timestamp: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = TakeNoteActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        titleET = bindings.docTitleEt
        contentET = bindings.docContentEt.editText
        val bottomButton = bindings.addRecord

        val intent = intent

        @Suppress("DEPRECATION")
        val type = intent.getSerializableExtra(EXTRA_TYPE)!! as Type
        when (type) {
            Type.CREATE -> {
                onSaveAction = OnSaveAction.CREATE_NEW
                bottomButton.setText(R.string.note_add_record_btn)
            }

            Type.UPDATE -> {
                onSaveAction = OnSaveAction.UPDATE
                bottomButton.setText(R.string.note_modify_record_btn)
                androidAssert(intent.hasExtra(EXTRA_TIMESTAMP))
                timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, -1)
                val record = database.query(timestamp!!)!!
                titleET.setText(record.title)
                contentET.setText(record.content)
            }
        }

        bottomButton.setOnClickListener {
            when (onSaveAction) {
                OnSaveAction.CREATE_NEW -> {
                    createNewNote()
                    modified = false
                    onSaveAction = OnSaveAction.UPDATE
                    bottomButton.setText(R.string.note_modify_record_btn)
                }

                OnSaveAction.UPDATE -> {
                    updateNote()
                    modified = false
                }
            }
        }

        val afterTextChangedAction: (text: Editable?) -> Unit = {
            modified = true
        }
        titleET.doAfterTextChanged(afterTextChangedAction)
        contentET.doAfterTextChanged(afterTextChangedAction)
    }

    enum class Type : Serializable {
        CREATE, UPDATE
    }

    enum class OnSaveAction {
        CREATE_NEW, UPDATE
    }

    /**
     * Returns timestamp key of the just-create note
     */
    private fun createNewNote() {
        val timestamp = System.currentTimeMillis()
        database.addRecord(title = titleET.text.toString(), content = contentET.text.toString(), timestamp = timestamp)
        ToastUtils.show(this, R.string.note_record_adding_succeeded)
        this.timestamp = timestamp
    }

    private fun updateNote() {
        val timestamp = this.timestamp!!
        database.update(
            timestamp, Record(
                timestamp, titleET.text.toString(),
                contentET.text.toString()
            )
        )
        ToastUtils.show(this, R.string.note_record_updating_succeeded)
    }

    override fun finish() {
        val setResultIntent = {
            setResult(0, Intent().apply {
                putExtra(EXTRA_TIMESTAMP, timestamp)
            })
        }
        if (modified) {
            DialogUtils.createConfirmationAlertDialog(this, titleRes = R.string.note_modified_save_alert,
                width = MATCH_PARENT,
                positiveAction = { _, _ ->
                    when (onSaveAction) {
                        OnSaveAction.CREATE_NEW -> {
                            createNewNote()
                        }

                        OnSaveAction.UPDATE -> {
                            updateNote()
                        }
                    }
                    setResultIntent()
                    super.finish()
                },
                negativeAction = { _, _ -> super.finish() }
            ).show()
        } else {
            setResultIntent()
            super.finish()
        }
    }

    companion object {
        /**
         * Serializable intent extra
         *
         * Activity start type.
         *
         * See [Type]
         */
        const val EXTRA_TYPE = "type"

        /**
         * Long intent extra
         * this will be used when [Type] is [Type.UPDATE]
         * and this extra value should be set back via [setResult]
         * for `RecyclerView` updating purposes
         */
        const val EXTRA_TIMESTAMP = "timestamp"
    }
}
