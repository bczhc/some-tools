package pers.zhc.tools.fdb

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import pers.zhc.tools.R
import pers.zhc.tools.utils.DisplayUtil
import pers.zhc.tools.utils.runOnUiThread
import pers.zhc.tools.utils.schedule
import java.util.*

/**
 * @author bczhc
 */
class PanelRL : RelativeLayout {
    private var mOnTouchListener: OnTouchListener? = null
    private lateinit var mPanelLL: LinearLayout
    private lateinit var btnStrings: Array<String>
    private lateinit var mImageView: ImageView
    var onButtonClickedListener: ListenerFunction? = null
    var onButtonLongClickedListener: ListenerFunction? = null
    private var panelColor = 0
    private var panelTextColor = 0

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

        // get data string array
        btnStrings = context.resources.getStringArray(R.array.btn_string)

        // initialize buttons LinearLayout
        mPanelLL = View.inflate(context, R.layout.fdb_panel_view, null).findViewById(R.id.panel_ll)
        btnStrings.forEachIndexed { index, s ->
            val textView = View.inflate(context, R.layout.fdb_panel_btn_view, null).findViewById<TextView>(R.id.btn_tv)
            textView.text = s
            mPanelLL.addView(textView)

            val getOnTouchListener = {
                val clickOffsetTolerance = 1F
                val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
                var prevRawX = 0F
                var prevRawY = 0F
                var longClickTimer: Timer? = null
                var longClickDone = false

                val getMode = { v: View ->
                    if (v === mImageView) {
                        MODE_IMAGE_ICON
                    } else {
                        MODE_PANEL
                    }
                }

                ({ v: View, ev: MotionEvent ->
                    when (ev.action) {
                        MotionEvent.ACTION_DOWN -> {
                            longClickDone = false
                            prevRawX = ev.rawX
                            prevRawY = ev.rawY
                            if (longClickTimer == null) {
                                longClickTimer = Timer().also {
                                    it.schedule({
                                        if (ev.rawX - prevRawX <= clickOffsetTolerance
                                            && ev.rawY - prevRawY <= clickOffsetTolerance
                                        ) {
                                            longClickTimer = null
                                            longClickDone = true
                                            runOnUiThread {
                                                onButtonLongClicked(getMode(v), index)
                                            }
                                        }
                                    }, longPressTimeout)
                                }
                            }
                        }

                        MotionEvent.ACTION_UP -> {
                            longClickTimer?.cancel()
                            longClickTimer = null
                            if (!longClickDone && ev.rawX - prevRawX <= clickOffsetTolerance
                                && ev.rawY - prevRawY <= clickOffsetTolerance
                            ) {
                                // click
                                onButtonClicked(getMode(v), index)
                            }
                        }

                        else -> {
                        }
                    }
                    true
                })
            }

            @Suppress("ClickableViewAccessibility")
            mImageView.setOnTouchListener(getOnTouchListener())
            @Suppress("ClickableViewAccessibility")
            textView.setOnTouchListener(getOnTouchListener())
        }

        changeMode(MODE_IMAGE_ICON)
    }


    private fun onButtonClicked(mode: Int, buttonIndex: Int) {
        onButtonClickedListener?.invoke(mode, buttonIndex)
    }

    private fun onButtonLongClicked(mode: Int, buttonIndex: Int) {
        onButtonLongClickedListener?.invoke(mode, buttonIndex)
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
        ev!!
        mOnTouchListener?.onTouch(this, ev)
        return false
    }

    fun setPanelColor(color: Int) {
        panelColor = color
        val childCount = mPanelLL.childCount
        for (i in (0 until childCount)) {
            val child = mPanelLL.getChildAt(i) as TextView
            child.setBackgroundColor(color)
        }
    }

    fun setPanelTextColor(color: Int) {
        panelTextColor = color
        val childCount = mPanelLL.childCount
        for (i in (0 until childCount)) {
            val child = mPanelLL.getChildAt(i) as TextView
            child.setTextColor(color)
        }
    }

    fun getPanelColor(): Int {
        return panelColor
    }

    fun getPanelTextColor(): Int {
        return panelTextColor
    }

    companion object {
        const val MODE_IMAGE_ICON = 0
        const val MODE_PANEL = 1
    }
}

typealias ListenerFunction = (mode: Int, buttonIndex: Int) -> Unit
