package pers.zhc.tools.charucd

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import com.google.android.material.progressindicator.LinearProgressIndicator
import pers.zhc.tools.R
import pers.zhc.tools.databinding.CharUcdProgressViewBinding
import pers.zhc.tools.utils.setBaseLayoutSizeMW
import pers.zhc.tools.views.WrapLayout

/**
 * @author bczhc
 */
class ParseProgressView : WrapLayout {
    private lateinit var actionTV: TextView
    private lateinit var progressTV: TextView
    private lateinit var progressBar: LinearProgressIndicator

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        val bindings = CharUcdProgressViewBinding.inflate(LayoutInflater.from(context))
        val inflate = bindings.root.apply {
            setBaseLayoutSizeMW()
        }
        actionTV = bindings.actionTv
        progressTV = bindings.progressTv
        progressBar = bindings.progressBar
        this.setView(inflate)
    }

    fun setActionText(text: String) {
        actionTV.text = text
    }

    /**
     * [progress] is in [0, 1]
     */
    fun setProgressAndTitle(progress: Float, animated: Boolean = true) {
        progressTV.text = context.getString(R.string.percentage, progress * 100F)
        progressBar.setProgressCompat((progress * 100F).toInt(), animated)
    }

    fun setProgressTitle(s: String) {
        progressTV.text = s
    }

    fun setProgress(progress: Float, animated: Boolean = true) {
        progressBar.setProgressCompat((progress * 100F).toInt(), animated)
    }
}