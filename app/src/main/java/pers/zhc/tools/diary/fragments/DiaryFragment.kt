package pers.zhc.tools.diary.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.diary_main_diary_fragment.view.*
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.BaseActivity.RequestCode
import pers.zhc.tools.R
import pers.zhc.tools.diary.*
import pers.zhc.tools.diary.DiaryDatabase.Companion.changeDatabase
import pers.zhc.tools.diary.DiaryDatabase.Companion.getDatabaseRef
import pers.zhc.tools.diary.DiaryDatabase.Companion.getHolder
import pers.zhc.tools.diary.DiaryDatabase.Companion.releaseDatabaseRef
import pers.zhc.tools.filepicker.FilePicker
import pers.zhc.tools.utils.*
import pers.zhc.tools.utils.FileUtil.Companion.copy
import pers.zhc.tools.utils.PopupMenuUtil.Companion.createPopupMenu
import java.io.File
import java.util.*

/**
 * @author bczhc
 */
class DiaryFragment : DiaryBaseFragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAdapter: MyAdapter
    private val diaryItemDataList = ArrayList<DiaryItemData>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val inflate = inflater.inflate(R.layout.diary_main_diary_fragment, container, false)
        recyclerView = inflate.recycler_view!!

        loadRecyclerView()

        return inflate
    }

    private fun loadRecyclerView() {
        FastScrollerBuilder(recyclerView).apply {
            setThumbDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.thumb)!!)
        }.build()
        Thread {
            refreshItemDataList()
        }.start()

        Common.runOnUiThread(requireContext()) {

            recyclerViewAdapter = MyAdapter(requireContext(), diaryItemDataList)
            recyclerView.adapter = recyclerViewAdapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            recyclerViewAdapter.setOnItemClickListener { position, _ ->
                openDiaryPreview(
                    diaryItemDataList[position].dateInt
                )
            }
            recyclerViewAdapter.setOnItemLongClickListener { position, view ->
                val popupMenu = createPopupMenu(
                    requireContext(),
                    view, R.menu.diary_popup_menu
                )
                popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                    val itemId = item.itemId
                    if (itemId == R.id.change_date_btn) {
                        popupMenuChangeDate(position)
                    } else if (itemId == R.id.delete_btn) {
                        popupMenuDelete(position)
                    }
                    true
                }
                popupMenu.show()
            }

        }
    }

    private fun popupMenuChangeDate(position: Int) {
        val diaryItemData = diaryItemDataList[position]
        val oldDateInt = diaryItemData.dateInt
        val dateET = EditText(requireContext())
        val dialog: AlertDialog = DialogUtil.createConfirmationAlertDialog(
            requireContext(),
            { d, _ ->
                val dateString = dateET.text.toString()
                val newDateInt = try {
                    dateString.toInt()
                } catch (e: java.lang.Exception) {
                    ToastUtils.show(requireContext(), R.string.please_enter_correct_value_toast)
                    return@createConfirmationAlertDialog
                }
                changeDate(oldDateInt, newDateInt)
                d.dismiss()

                // update view
                diaryItemData.dateInt = newDateInt
                recyclerViewAdapter.notifyItemChanged(position)
            },
            null,
            dateET,
            R.string.enter_new_date,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        )
        dialog.show()
    }

    private fun popupMenuDelete(position: Int) {
        val dateInt = diaryItemDataList[position].dateInt
        DialogUtil.createConfirmationAlertDialog(requireContext(), { dialog, _ ->
            val statement = diaryDatabase.compileStatement(
                """DELETE
FROM diary
WHERE "date" IS ?"""
            )
            statement.bind(1, dateInt)
            statement.step()
            statement.release()
            dialog.dismiss()

            // update view
            diaryItemDataList.removeAt(position)
            recyclerViewAdapter.notifyItemRemoved(position)
        }, R.string.whether_to_delete).show()
    }

    private fun changeDate(oldDateString: Int, newDate: Int) {
        diaryDatabase.execBind(
            """UPDATE diary
SET "date"=?
WHERE "date" IS ?""", arrayOf(newDate, oldDateString)
        )
    }

    private fun openDiaryPreview(dateInt: Int) {
        val intent = Intent(requireContext(), DiaryContentPreviewActivity::class.java)
        intent.putExtra(DiaryContentPreviewActivity.EXTRA_DATE_INT, dateInt)
        startActivityForResult(intent, RequestCode.START_ACTIVITY_3)
    }

    private fun refreshItemDataList() {
        diaryItemDataList.clear()

        val statement = diaryDatabase.compileStatement(
            """SELECT "date", content
FROM diary"""
        )
        val cursor = statement.cursor
        while (cursor.step()) {
            val date = cursor.getInt(0)
            val content = cursor.getText(1)
            diaryItemDataList.add(DiaryItemData(date, content))
        }

        statement.release()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.write_diary -> {
                writeDiary()
            }
            R.id.create -> {
                showCreateSpecifiedDateDiaryDialog()
            }
            R.id.export -> {
                val intent = Intent(context, FilePicker::class.java)
                intent.putExtra(FilePicker.EXTRA_OPTION, FilePicker.PICK_FOLDER)
                startActivityForResult(intent, RequestCode.START_ACTIVITY_1)
            }
            R.id.import_ -> {
                val intent = Intent(context, FilePicker::class.java)
                intent.putExtra(FilePicker.EXTRA_OPTION, FilePicker.PICK_FILE)
                startActivityForResult(intent, RequestCode.START_ACTIVITY_2)
            }
            R.id.sort -> {
                sort()
            }
            R.id.attachment -> {
                val intent = Intent(context, DiaryAttachmentActivity::class.java)
                startActivity(intent)
            }
            R.id.settings -> {
                startActivity(Intent(context, DiaryAttachmentSettingsActivity::class.java))
            }
        }
        return true
    }

    private fun showCreateSpecifiedDateDiaryDialog() {
        MaterialDatePicker.Builder.datePicker().apply {

        }.build().apply {
            addOnPositiveButtonClickListener {
                val dateInt = MyDate(Date(it)).dateInt

                if (checkRecordExistence(dateInt)) {
                    showDuplicateConfirmDialog(dateInt)
                } else {
                    createDiary(dateInt)
                }
            }
        }.show(childFragmentManager, javaClass.name)
    }

    private fun showDuplicateConfirmDialog(dateInt: Int) {
        val dialog = DialogUtil.createConfirmationAlertDialog(context, { d, _ ->
            openDiaryPreview(dateInt)
            d.dismiss()
            d.dismiss()
        }, R.string.diary_duplicated_diary_dialog_title)
        DialogUtil.setDialogAttr(
            dialog,
            false,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        )
        dialog.show()
    }

    private fun createDiary(dateInt: Int) {
        val intent = Intent(context, DiaryTakingActivity::class.java)
        // use the current time
        intent.putExtra(DiaryTakingActivity.EXTRA_DATE_INT, dateInt)
        startActivityForResult(intent, RequestCode.START_ACTIVITY_0)
    }

    private fun checkRecordExistence(dateInt: Int): Boolean {
        return diaryDatabase.hasRecord(
            """SELECT "date"
FROM diary
WHERE "date" IS ?""", arrayOf<Any>(dateInt)
        )
    }

    private fun sort() {
        val foreignKeys: Int = getForeignKeyState()
        // disable foreign keys constraint
        setForeignKeyState(0)
        diaryDatabase.exec("DROP TABLE IF EXISTS tmp")
        diaryDatabase.beginTransaction()
        val statement = diaryDatabase.compileStatement(
            """SELECT sql
FROM sqlite_master
WHERE type IS 'table'
  AND tbl_name IS 'diary'"""
        )
        val cursor = statement.cursor
        Common.doAssertion(cursor.step())
        val diaryTableSql = cursor.getText(statement.getIndexByColumnName("sql"))
        statement.release()
        val tmpTableSql = diaryTableSql.replaceFirst("diary".toRegex(), "tmp")
        diaryDatabase.exec(tmpTableSql)
        diaryDatabase.exec("INSERT INTO tmp SELECT * FROM diary ORDER BY \"date\"")
        diaryDatabase.exec("DROP TABLE diary")
        diaryDatabase.exec("ALTER TABLE tmp RENAME TO diary")
        diaryDatabase.commit()
        refreshList()

        // restore foreign keys setting
        setForeignKeyState(foreignKeys)
    }

    private fun getForeignKeyState(): Int {
        val fk = diaryDatabase.compileStatement("PRAGMA foreign_keys")
        val fkCursor = fk.cursor
        Common.doAssertion(fkCursor.step())
        val foreignKey = fkCursor.getInt(0)
        fk.release()
        return foreignKey
    }

    private fun setForeignKeyState(state: Int) {
        val fk = diaryDatabase.compileStatement("PRAGMA foreign_keys=$state")
        fk.step()
        fk.release()
    }

    private fun writeDiary() {
        val recordExistence: Boolean = checkRecordExistence(getCurrentDateInt())
        if (recordExistence) {
            val intent = Intent(context, DiaryTakingActivity::class.java)
            intent.putExtra(DiaryTakingActivity.EXTRA_DATE_INT, getCurrentDateInt())
            startActivityForResult(intent, RequestCode.START_ACTIVITY_4)
        } else {
            createDiary(getCurrentDateInt())
        }
    }

    private fun getCurrentDateInt(): Int {
        val date = MyDate(Date(System.currentTimeMillis()))
        return date.dateInt
    }

    private fun getDiaryItemPosition(dateInt: Int): Int {
        for (i in diaryItemDataList.indices) {
            if (diaryItemDataList[i].dateInt == dateInt) {
                return i
            }
        }
        return -1
    }

    private fun queryDiaryContent(dateInt: Int): String? {
        val statement = diaryDatabase.compileStatement(
            """SELECT content
FROM diary
WHERE "date" IS ?"""
        )
        statement.bind(1, dateInt)
        val cursor = statement.cursor
        Common.doAssertion(cursor.step())
        val content = cursor.getText(0)
        statement.release()
        return content
    }

    private fun importDiary(file: File) {
        val refCount = getHolder().refCount
        // the only one reference is for the current activity
        if (refCount > 1) {
            ToastUtils.show(context, getString(R.string.diary_import_ref_count_not_zero_msg, refCount))
            return
        }
        releaseDatabaseRef()
        Common.doAssertion(getHolder().refCount == 0)

        val latch = SpinLatch()
        latch.suspend()
        Thread {
            copy(file, File(DiaryDatabase.internalDatabasePath))
            latch.stop()
        }.start()
        latch.await()
        ToastUtils.show(context, R.string.importing_succeeded)
        val newDatabase = SQLite3.open(DiaryDatabase.internalDatabasePath)
        if (newDatabase.checkIfCorrupt()) {
            newDatabase.close()
            if (!File(DiaryDatabase.internalDatabasePath).delete()) {
                throw RuntimeException("Failed to delete corrupted database file.")
            }
            ToastUtils.show(context, R.string.corrupted_database_and_recreate_new_msg)
        }
        changeDatabase(file.path)
        (activity as DiaryBaseActivity).diaryDatabase = getDatabaseRef()
        diaryDatabase = (activity as DiaryBaseActivity).diaryDatabase
        refreshList()
    }

    private fun refreshList() {
        refreshItemDataList()
        recyclerViewAdapter.notifyDataSetChanged()
    }

    private fun exportDiary(dir: File) {
        val databaseFile = Common.getInternalDatabaseDir(context, "diary.db")
        Thread {
            try {
                copy(databaseFile, File(dir, "diary.db"))
                ToastUtils.show(context, R.string.exporting_succeeded)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                ToastUtils.showError(context, R.string.copying_failed, e)
            }
        }.start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RequestCode.START_ACTIVITY_0 -> {
                // "write diary" action: on diary taking activity returned
                // it must be an activity which intends to create a new diary record
                data!!
                val dateInt = data.getIntExtra(DiaryTakingActivity.EXTRA_DATE_INT, -1)

                // update view
                diaryItemDataList.add(DiaryItemData(dateInt, queryDiaryContent(dateInt)!!))
                recyclerViewAdapter.notifyItemInserted(diaryItemDataList.size - 1)
            }
            RequestCode.START_ACTIVITY_1 -> {
                // export
                data ?: return
                val dir = data.getStringExtra(FilePicker.EXTRA_RESULT) ?: return
                exportDiary(File(dir))
            }
            RequestCode.START_ACTIVITY_2 -> {
                // import
                data ?: return
                val file = data.getStringExtra(FilePicker.EXTRA_RESULT) ?: return
                importDiary(File(file))
            }
            RequestCode.START_ACTIVITY_3, RequestCode.START_ACTIVITY_4 -> {
                // START_ACTIVITY_3:
                // "write diary" action: start a diary taking activity directly when the diary of today's date already exists
                // refresh the corresponding view
                data!!
                Common.doAssertion(data.hasExtra(DiaryContentPreviewActivity.EXTRA_DATE_INT))
                val dateInt = data.getIntExtra(DiaryContentPreviewActivity.EXTRA_DATE_INT, -1)
                val content = queryDiaryContent(dateInt)
                val position = getDiaryItemPosition(dateInt)
                diaryItemDataList[position].content = content!!
                recyclerViewAdapter.notifyItemChanged(position)
            }
            else -> {
            }
        }
    }

    private class DiaryItemData(var dateInt: Int, var content: String)

    private class MyAdapter(
        private val context: Context,
        private val data: List<DiaryItemData>
    ) :
        AdapterWithClickListener<MyAdapter.MyViewHolder?>() {
        private val weeks: Array<String> = context.resources.getStringArray(R.array.weeks)

        private class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        private fun createDiaryItemRL(parent: ViewGroup): View {
            return LayoutInflater.from(context).inflate(R.layout.diary_item_view, parent, false)
        }

        @SuppressLint("SetTextI18n")
        private fun bindDiaryItemRL(item: View, myDate: MyDate, content: String) {
            val dateTV = item.findViewById<TextView>(R.id.date_tv)
            val contentTV = item.findViewById<TextView>(R.id.content_tv)
            var weekString: String? = null
            try {
                val calendar = Calendar.getInstance()
                calendar[myDate.year, myDate.month - 1] = myDate.day
                val weekIndex = calendar[Calendar.DAY_OF_WEEK] - 1
                weekString = weeks[weekIndex]
            } catch (e: Exception) {
                Common.showException(e, context)
            }
            dateTV.text = "$myDate $weekString"
            contentTV.text = limitText(content)
        }

        private fun limitText(s: String): String {
            return if (s.length > 100) s.substring(0, 100) + "..." else s
        }

        override fun onCreateViewHolder(parent: ViewGroup): MyViewHolder {
            val diaryItemRL = createDiaryItemRL(parent)
            return MyViewHolder(diaryItemRL)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val itemData = data[position]
            bindDiaryItemRL(holder.itemView, MyDate(itemData.dateInt), itemData.content)
        }

        override fun getItemCount(): Int {
            return data.size
        }
    }
}