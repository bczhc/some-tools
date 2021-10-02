package pers.zhc.tools

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup

/**
 * @author bczhc
 */
abstract class BaseViewGroup: ViewGroup {
    @Suppress("PropertyName")
    protected val TAG = this::class.java

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
}