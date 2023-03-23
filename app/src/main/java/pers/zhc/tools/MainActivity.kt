package pers.zhc.tools

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Base64
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonSyntaxException
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pers.zhc.tools.MyApplication.Companion.HTTP_CLIENT_DEFAULT
import pers.zhc.tools.app.ActivityItem
import pers.zhc.tools.app.AppMenuAdapter
import pers.zhc.tools.app.SmallToolsListActivity
import pers.zhc.tools.app.TestListActivity
import pers.zhc.tools.bus.BusQueryMainActivity
import pers.zhc.tools.coursetable.CourseTableMainActivity
import pers.zhc.tools.databinding.GitLogViewBinding
import pers.zhc.tools.databinding.GithubActionDownloadViewBinding
import pers.zhc.tools.databinding.ToolsActivityMainBinding
import pers.zhc.tools.diary.DiaryMainActivity
import pers.zhc.tools.email.EmailMainActivity
import pers.zhc.tools.fdb.FdbMainActivity
import pers.zhc.tools.fourierseries.FourierSeriesActivity
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.magic.FileListActivity
import pers.zhc.tools.note.NotesActivity
import pers.zhc.tools.stcflash.FlashMainActivity
import pers.zhc.tools.tasknotes.TaskNotesMainActivity
import pers.zhc.tools.transfer.TransferMainActivity
import pers.zhc.tools.utils.*
import pers.zhc.tools.words.WordsMainActivity
import pers.zhc.tools.wubi.WubiInputMethodActivity
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.system.exitProcess

/**
 * @author bczhc
 */
class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = ToolsActivityMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)
        val recyclerView = bindings.recyclerView

        recyclerView.setLinearLayoutManager()
        recyclerView.adapter = AppMenuAdapter(this, getActivities())
    }

    private fun getActivities(): List<ActivityItem> {
        return listOf(
            ActivityItem(R.string.app_menu_test, TestListActivity::class.java),
            ActivityItem(R.string.app_menu_little_tools, SmallToolsListActivity::class.java),
            ActivityItem(R.string.floating_drawing_board, FdbMainActivity::class.java),
            ActivityItem(R.string.notes, NotesActivity::class.java),
            ActivityItem(R.string.diary, DiaryMainActivity::class.java),
            ActivityItem(R.string.wubi_input_method, WubiInputMethodActivity::class.java),
            ActivityItem(R.string.stc_flash, FlashMainActivity::class.java),
            ActivityItem(R.string.bus_query_label, BusQueryMainActivity::class.java),
            ActivityItem(R.string.magic_label, FileListActivity::class.java),
            ActivityItem(R.string.words_label, WordsMainActivity::class.java),
            ActivityItem(R.string.transfer_label, TransferMainActivity::class.java),
            ActivityItem(R.string.email_label, EmailMainActivity::class.java),
            ActivityItem(R.string.fourier_series_label, FourierSeriesActivity::class.java),
            ActivityItem(R.string.task_notes_label, TaskNotesMainActivity::class.java),
            ActivityItem(R.string.course_table_label, CourseTableMainActivity::class.java)
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> {
                startActivity(Intent(this, Settings::class.java))
            }

            R.id.update -> {
                updateAction()
            }

            R.id.git_log -> {
                showGitLogDialog()
            }

            R.id.switch_themes -> {
                if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                } else if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
            }

            R.id.wake_lock_acquire -> {
                acquireWakeLockAction()
            }

            R.id.wake_lock_release -> {
                releaseWakeLockAction()
            }

            R.id.exit -> {
                exitProcess(0)
            }
        }
        return true
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLockAction() {
        if (MyApplication.wakeLock == null) {
            val powerManager = applicationContext.getSystemService(POWER_SERVICE) as PowerManager
            MyApplication.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
        }
        if (MyApplication.wakeLock!!.isHeld) {
            ToastUtils.show(this, R.string.wake_lock_already_held_toast)
            return
        }
        MyApplication.wakeLock!!.acquire()
        ToastUtils.show(this, R.string.wake_lock_held_success)
    }

    private fun releaseWakeLockAction() {
        if (MyApplication.wakeLock == null || !MyApplication.wakeLock!!.isHeld) {
            ToastUtils.show(this, R.string.wake_lock_no_held)
            return
        }
        MyApplication.wakeLock!!.release()
        ToastUtils.show(this, R.string.wake_lock_release_success)
    }

    private fun showGitLogDialog() {
        val commitLogSplit = BuildConfig.commitLogEncodedSplit
        val base64Encoded = commitLogSplit.joinToString(separator = "")
        val gitLog = JNI.Lzma.decompress(Base64.decode(base64Encoded, Base64.DEFAULT)).toString(StandardCharsets.UTF_8)

        val bindings = GitLogViewBinding.inflate(layoutInflater)
        bindings.tv.text = gitLog

        Dialog(this).apply {
            setContentView(bindings.root)
        }.show()
    }

    private fun updateAction() {
        showGithubActionDownloadDialog(this)
    }

    private fun showGithubActionDownloadDialog(context: Context) {
        val download = { item: Commit, abi: String ->

            val apk = item.apks.find { it.abi == abi }!!

            val storagePath = Common.getAppMainExternalStoragePath(context)
            val updateDir = File(storagePath, "update")
            updateDir.requireMkdirs()
            val localFile = File(updateDir, "some-tools.apk")

            lifecycleScope.launch {
                val url = Url("${Info.staticResourceRootURL}/apks/some-tools/${item.commitHash}/${apk.name}")
                DownloadUtils.startDownloadWithDialog(context, url, localFile)
                withContext(Dispatchers.Default) {
                    Common.installApk(context, localFile)
                }
            }
        }

        val onItemClicked = { item: Commit, upperDialog: Dialog ->
            // check abi
            val supportedAbis = Build.SUPPORTED_ABIS
            val foundAbi = item.apks.map { it.abi }.find {
                supportedAbis.contains(it)
            }
            if (foundAbi == null) {
                ToastUtils.show(context, R.string.app_unsupported_abi)
            } else {
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.app_download_confirmation_dialog_title)
                    .setPositiveButton(R.string.confirm) { _, _ ->
                        download(item, foundAbi)
                        upperDialog.dismiss()
                    }
                    .setNeutralButton("ABI") { _, _ ->

                        val abis = item.apks.map { it.abi }.toTypedArray()
                        val abiIndex = abis.indexOfFirst { it == foundAbi }
                        MaterialAlertDialogBuilder(context)
                            .setTitle("ABI")
                            .defaultNegativeButton()
                            .setPositiveAction { self, _ ->
                                val selectedAbi = abis[(self as AlertDialog).listView.checkedItemPosition]
                                download(item, selectedAbi)
                                upperDialog.dismiss()
                            }
                            .apply {
                                setSingleChoiceItems(abis, abiIndex, null)
                            }
                            .show()

                    }
                    .setMessage(item.commitMessage)
                    .show()
            }

        }

        val showDownloadList = { infoJson: String ->
            val commits = try {
                ArrayList(
                    MyApplication.GSON.fromJson(infoJson, JsonArray::class.java)
                        .map { MyApplication.GSON.fromJson(it, Commit::class.java) }
                        .reversed()
                )
            } catch (_: JsonSyntaxException) {
                null
            }

            if (commits == null) {
                ToastUtils.show(context, R.string.getting_information_failed)
            } else {
                val inflate = View.inflate(context, R.layout.github_action_download_view, null)
                val bindings = GithubActionDownloadViewBinding.bind(inflate)
                val recyclerView = bindings.recyclerView
                recyclerView.layoutManager = LinearLayoutManager(context)
                val adapter = GithubActionDownloadListAdapter(context, commits)
                recyclerView.adapter = adapter

                val dialog = Dialog(context)
                DialogUtils.setDialogAttr(
                    dialog,
                    width = ViewGroup.LayoutParams.MATCH_PARENT,
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                )
                dialog.setContentView(inflate)
                dialog.show()

                adapter.setOnItemClickListener { position, _ ->
                    onItemClicked(commits[position], dialog)
                }
            }
        }

        val progressDialog = ProgressDialog(context)
        val progressView = progressDialog.getProgressView()
        progressView.setIsIndeterminateMode(true)
        progressView.setTitle(context.getString(R.string.getting_information))
        DialogUtils.setDialogAttr(
            progressDialog,
            width = ViewGroup.LayoutParams.MATCH_PARENT,
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        )
        progressDialog.show()
        lifecycleScope.launch(Dispatchers.IO) {
            val info = fetchInfo()
            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                if (info == null) {
                    ToastUtils.show(context, R.string.getting_information_failed)
                    return@withContext
                }
                showDownloadList(info)
            }
        }
    }

    private suspend fun fetchInfo(): String? {
        val url = Url(Info.staticResourceRootURL + "/apks/some-tools/files.json")
        runCatching {
            HTTP_CLIENT_DEFAULT.get(url)
                .bodyAsText()
        }.onSuccess { return it }
        return null
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, R.anim.fade_out)
    }

    private class GithubActionDownloadListAdapter(private val context: Context, private val data: ArrayList<Commit>) :
        AdapterWithClickListener<GithubActionDownloadListAdapter.MyViewHolder>() {
        class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val commitInfoTV = view.findViewById<TextView>(R.id.commit_info_tv)
        }

        override fun onCreateViewHolder(parent: ViewGroup): MyViewHolder {
            val inflate = LayoutInflater.from(context).inflate(R.layout.github_action_download_item, parent, false)
            return MyViewHolder(inflate)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.commitInfoTV.text = data[position].commitMessage
        }

        override fun getItemCount(): Int {
            return data.size
        }
    }

    private data class Commit(
        val commitHash: String,
        val commitMessage: String,
        val apks: ArrayList<Apk>,
    )

    private data class Apk(
        val abi: String,
        val sha1: String,
        val name: String,
    )
}
