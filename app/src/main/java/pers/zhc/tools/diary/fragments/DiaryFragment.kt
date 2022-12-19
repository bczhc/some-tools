package pers.zhc.tools.diary.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.diary_advenced_search_dialog.view.*
import kotlinx.android.synthetic.main.diary_main_diary_fragment.view.*
import kotlinx.android.synthetic.main.diary_stat_dialog.view.*
import org.intellij.lang.annotations.Language
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.R
import pers.zhc.tools.databinding.DiaryItemViewBinding
import pers.zhc.tools.databinding.DiaryPickingRandomDiaryDialogBinding
import pers.zhc.tools.diary.*
import pers.zhc.tools.filepicker.FilePickerActivityContract
import pers.zhc.tools.utils.*
import pers.zhc.util.Assertion
import java.io.File
import java.util.*

/**
 * @author bczhc
 */
class DiaryFragment : DiaryBaseFragment(), Toolbar.OnMenuItemClickListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAdapter: ListAdapter
    private lateinit var constraintLayout: ConstraintLayout
    private val diaryItemDataList = ArrayList<Diary>()
    private val advancedSearchDialog by lazy {
        createAdvancedSearchDialog()
    }
    private var searchRegex: Regex? = null
    private val weeks: Array<String> by lazy {
        requireContext().resources.getStringArray(R.array.weeks)
    }

    private val launchers = object {
        val writeOrCreateDiary = registerForActivityResult(DiaryTakingActivity.ActivityContract()) { result ->
            val dateInt = result.dateInt
            if (result.isNewRecord) {
                // add a new diary item
                diaryItemDataList.add(Diary(dateInt, diaryDatabase.queryDiaryContent(dateInt)))
                recyclerViewAdapter.notifyItemInserted(diaryItemDataList.size - 1)
            } else {
                // update list item
                val content = diaryDatabase.queryDiaryContent(dateInt)
                val position = diaryItemDataList.indexOfFirst { it.dateInt == dateInt }
                diaryItemDataList[position].content = content
                recyclerViewAdapter.notifyItemChanged(position)
            }
        }
        val importDiary = registerForActivityResult(
            FilePickerActivityContract(
                FilePickerActivityContract.FilePickerType.PICK_FILE,
                false
            )
        ) { result ->
            result ?: return@registerForActivityResult
            importDiary(File(result.path))
        }
        val exportDiary = registerForActivityResult(
            FilePickerActivityContract(
                FilePickerActivityContract.FilePickerType.PICK_FOLDER,
                false
            )
        ) { result ->
            result ?: return@registerForActivityResult
            exportDiary(File(result.path))
        }
        val openDiaryPreview = registerForActivityResult(DiaryContentPreviewActivity.ActivityContract()) { date ->
            // in DiaryContentPreviewActivity, use "edit" menu can edit the diary, so
            // here must do a list item updating
            val position = diaryItemDataList.indexOfFirst { it.dateInt == date.dateInt }
            androidAssert(position != -1)
            diaryItemDataList[position].content = diaryDatabase.queryDiaryContent(date.dateInt)
            recyclerViewAdapter.notifyItemChanged(position)
        }
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
        Thread {
            refreshItemDataList()
        }.start()

        val context = requireContext()

        Common.runOnUiThread(context) {
            recyclerViewAdapter = ListAdapter(context, diaryItemDataList, this::makeTitle)
            recyclerView.setUpFastScroll(context)
            recyclerView.setLinearLayoutManager()
            recyclerView.adapter = recyclerViewAdapter

            recyclerViewAdapter.setOnItemClickListener { position, _ ->
                openDiaryPreview(
                    diaryItemDataList[position].dateInt
                )
            }
            recyclerViewAdapter.setOnItemLongClickListener { position, view ->
                val popupMenu = PopupMenuUtil.create(
                    context,
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
            WRAP_CONTENT,
            false
        )
        dialog.show()
    }

    private fun popupMenuDelete(position: Int) {
        val diaryDatabase = diaryDatabase.database

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
                    androidAssert(
                        diaryDatabase.hasRecord(
                            """SELECT "date" FROM diary WHERE "date" IS ?""", arrayOf(removed.dateInt)
                        )
                    )
                    diaryItemDataList.add(position, removed)
                    recyclerViewAdapter.notifyItemInserted(position)
                }
            }.show()
        }, titleRes = R.string.whether_to_delete, message = makeTitle(MyDate(dateInt))).show()
    }

    private fun changeDate(oldDateString: Int, newDate: Int) {
        diaryDatabase.database.execBind(
            """UPDATE diary
SET "date"=?
WHERE "date" IS ?""", arrayOf(newDate, oldDateString)
        )
    }

    private fun openDiaryPreview(dateInt: Int) {
        launchers.openDiaryPreview.launch(
            DiaryContentPreviewActivity.ActivityContract.Input(
                dateInt,
                searchRegex
            )
        )
    }

    private fun refreshItemDataList(
        @Language("SQLite") sql: String = "SELECT \"date\", content FROM diary",
        binds: Array<Any>? = null
    ) {
        diaryItemDataList.let {
            it.clear()
            it.addAll(diaryDatabase.queryDiaries(sql, binds))
        }
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
                launchers.exportDiary.launch(Unit)
            }

            R.id.import_ -> {
                launchers.importDiary.launch(Unit)
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

            R.id.random -> {
                showPickingRandomDiaryDialog()
            }
        }
        return true
    }

    private fun showPickingRandomDiaryDialog() {
        val context = requireContext()

        val bindings = DiaryPickingRandomDiaryDialogBinding.inflate(LayoutInflater.from(context))
        bindings.root.setOnClickListener {
            val randomDateInt = diaryDatabase.pickRandomDiary() ?: run {
                ToastUtils.show(context, R.string.diary_no_diary_toast)
                return@setOnClickListener
            }
            openDiaryPreview(randomDateInt)
        }

        Dialog(context).apply {
            setCancelable(true)
            setContentView(bindings.root)
            DialogUtils.setDialogAttr(
                this,
                isTransparent = true,
                width = WRAP_CONTENT,
                height = WRAP_CONTENT,
            )
        }.show()
    }

    private fun showAdvancedSearchDialog() {
        advancedSearchDialog.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun createAdvancedSearchDialog(): AlertDialog {
        val diaryDatabase = diaryDatabase.database

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
                                diaryItemDataList.add(Diary(date, content))
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
        val diaryDatabase = diaryDatabase.database

        val statement = diaryDatabase.compileStatement("SELECT SUM(length(content)) FROM diary")
        val cursor = statement.cursor
        Assertion.doAssertion(cursor.step())
        val c = cursor.getInt(0)
        statement.release()
        return c
    }

    private fun getRowsCount(): Int {
        val diaryDatabase = diaryDatabase.database

        return diaryDatabase.getRowCount("SELECT COUNT() FROM diary")
    }

    private fun showCreateSpecifiedDateDiaryDialog() {
        MaterialDatePicker.Builder.datePicker().apply {

        }.build().apply {
            addOnPositiveButtonClickListener {
                val dateInt = MyDate(Date(it)).dateInt

                if (diaryDatabase.hasDiary(dateInt)) {
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
            WRAP_CONTENT,
            false
        )
        dialog.show()
    }

    private fun createDiary(dateInt: Int) {
        launchers.writeOrCreateDiary.launch(MyDate(dateInt))
    }

    private fun writeDiary() {
        val date = MyDate(getCurrentDateInt())

        launchers.writeOrCreateDiary.launch(date)
    }

    private fun getCurrentDateInt(): Int {
        val date = MyDate(Date(System.currentTimeMillis()))
        return date.dateInt
    }

    private fun importDiary(file: File) {
        val refCount = DiaryDatabase.getDatabaseRefCount()

        // the only one reference is for the current activity
        if (refCount > 1) {
            ToastUtils.show(context, getString(R.string.diary_import_ref_count_not_zero_msg, refCount))
            return
        }

        val diaryActivity = requireActivity() as DiaryBaseActivity
        diaryActivity.diaryDatabaseRef.release()
        androidAssert(DiaryDatabase.getDatabaseRefCount() == 0)

        FileUtil.copy(file, DiaryDatabase.internalDatabasePath)

        var msgResOnFinished = 0
        SQLite3::class.withNew(DiaryDatabase.internalDatabasePath.path) {
            if (it.checkIfCorrupt()) {
                msgResOnFinished = R.string.corrupted_database_and_recreate_new_msg
                DiaryDatabase.internalDatabasePath.requireDelete()
            } else {
                msgResOnFinished = R.string.importing_succeeded
            }
        }

        // substitute old references
        diaryActivity.diaryDatabaseRef = DiaryDatabase.getDatabaseRef()
        diaryActivity.diaryDatabase = diaryActivity.diaryDatabaseRef.get()
        diaryDatabase = diaryActivity.diaryDatabase

        ToastUtils.show(context, msgResOnFinished)

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
                FileUtil.copy(databaseFile, File(dir, "diary.db"))
                ToastUtils.show(context, R.string.exporting_succeeded)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                ToastUtils.showError(context, R.string.copying_failed, e)
            }
        }.start()
    }

    private val titleCalendar = Calendar.getInstance()

    private fun makeTitle(date: MyDate): String {
        titleCalendar.apply {
            clear()
            set(date.year, date.month - 1, date.day)
        }
        val weekString = weeks[titleCalendar[Calendar.DAY_OF_WEEK] - 1]
        return "$date $weekString"
    }

    private class ListAdapter(
        private val context: Context,
        private val data: List<Diary>,
        private val makeTitle: (date: MyDate) -> String,
    ) :
        AdapterWithClickListener<ListAdapter.MyViewHolder?>() {

        private class MyViewHolder(val bindings: DiaryItemViewBinding) : RecyclerView.ViewHolder(bindings.root)

        override fun onCreateViewHolder(parent: ViewGroup): MyViewHolder {
            val bindings = DiaryItemViewBinding.inflate(LayoutInflater.from(context), parent, false)
            return MyViewHolder(bindings)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val bindings = holder.bindings
            val itemData = data[position]
            bindings.dateTv.text = makeTitle(MyDate(itemData.dateInt))
            bindings.contentTv.text = itemData.content.limitText(100)
        }

        override fun getItemCount(): Int {
            return data.size
        }
    }
}
