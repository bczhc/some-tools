package pers.zhc.tools.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.android.synthetic.main.progress_bar.view.*
import kotlinx.android.synthetic.main.progress_bar_indeterminate.view.*
import pers.zhc.tools.R

/**
 * @author bczhc
 */
class ProgressView : RelativeLayout {
    private var isIndeterminateMode = false
    private val determinateProgressBar = DeterminateProgressBar()
    private val indeterminateProgressBar = IndeterminateProgressBar()

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        val inflate = getProgressBarRootView()
        if (isIndeterminateMode) {
            indeterminateProgressBar.titleTV = inflate.title!!
        } else {
            determinateProgressBar.titleTV = inflate.progress_bar_title!!
            determinateProgressBar.progressBar = inflate.progress_bar!!
            determinateProgressBar.textTV = inflate.progress_tv!!
        }
        this.addView(inflate)
    }

    private fun getProgressBarRootView(): View {
        return LayoutInflater.from(context).inflate(
            if (isIndeterminateMode) {
                R.layout.progress_bar_indeterminate
            } else {
                R.layout.progress_bar
            }, this, false
        )!!
    }

    fun setTitle(title: String) {
        if (isIndeterminateMode) {
            indeterminateProgressBar.titleTV.text = title
        } else {
            determinateProgressBar.titleTV.text = title
        }
    }

    /**
     * [0, 1]
     */
    fun setProgressAndText(progress: Float) {
        if (!isIndeterminateMode) {
            determinateProgressBar.progressBar.setProgressCompat((progress * 100F).toInt(), true)
            determinateProgressBar.textTV.text = context.getString(R.string.progress_tv, progress * 100F)
        }
    }

    fun setProgress(progress: Float) {
        if (!isIndeterminateMode) {
            determinateProgressBar.progressBar.setProgressCompat((progress * 100F).toInt(), true)
        }
    }

    fun setText(text: String) {
        if (!isIndeterminateMode) {
            determinateProgressBar.textTV.text = text
        }
    }

    fun setIsIndeterminateMode(indeterminateMode: Boolean) {
        this.isIndeterminateMode = indeterminateMode
        this.removeAllViews()
        init()
    }

    class DeterminateProgressBar {
        lateinit var textTV: TextView
        lateinit var progressBar: LinearProgressIndicator
        lateinit var titleTV: TextView
    }

    class IndeterminateProgressBar {
        lateinit var titleTV: TextView
    }
}