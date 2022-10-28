package pers.zhc.tools.note

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import kotlinx.android.synthetic.main.take_note_activity.*
import pers.zhc.tools.R
import pers.zhc.tools.utils.DialogUtils
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.androidAssert
import java.io.Serializable

class NoteTakingActivity : NoteBaseActivity() {
    private lateinit var type: Type
    private lateinit var contentET: EditText
    private lateinit var titleET: EditText
    private var modified = false

    /**
     * present when [type] is [Type.UPDATE]
     */
    private var timestamp: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.take_note_activity)

        titleET = doc_title_et!!
        contentET = doc_content_et!!.editText
        val bottomButton = add_record!!

        val intent = intent
        type = intent.getSerializableExtra(EXTRA_TYPE) as Type
        val buttonTextRes = when (type) {
            Type.CREATE -> {
                R.string.note_add_record_btn
            }
            Type.UPDATE -> {
                R.string.note_modify_record_btn
            }
        }
        bottomButton.setText(buttonTextRes)

        when (type) {
            Type.CREATE -> {
                bottomButton.setOnClickListener {
                    createNewNote()
                    modified = false
                }
            }
            Type.UPDATE -> {
                androidAssert(intent.hasExtra(EXTRA_TIMESTAMP))
                timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, -1)
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_TIMESTAMP, timestamp)
                }
                setResult(0, resultIntent)

                val record = database.query(timestamp!!)!!
                titleET.setText(record.title)
                contentET.setText(record.content)

                bottomButton.setOnClickListener {
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

    private fun createNewNote() {
        database.addRecord(title = titleET.text.toString(), content = contentET.text.toString())
        ToastUtils.show(this, R.string.note_record_adding_succeeded)
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
                    super.finish()
                },
                negativeAction = { _, _ -> super.finish() }
            ).show()
        } else {
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