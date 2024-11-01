package pers.zhc.tools.diary.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.intellij.lang.annotations.Language
import pers.zhc.tools.R
import pers.zhc.tools.databinding.*
import pers.zhc.tools.diary.*
import pers.zhc.tools.filepicker.FilePickerActivityContract
import pers.zhc.tools.jni.JNI.CharStat
import pers.zhc.tools.utils.*
import pers.zhc.util.Assertion
import java.io.File
import java.util.*
import java.util.regex.Pattern

/**
 * @author bczhc
 */
class DiaryFragment : DiaryBaseFragment(), Toolbar.OnMenuItemClickListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAdapter: ListAdapter
    private lateinit var constraintLayout: ConstraintLayout
    private val diaryItemDataList = ArrayList<DiaryListItem>()
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
                // add a new diary item (to the end)
                diaryItemDataList.add(DiaryListItem(diary = Diary(dateInt, diaryDatabase.queryDiaryContent(dateInt))))
                recyclerViewAdapter.notifyItemInserted(diaryItemDataList.size - 1)
            } else {
                // update list item
                val content = diaryDatabase.queryDiaryContent(dateInt)
                val position = diaryItemDataList.indexOfFirst { it.diary.dateInt == dateInt }
                diaryItemDataList[position].diary.content = content
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
                true,
                defaultFilename = "diary.db"
            )
        ) { result ->
            result ?: return@registerForActivityResult
            exportDiary(File(result.path, result.filename!!))
        }
        val openDiaryPreview = registerForActivityResult(DiaryContentPreviewActivity.ActivityContract()) { date ->
            // in DiaryContentPreviewActivity, use "edit" menu can edit the diary, so
            // here must do a list item updating
            val position = diaryItemDataList.indexOfFirst { it.diary.dateInt == date.dateInt }
            androidAssert(position != -1)
            diaryItemDataList[position].diary.content = diaryDatabase.queryDiaryContent(date.dateInt)
            recyclerViewAdapter.notifyItemChanged(position)
        }
    }

    private var actionMode: ActionMode? = null
    private var inActionMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val bindings = DiaryMainDiaryFragmentBinding.inflate(inflater, container, false)
        val inflate = bindings.root
        recyclerView = bindings.recyclerView
        constraintLayout = bindings.constraintLayout

        loadRecyclerView()

        val toolbar = bindings.toolbar
        toolbar.setOnMenuItemClickListener(this)
        setupOuterToolbar(toolbar)

        val searchView = bindings.searchView
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
                    searchRegex = null
                    return true
                }

                searchRegex = Regex(Pattern.quote(query), RegexOption.IGNORE_CASE)

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

        Common.runOnUiThread {
            recyclerViewAdapter = ListAdapter(context, diaryItemDataList, this::makeTitle)
            recyclerView.setUpFastScroll(context)
            recyclerView.setLinearLayoutManager()
            recyclerView.adapter = recyclerViewAdapter

            recyclerViewAdapter.setOnItemClickListener { position, _ ->
                val thisItem = diaryItemDataList[position]
                if (inActionMode) {
                    thisItem.selected = !thisItem.selected
                    recyclerViewAdapter.notifyItemChanged(position)
                } else {
                    openDiaryPreview(
                        thisItem.diary.dateInt
                    )
                }
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
        val oldDateInt = diaryItemData.diary.dateInt
        val dateET = EditText(requireContext())
        val dialog: AlertDialog = DialogUtil.createConfirmationAlertDialog(
            requireContext(),
            { d, _ ->
                val dateString = dateET.text.toString()
                val newDateInt = try {
                    dateString.toInt()
                } catch (e: Exception) {
                    ToastUtils.show(requireContext(), R.string.please_enter_correct_value_toast)
                    return@createConfirmationAlertDialog
                }
                changeDate(oldDateInt, newDateInt)
                d.dismiss()

                // update view
                diaryItemData.diary.dateInt = newDateInt
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

        val dateInt = diaryItemDataList[position].diary.dateInt
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
                        arrayOf(removed.diary.dateInt, removed.diary.content)
                    )
                    androidAssert(
                        diaryDatabase.hasRecord(
                            """SELECT "date" FROM diary WHERE "date" IS ?""", arrayOf(removed.diary.dateInt)
                        )
                    )
                    diaryItemDataList.add(position, removed)
                    recyclerViewAdapter.notifyItemInserted(position)
                }
            }.show()
        }, titleRes = R.string.whether_to_delete, message = makeTitle(IntDate(dateInt))).show()
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
            val rows = diaryDatabase.queryDiaries(sql, binds)
            it.addAll(rows.asSequence().map { x -> DiaryListItem(diary = x) })
            rows.release()
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

            R.id.password -> {
                changePasswordDialog()
            }

            R.id.multi_select -> {
                startMultipleSelection()
            }

            R.id.ui_password -> {
                val context = requireContext()
                val bindings = VisiblePasswordDialogBinding.inflate(LayoutInflater.from(context))
                DialogUtils.createConfirmationAlertDialog(context,
                    title = getString(R.string.diary_ui_password_menu),
                    view = bindings.root,
                    positiveAction = { _, _ ->
                        val uiPassword = bindings.et.text.toString()
                        val config = LocalConfig.read()
                        config.uiPassword = uiPassword
                        LocalConfig.write(config)
                    }
                ).show()
            }
        }
        return true
    }

    private fun startMultipleSelection() {
        if (inActionMode) {
            inActionMode = false
            finishActionMode()
            return
        }

        inActionMode = true
        androidAssert(actionMode == null)
        val parent = requireActivity() as AppCompatActivity
        actionMode = parent.startSupportActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu): Boolean {
                parent.menuInflater.inflate(R.menu.diary_multi_selection_action_mode, menu)
                (menu.findItem(R.id.select_all).actionView as MaterialCheckBox).setOnCheckedChangeListener { _, isChecked ->
                    diaryItemDataList.forEach {
                        it.selected = isChecked
                    }
                    recyclerViewAdapter.notifyDataSetChanged()
                }
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return true
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.stat -> {
                        showSelectedStat()
                    }

                    else -> {
                        return false
                    }
                }
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                inActionMode = false
                finishActionMode()
            }
        })
    }

    private fun showSelectedStat() {
        val total = diaryItemDataList.filter { it.selected }.sumOf {
            CharStat.graphemeCount(it.diary.content)
        }
        DiaryBaseActivity.createDiaryRecordStatDialog(requireContext(), total).show()
    }

    private fun finishActionMode() {
        actionMode!!.finish()
        actionMode = null
        clearSelectionState()
        recyclerViewAdapter.notifyDataSetChanged()
    }

    private fun clearSelectionState() {
        diaryItemDataList.forEach {
            it.selected = false
        }
    }

    private fun changePasswordDialog() {
        val context = requireContext()
        val bindings = DiaryChangePasswordDialogBinding.inflate(LayoutInflater.from(context))

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.diary_change_password_dialog_title)
            .setView(bindings.root)
            .setNegativeAction()
            .setPositiveAction { _, _ ->
                val newPassword = bindings.password1Et.text.toString().ifEmpty { DiaryDatabase.DEFAULT_PASSPHRASE }
                diaryDatabase.rekey(newPassword)
                ToastUtils.show(context, R.string.diary_password_change_succeeded)
                LocalConfig.write(LocalConfig.read().apply {
                    this.password = newPassword
                })
            }
            .show()

        bindings.currentPasswordTv.text = LocalConfig.readPassword()
        val checkPassword = {
            val errorText = if (bindings.password1Et.text.toString() != bindings.password2Et.text.toString()) {
                getString(R.string.password_not_same_error_msg)
            } else {
                null
            }
            bindings.til2.error = errorText
        }
        bindings.password1Et.doAfterTextChanged {
            checkPassword()
        }
        bindings.password2Et.doAfterTextChanged {
            checkPassword()
        }
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
        val bindings = DiaryAdvencedSearchDialogBinding.bind(inflate)
        val regexView = bindings.regexInput
        return DialogUtils.createConfirmationAlertDialog(
            context,
            { _, _ ->
                val ignoreCase = bindings.ignoreCaseCb.isChecked

                val regex = (regexView.regex ?: return@createConfirmationAlertDialog).let {
                    if (ignoreCase) {
                        Regex(it.pattern, RegexOption.IGNORE_CASE)
                    } else it
                }

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
                                diaryItemDataList.add(DiaryListItem(diary = Diary(date, content)))
                            }
                        }
                    }
                    Common.runOnUiThread {
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
                this.findViewById<TextView>(R.id.stat_content_tv)!!.text =
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
                val dateInt = IntDate(Date(it)).dateInt

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
        launchers.writeOrCreateDiary.launch(IntDate(dateInt))
    }

    private fun writeDiary() {
        val date = IntDate(getCurrentDateInt())

        launchers.writeOrCreateDiary.launch(date)
    }

    private fun getCurrentDateInt(): Int {
        val date = IntDate(Date(System.currentTimeMillis()))
        return date.dateInt
    }

    private fun importDiary(file: File) {
        val refCount = DiaryDatabase.getDatabaseRefCount()

        // the only one reference is for the current activity
        if (refCount > 1) {
            ToastUtils.show(context, getString(R.string.diary_import_ref_count_not_zero_msg, refCount))
            return
        }

        DiaryBaseActivity.showPasswordPromptDialog(requireContext(), file, onSuccess = {
            val diaryActivity = requireActivity() as DiaryBaseActivity
            diaryActivity.diaryDatabaseRef!!.release()
            androidAssert(DiaryDatabase.getDatabaseRefCount() == 0)

            LocalConfig.updatePassword(it)
            BackupManager.backup(DiaryDatabase.internalDatabasePath)
            BackupManager.backup(file)
            file.copyTo(DiaryDatabase.internalDatabasePath, true)

            // substitute old references
            diaryActivity.diaryDatabaseRef = DiaryDatabase.getDatabaseRef()
            diaryActivity.diaryDatabase = diaryActivity.diaryDatabaseRef!!.get()
            diaryDatabase = diaryActivity.diaryDatabase

            ToastUtils.show(requireContext(), R.string.importing_succeeded)

            refreshList()
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshList() {
        refreshItemDataList()
        recyclerViewAdapter.notifyDataSetChanged()
    }

    private fun exportDiary(dest: File) {
        val databaseFile = DiaryDatabase.internalDatabasePath
        Thread {
            try {
                BackupManager.backup(databaseFile)
                if (dest.exists()) {
                    BackupManager.backup(dest)
                }
                FileUtil.copy(databaseFile, dest)
                ToastUtils.show(context, R.string.exporting_succeeded)
            } catch (e: Exception) {
                e.printStackTrace()
                ToastUtils.showError(context, R.string.copying_failed, e)
            }
        }.start()
    }

    private val titleCalendar = Calendar.getInstance()

    private fun makeTitle(date: IntDate): String {
        titleCalendar.apply {
            clear()
            set(date.year, date.month - 1, date.day)
        }
        val weekString = weeks[titleCalendar[Calendar.DAY_OF_WEEK] - 1]
        return "$date $weekString"
    }

    private data class DiaryListItem(
        val diary: Diary,
        var selected: Boolean = false
    )

    private class ListAdapter(
        private val context: Context,
        private val data: List<DiaryListItem>,
        private val makeTitle: (date: IntDate) -> String,
    ) :
        AdapterWithClickListener<ListAdapter.MyViewHolder?>() {

        private class MyViewHolder(val bindings: DiaryItemViewBinding) : RecyclerView.ViewHolder(bindings.root)

        override fun onCreateViewHolder(parent: ViewGroup): MyViewHolder {
            val bindings = DiaryItemViewBinding.inflate(LayoutInflater.from(context), parent, false)
            return MyViewHolder(bindings)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val bindings = holder.bindings
            val item = data[position]
            bindings.dateTv.text = makeTitle(IntDate(item.diary.dateInt))
            bindings.contentTv.text = item.diary.content.limitText(100)
            bindings.root.setBackgroundResource(
                if (item.selected) {
                    R.drawable.clickable_view_selected
                } else {
                    R.drawable.selectable_bg
                }
            )
        }

        override fun getItemCount(): Int {
            return data.size
        }
    }
}
