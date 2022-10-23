package pers.zhc.tools.note

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.take_note_activity.*
import pers.zhc.tools.R
import pers.zhc.tools.utils.ToastUtils
import java.io.Serializable

class NoteTakingActivity : NoteBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.take_note_activity)

        val titleET = doc_title_et!!
        val contentET = doc_content_et!!.editText
        val bottomButton = add_record!!

        val intent = intent
        val type = intent.getSerializableExtra(EXTRA_TYPE) as Type
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
                    database.addRecord(title = titleET.text.toString(), content = contentET.text.toString())
                    ToastUtils.show(this, R.string.note_record_adding_succeeded)
                }
            }
            Type.UPDATE -> {
                val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, -1)
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_TIMESTAMP, timestamp)
                }
                setResult(0, resultIntent)

                val record = database.query(timestamp)!!
                titleET.setText(record.title)
                contentET.setText(record.content)

                bottomButton.setOnClickListener {
                    database.update(
                        timestamp, Record(
                            timestamp, titleET.text.toString(),
                            contentET.text.toString()
                        )
                    )
                    ToastUtils.show(this, R.string.note_record_updating_succeeded)
                }
            }
        }
    }

    enum class Type : Serializable {
        CREATE, UPDATE
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