package pers.zhc.tools.fdb

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import kotlinx.android.synthetic.main.fdb_panel_btn_view.view.*
import kotlinx.android.synthetic.main.fdb_panel_view.view.*
import pers.zhc.tools.R
import pers.zhc.tools.utils.DisplayUtil

/**
 * @author bczhc
 */
class PanelRL : RelativeLayout {
    private var mOnTouchListener: OnTouchListener? = null
    private lateinit var mPanelLL: LinearLayout
    private lateinit var btnStrings: Array<String>
    private lateinit var mImageView: ImageView
    private var onButtonTouchedListener: ListenerFunction? = null

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        context!!
        init()
    }

    private fun init() {
        // initialize image icon
        mImageView = ImageView(context)
        val iconDrawable = ResourcesCompat.getDrawable(this.resources, R.drawable.ic_db, null)
        val d = DisplayUtil.dip2px(context, 35F)
        mImageView.layoutParams = ViewGroup.LayoutParams(d, d)
        mImageView.setImageDrawable(iconDrawable)
        mImageView.setOnClickListener {
            onButtonTouched(MODE_IMAGE_ICON, 0)
        }

        // get data string array
        btnStrings = context.resources.getStringArray(R.array.btn_string)

        // initialize buttons LinearLayout
        mPanelLL = View.inflate(context, R.layout.fdb_panel_view, null).panel_ll!!
        btnStrings.forEachIndexed { index, s ->
            val textView = View.inflate(context, R.layout.fdb_panel_btn_view, null).btn_tv!!
            textView.text = s
            mPanelLL.addView(textView)
            textView.setOnClickListener {
                onButtonTouched(MODE_PANEL, index)
            }
        }

        changeMode(MODE_IMAGE_ICON)
    }


    private fun onButtonTouched(mode: Int, buttonIndex: Int) {
        onButtonTouchedListener?.invoke(mode, buttonIndex)
    }

    fun setOnButtonTouchedListener(listener: ListenerFunction?) {
        this.onButtonTouchedListener = listener
    }

    fun changeMode(mode: Int) {
        this.removeAllViews()
        when (mode) {
            MODE_IMAGE_ICON -> {
                this.addView(mImageView)
            }
            MODE_PANEL -> {
                this.addView(mPanelLL)
            }
        }
    }

    fun getImageView(): ImageView {
        return this.mImageView
    }

    fun getPanelLL(): LinearLayout {
        return this.mPanelLL
    }

    fun getPanelTextView(index: Int): TextView {
        return this.mPanelLL.getChildAt(index) as TextView
    }

    override fun setOnTouchListener(l: OnTouchListener?) {
        this.mOnTouchListener = l
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        mOnTouchListener?.onTouch(this, ev)
        return super.onInterceptTouchEvent(ev)
    }

    fun setPanelColor(color: Int) {
        val childCount = mPanelLL.childCount
        for (i in (0 until childCount)) {
            val child = mPanelLL.getChildAt(i) as TextView
            child.setBackgroundColor(color)
        }
    }

    fun setPanelTextColor(color: Int) {
        val childCount = mPanelLL.childCount
        for (i in (0 until childCount)) {
            val child = mPanelLL.getChildAt(i) as TextView
            child.setTextColor(color)
        }
    }

    companion object {
        const val MODE_IMAGE_ICON = 0
        const val MODE_PANEL = 1
    }
}

typealias ListenerFunction = (mode: Int, buttonIndex: Int) -> Unit