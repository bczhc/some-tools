package pers.zhc.tools.views

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.material_input_layout.view.*
import pers.zhc.tools.R
import pers.zhc.tools.utils.DisplayUtil
import kotlin.math.max

/**
 * @author bczhc
 */
class SmartHintEditText : WrapLayout {
    private lateinit var mET: EditText
    val editText get() = mET

    private lateinit var inputLayout: TextInputLayout
    val textInputLayout get() = inputLayout

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init(attrs)
    }

    private fun measureMinimumSize(et: EditText, hint: String?, text: String?): Point {
        val measure = { a: EditText ->
            val measureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            a.measure(measureSpec, measureSpec)
            val editTextWidth = a.measuredWidth
            val editTextHeight = a.measuredHeight
            Point(editTextWidth, editTextHeight)
        }

        hint?.let { et.setText(it) }
        val p1 = measure(et)
        text?.let { et.setText(it) }
        val p2 = measure(et)

        et.setText("")

        return Point(max(p1.x, p2.x), max(p1.y, p2.y))
    }

    private fun init(attrs: AttributeSet?) {
        val inflate = View.inflate(context, R.layout.material_input_layout, null).layout!!
        this.setView(inflate)
        inputLayout = inflate

        val til = inflate.layout!!
        mET = inflate.edit_text!!

        if (attrs != null) {
            val inputType = attrs.getAttributeIntValue(
                "http://schemas.android.com/apk/res/android",
                "inputType",
                0x20001 /* textMultiLine */
            )
            val ta = context.obtainStyledAttributes(attrs, R.styleable.SmartHintEditText)
            val hint = ta.getString(R.styleable.SmartHintEditText_hint)
            val text = ta.getText(R.styleable.SmartHintEditText_text)
            val textSize = ta.getDimensionPixelSize(R.styleable.SmartHintEditText_textSize, -1)
            ta.recycle()
            mET.inputType = inputType
            if (textSize != -1) {
                mET.textSize = DisplayUtil.px2sp(context, textSize.toFloat()).toFloat()
            }

            val minimumSize = measureMinimumSize(mET, hint, text?.toString())
            mET.minWidth = minimumSize.x
            mET.minHeight = minimumSize.y

            hint?.let { til.hint = it }
            text?.let { mET.setText(it) }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            mET.width = MeasureSpec.getSize(widthMeasureSpec)
        }
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
            mET.height = MeasureSpec.getSize(heightMeasureSpec)
        }
    }
}