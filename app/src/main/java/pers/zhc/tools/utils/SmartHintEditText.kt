package pers.zhc.tools.utils

import android.content.Context
import android.util.AttributeSet
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import pers.zhc.tools.R

/**
 * @author bczhc
 */
class SmartHintEditText : TextInputLayout {
    private var mET: EditText? = null

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        mET = EditText(context)
        if (attrs != null) {
            val inputType = attrs.getAttributeIntValue("android", "inputStyle", 0x20001/* textMultiLine */)
            val ta = context.obtainStyledAttributes(attrs, R.styleable.SmartHintEditText)
            val hint = ta.getString(R.styleable.SmartHintEditText_hint)
            val text = ta.getText(R.styleable.SmartHintEditText_text)
            val textSize = ta.getDimension(R.styleable.SmartHintEditText_textSize, -1F)
            ta.recycle()
            if (hint != null) mET!!.hint = hint
            if (text != null) mET!!.setText(text)
            mET!!.inputType = inputType
            if (textSize != -1F) {
                mET!!.textSize = textSize
            }
        }
        val measureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        mET!!.measure(measureSpec, measureSpec)
        val measuredWidth = mET!!.measuredWidth

        this.addView(mET)

        this.minimumWidth = measuredWidth
    }
}