package pers.zhc.tools.diary.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.diary_advenced_search_dialog.view.*
import kotlinx.android.synthetic.main.diary_main_diary_fragment.view.*
import kotlinx.android.synthetic.main.diary_stat_dialog.view.*
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.intellij.lang.annotations.Language
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
import pers.zhc.util.Assertion
import java.io.File
import java.util.*

/**
 * @author bczhc
 */
class DiaryFragment : DiaryBaseFragment(), Toolbar.OnMenuItemClickListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAdapter: MyAdapter
    private lateinit var constraintLayout: ConstraintLayout
    private val diaryItemDataList = ArrayList<DiaryItemData>()
    private val advancedSearchDialog by lazy {
        createAdvancedSearchDialog()
    }
    private var searchRegex: Regex? = null
    private val weeks: Array<String> by lazy {
        requireContext().resources.getStringArray(R.array.weeks)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val inflate = inflater.inflate(R.layout.diary_main_diary_fragment, container, false)
        recyclerView = inflate.recycler_view!!
        constraintLayout = inflate.constraint_layout

        loadRecyclerView()

        val toolbar = inflate.toolbar!!
        toolbar.setOnMenuItemClickListener(this)
        setupOuterToolbar(toolbar)

        val searchView = inflate.search_view!!
        configSearchView(searchView)

        return inflate
    }

    private fun configSearchView(searchView: SearchView) {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onQueryTextChange(query: String): Boolean {
                if (query.isEmpty()) {
                    refreshList()
                    return true
                }

                refreshItemDataList(
                    """

SELECT "date", content
FROM diary
WHERE instr(lower("date"), lower(?)) > 0
   OR instr(lower(content), lower(?)) > 0""", arrayOf(query, query)
                )

                recyclerViewAdapter.notifyDataSetChanged()
                return true
            }
        })
    }

    private fun loadRecyclerView() {
        FastScrollerBuilder(recyclerView).apply {
            setThumbDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.thumb)!!)
        }.build()
        Thread {
            refreshItemDataList()
        }.start()

        Common.runOnUiThread(requireContext()) {

            recyclerViewAdapter = MyAdapter(this, diaryItemDataList)
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
            MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        )
        dialog.show()
    }

    private fun popupMenuDelete(position: Int) {
        val dateInt = diaryItemDataList[position].dateInt
        DialogUtils.createConfirmationAlertDialog(requireContext(), { dialog, _ ->
            diaryDatabase.execBind("""DELETE FROM diary WHERE "date" IS ?""", arrayOf(dateInt))
            dialog.dismiss()

            // update view
            val removed = diaryItemDataList.removeAt(position)
            recyclerViewAdapter.notifyItemRemoved(position)

            Snackbar.make(constraintLayout, R.string.deleted_message, Snackbar.LENGTH_LONG).apply {
                setAction(R.string.undo) {
                    diaryDatabase.execBind(
                        "INSERT INTO diary(\"date\", content) VALUES (?, ?)",
                        arrayOf(removed.dateInt, removed.content)
                    )
                    Assertion.doAssertion(
                        diaryDatabase.hasRecord(
                            """SELECT COUNT() FROM diary WHERE "date" IS ?""", arrayOf(removed.dateInt)
                        )
                    )
                    diaryItemDataList.add(position, removed)
                    recyclerViewAdapter.notifyItemInserted(position)
                }
            }.show()
        }, titleRes = R.string.whether_to_delete, message = makeTitle(MyDate(dateInt))).show()
    }

    private fun changeDate(oldDateString: Int, newDate: Int) {
        diaryDatabase.execBind(
            """UPDATE diary
SET "date"=?
WHERE "date" IS ?""", arrayOf(newDate, oldDateString)
        )
    }

    private fun openDiaryPreview(dateInt: Int) {
        val intent = Intent(requireContext(), DiaryContentPreviewActivity::class.java).apply {
            putExtra(DiaryContentPreviewActivity.EXTRA_DATE_INT, dateInt)
            searchRegex?.let { putExtra(DiaryContentPreviewActivity.EXTRA_HIGHLIGHT_REGEX, it) }
        }
        startActivityForResult(intent, RequestCode.START_ACTIVITY_3)
    }

    private fun refreshItemDataList() {
        refreshItemDataList("""SELECT "date", content FROM diary""")
    }

    private fun refreshItemDataList(@Language("SQLite") sql: String, binds: Array<out Any>? = null) {
        diaryItemDataList.clear()

        val statement = diaryDatabase.compileStatement(sql, binds ?: arrayOf())
        val cursor = statement.cursor
        while (cursor.step()) {
            val date = cursor.getInt(0)
            val content = cursor.getText(1)
            diaryItemDataList.add(DiaryItemData(date, content))
        }

        statement.release()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
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
            R.id.attachment -> {
                val intent = Intent(context, DiaryAttachmentActivity::class.java)
                startActivity(intent)
            }
            R.id.settings -> {
                startActivity(Intent(context, DiaryAttachmentSettingsActivity::class.java))
            }
            R.id.statistics -> {
                showStatDialog()
            }
            R.id.advanced_search -> {
                showAdvancedSearchDialog()
            }
            R.id.sort_date -> {
                reorderDiary(Order.DATE)
            }
            R.id.sort_random -> {
                reorderDiary(Order.RANDOM)
            }
        }
        return true
    }

    private enum class Order {
        DATE, RANDOM
    }
    private fun reorderDiary(order: Order) {
        when (order) {
            Order.DATE -> {
                refreshItemDataList()
            }
            Order.RANDOM -> {
                refreshItemDataList("SELECT \"date\", content FROM diary ORDER BY random()")
            }
        }
        recyclerViewAdapter.notifyDataSetChanged()
    }

    private fun openRandomDiary() {
        var randomDate: Int? = null
        diaryDatabase.withCompiledStatement("SELECT \"date\" FROM diary ORDER BY random() LIMIT 1;") {
            val cursor = it.cursor
            randomDate = if (cursor.step()) {
                cursor.getInt(0)
            } else null
        }
        randomDate?.let { openDiaryPreview(it) }
    }

    private fun showAdvancedSearchDialog() {
        advancedSearchDialog.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun createAdvancedSearchDialog(): AlertDialog {
        val context = requireContext()

        val inflate = View.inflate(context, R.layout.diary_advenced_search_dialog, null)
        val regexView = inflate.regex_input!!
        return DialogUtils.createConfirmationAlertDialog(
            context,
            { _, _ ->

                val regex = regexView.regex ?: return@createConfirmationAlertDialog
                searchRegex = regex

                val progressDialog = ProgressDialog(context).also {
                    it.getProgressView().apply {
                        setIsIndeterminateMode(true)
                        setTitle(context.getString(R.string.diary_searching_progress_dialog_title))
                    }
                    it.setCancelable(false)
                    it.setCanceledOnTouchOutside(false)
                }
                progressDialog.show()

                Thread {
                    diaryItemDataList.clear()
                    diaryDatabase.queryExec("SELECT \"date\", content FROM diary") { cursor ->
                        while (cursor.step()) {
                            val date = cursor.getInt(0)
                            val content = cursor.getText(1)
                            if (regex.containsMatchIn(content)) {
                                diaryItemDataList.add(DiaryItemData(date, content))
                            }
                        }
                    }
                    Common.runOnUiThread(context) {
                        progressDialog.dismiss()
                        recyclerViewAdapter.notifyDataSetChanged()
                    }
                }.start()

            },
            view = inflate,
            width = MATCH_PARENT,
            titleRes = R.string.diary_advanced_search_dialog_title
        )
    }

    private fun showStatDialog() {
        val context = requireContext()
        val dialog = Dialog(context).apply {
            setTitle(R.string.diary_statistics_dialog_title)
            setContentView(View.inflate(context, R.layout.diary_stat_dialog, null).apply {
                this.stat_content_tv!!.text =
                    context.getString(R.string.diary_statistics_dialog_content, getTotalCharsCount(), getRowsCount())
            })
            DialogUtils.setDialogAttr(this, width = MATCH_PARENT)
        }
        dialog.show()
    }

    private fun getTotalCharsCount(): Int {
        val statement = diaryDatabase.compileStatement("SELECT SUM(length(content)) FROM diary")
        val cursor = statement.cursor
        Assertion.doAssertion(cursor.step())
        val c = cursor.getInt(0)
        statement.release()
        return c
    }

    private fun getRowsCount(): Int {
        return diaryDatabase.getRowCount("SELECT COUNT() FROM diary")
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
            MATCH_PARENT,
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

    @SuppressLint("NotifyDataSetChanged")
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
        private val outer: DiaryFragment,
        private val data: List<DiaryItemData>
    ) :
        AdapterWithClickListener<MyAdapter.MyViewHolder?>() {
        private val context = outer.requireContext()

        private class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        private fun createDiaryItemRL(parent: ViewGroup): View {
            return LayoutInflater.from(context).inflate(R.layout.diary_item_view, parent, false)
        }

        @SuppressLint("SetTextI18n")
        private fun bindDiaryItemRL(item: View, myDate: MyDate, content: String) {
            val dateTV = item.findViewById<TextView>(R.id.date_tv)
            val contentTV = item.findViewById<TextView>(R.id.content_tv)
            dateTV.text = outer.makeTitle(myDate)
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

    private val titleCalendar = Calendar.getInstance()
    private fun makeTitle(date: MyDate): String {
        var weekString: String? = null
        try {
            titleCalendar.clear()
            titleCalendar.set(date.year, date.month - 1, date.day)
            val weekIndex = titleCalendar[Calendar.DAY_OF_WEEK] - 1
            weekString = weeks[weekIndex]
        } catch (e: Exception) {
            Common.showException(e, context)
        }
        return "$date $weekString"
    }
}
