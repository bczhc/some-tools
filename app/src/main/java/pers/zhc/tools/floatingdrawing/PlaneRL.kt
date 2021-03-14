package pers.zhc.tools.floatingdrawing

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.content.res.ResourcesCompat
import pers.zhc.tools.R

/**
 * @author bczhc
 */
class PlaneRL : RelativeLayout {
    private val mContext: Context
    private var onButtonTouchedListener: OnButtonTouchedInterface? = null

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        context!!
        this.mContext = context
        init()
    }

    private fun init() {
        val iv = ImageView(mContext)
        val proportionX = 75.toFloat() / 720.toFloat()
        val proportionY = 75.toFloat() / 1360.toFloat()
        val iconDrawable = ResourcesCompat.getDrawable(this.resources, R.drawable.ic_db, null)
        iv.layoutParams = ViewGroup.LayoutParams((width * proportionX).toInt(), (height * proportionY).toInt())
        iv.setImageDrawable(iconDrawable)

        this.addView(iv)
    }

    fun onButtonTouched(buttonIndex: Int) {
        onButtonTouchedListener?.onTouched(buttonIndex)
    }

    fun setOnButtonTouchedListener(listener: OnButtonTouchedInterface?) {
        this.onButtonTouchedListener = listener
    }

    interface OnButtonTouchedInterface {
        fun onTouched(buttonIndex: Int)
    }
}