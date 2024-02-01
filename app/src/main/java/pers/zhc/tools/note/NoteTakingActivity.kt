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
    private lateinit var type: Type
    private lateinit var contentET: EditText
    private lateinit var titleET: EditText
    private var modified = false

    /**
     * present when [type] is [Type.UPDATE], or after crating a new note
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
        type = intent.getSerializableExtra(EXTRA_TYPE)!! as Type
        val buttonTextRes = when (type) {
            Type.CREATE -> {
                R.string.note_add_record_btn
            }

            Type.UPDATE -> {
                R.string.note_modify_record_btn
            }
        }
        bottomButton.setText(buttonTextRes)

        if (type == Type.UPDATE) {
            androidAssert(intent.hasExtra(EXTRA_TIMESTAMP))
            timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, -1)
            val record = database.query(timestamp!!)!!
            titleET.setText(record.title)
            contentET.setText(record.content)
        }

        bottomButton.setOnClickListener {
            when (type) {
                Type.CREATE -> {
                    createNewNote()
                    modified = false
                    this.type = Type.UPDATE
                    bottomButton.setText(R.string.note_modify_record_btn)
                }
                Type.UPDATE -> {
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
                putExtra(EXTRA_TIMESTAMP, timestamp!!)
            })
        }
        if (modified) {
            DialogUtils.createConfirmationAlertDialog(this, titleRes = R.string.note_modified_save_alert,
                width = MATCH_PARENT,
                positiveAction = { _, _ ->
                    when (type) {
                        Type.CREATE -> {
                            createNewNote()
                        }

                        Type.UPDATE -> {
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
         * see [Type]
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
