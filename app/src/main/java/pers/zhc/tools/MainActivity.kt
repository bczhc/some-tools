package pers.zhc.tools

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import pers.zhc.tools.app.ActivityItem
import pers.zhc.tools.app.AppMenuAdapter
import pers.zhc.tools.app.SmallToolsListActivity
import pers.zhc.tools.app.TestListActivity
import pers.zhc.tools.bus.BusQueryMainActivity
import pers.zhc.tools.colorpicker.ScreenColorPickerMainActivity
import pers.zhc.tools.databinding.GitLogViewBinding
import pers.zhc.tools.databinding.ToolsActivityMainBinding
import pers.zhc.tools.diary.DiaryMainActivity
import pers.zhc.tools.email.EmailMainActivity
import pers.zhc.tools.fdb.FdbMainActivity
import pers.zhc.tools.fourierseries.FourierSeriesActivity
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.magic.FileListActivity
import pers.zhc.tools.main.MainActivity.Companion.showGithubActionDownloadDialog
import pers.zhc.tools.note.NotesActivity
import pers.zhc.tools.stcflash.FlashMainActivity
import pers.zhc.tools.tasknotes.TaskNotesMainActivity
import pers.zhc.tools.transfer.TransferMainActivity
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.setLinearLayoutManager
import pers.zhc.tools.words.WordsMainActivity
import pers.zhc.tools.wubi.WubiInputMethodActivity
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
            ActivityItem(R.string.screen_color_picker_label, ScreenColorPickerMainActivity::class.java),
            ActivityItem(R.string.task_notes_label, TaskNotesMainActivity::class.java),
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

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, R.anim.fade_out)
    }
}
