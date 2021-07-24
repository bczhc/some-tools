package pers.zhc.tools.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
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
        return View.inflate(
            context, if (isIndeterminateMode) {
                R.layout.progress_bar_indeterminate
            } else {
                R.layout.progress_bar
            }, null
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
    fun setProgress(progress: Float) {
        if (!isIndeterminateMode) {
            determinateProgressBar.progressBar.progress = (progress * 100F).toInt()
            determinateProgressBar.textTV.text = context.getString(R.string.progress_tv, progress * 100F)
        }
    }

    fun setIsIndeterminateMode(indeterminateMode: Boolean) {
        this.isIndeterminateMode = indeterminateMode
        this.removeAllViews()
        init()
    }

    class DeterminateProgressBar {
        lateinit var textTV: TextView
        lateinit var progressBar: ProgressBar
        lateinit var titleTV: TextView
    }

    class IndeterminateProgressBar {
        lateinit var titleTV: TextView
    }
}