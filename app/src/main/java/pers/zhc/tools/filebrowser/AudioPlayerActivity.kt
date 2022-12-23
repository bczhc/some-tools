package pers.zhc.tools.filebrowser

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.annotation.StringRes
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.databinding.AudioPlayerActivityBinding
import pers.zhc.tools.utils.ToastUtils
import pers.zhc.tools.utils.androidAssert
import pers.zhc.tools.utils.unreachable
import java.io.File
import java.util.Timer
import java.util.TimerTask

class AudioPlayerActivity : BaseActivity() {
    private var stopProgressUpdater: (() -> Unit)? = null
    private var stopMediaPlayer: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = AudioPlayerActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)
        val controlButton = bindings.controlButton
        val seekBar = bindings.seekBar
        val progressTV = bindings.progressTv

        val path = intent.getStringExtra(EXTRA_FILE_PATH) ?: run {
            throw RuntimeException("No path provided")
        }

        val mp = MediaPlayer.create(this, Uri.fromFile(File(path))).also {
            if (it == null) {
                ToastUtils.show(this, R.string.audio_player_failed_to_play_audio_toast)
                return
            }
            stopMediaPlayer = { it.stop() }
        }
        val duration = mp.duration.also { androidAssert(it != -1) }

        val updateSeekText = {
            val currentPosition = mp.currentPosition
            seekBar.progress =
                (currentPosition.toDouble() / duration.toDouble() * SEEK_BAR_MAX_PROGRESS.toDouble()).toInt()
            progressTV.text = formatSeekText(currentPosition, duration)
        }
        val seekAndUpdateMsg = { position: Int ->
            mp.seekTo(position)
            updateSeekText()
        }
        updateSeekText()

        val startProgressUpdater = {
            val timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    runOnUiThread { updateSeekText() }
                }
            }, 0, 10)
            stopProgressUpdater = {
                timer.cancel()
                timer.purge()
            }
        }

        controlButton.setOnClickListener {
            val state = getControlState(controlButton)

            when (state) {
                State.PLAY -> {
                    // if the seek bar position is at the end, reset it and play the video
                    // if we don't do this, the media player will play 0s (also, immediately)
                    // and then turn into paused state
                    if (seekBar.progress == SEEK_BAR_MAX_PROGRESS) {
                        seekAndUpdateMsg(0)
                    }
                    mp.start()
                    startProgressUpdater()
                }

                State.PAUSE -> {
                    mp.pause()
                    stopProgressUpdater!!()
                }
            }

            controlButton.setText(state.toggle().buttonTextRes)
        }

        seekBar.setOnProgressChangedListener { _, progress, fromUser ->
            if (!fromUser) return@setOnProgressChangedListener
            val seekTime = (progress.toDouble() / SEEK_BAR_MAX_PROGRESS.toDouble() * duration.toDouble()).toInt()
            seekAndUpdateMsg(seekTime)
        }

        mp.setOnCompletionListener {
            controlButton.setText(State.PLAY.buttonTextRes)
            seekBar.progress = 0
            stopProgressUpdater!!()
        }
    }

    private enum class State(@StringRes val buttonTextRes: Int) {
        PLAY(R.string.media_play_button),
        PAUSE(R.string.media_pause_button);

        fun toggle(): State {
            return when (this) {
                PLAY -> PAUSE
                PAUSE -> PLAY
            }
        }
    }

    private fun getControlState(button: Button): State {
        return when (button.text) {
            getString(State.PLAY.buttonTextRes) -> State.PLAY
            getString(State.PAUSE.buttonTextRes) -> State.PAUSE
            else -> unreachable()
        }
    }

    private fun SeekBar.setOnProgressChangedListener(callback: (seekBar: SeekBar, progress: Int, fromUser: Boolean) -> Unit) {
        this.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                callback(seekBar, progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })
    }

    override fun finish() {
        stopProgressUpdater?.invoke()
        stopMediaPlayer?.invoke()
        super.finish()
    }

    private fun formatSeekText(position: Int, duration: Int): String {
        val completeTwoDigits = { num: Int ->
            if (num < 10) "0$num"
            else "$num"
        }
        val format = { time: Int ->
            val hours = (time / 1000) / 3600
            val minutes = (time / 1000) / 60
            val seconds = (time / 1000) % 60
            "${completeTwoDigits(hours)}:${completeTwoDigits(minutes)}:${completeTwoDigits(seconds)}"
        }
        return "${format(position)}/${format(duration)}"
    }

    companion object {
        /**
         * string intent extra
         */
        const val EXTRA_FILE_PATH = "filePath"

        const val SEEK_BAR_MAX_PROGRESS = 1000
    }
}