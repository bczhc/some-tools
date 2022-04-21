package pers.zhc.tools.transfer

import android.content.Context
import android.util.AttributeSet
import android.view.View
import kotlinx.android.synthetic.main.transfer_error_receiving_item.view.*
import kotlinx.android.synthetic.main.transfer_received_item.view.*
import pers.zhc.tools.R
import pers.zhc.tools.views.WrapLayout

class ReceiveItemView : WrapLayout {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    private var state = State.UNINIT

    private val successView by lazy { View.inflate(context, R.layout.transfer_received_item, null) }
    private val failureView by lazy { View.inflate(context, R.layout.transfer_error_receiving_item, null) }

    private val recvTimeTV by lazy { successView.receiving_time_tv!! }
    private val recvSizeTV by lazy { successView.receiving_size_tv!! }
    private val recvTypeTV by lazy { successView.receiving_type_tv!! }

    private val errorMsgTV by lazy { failureView.err_msg_tv!! }
    private val errorTimeTV by lazy { failureView.error_time_tv!! }

    fun setSuccess(time: String, size: String, type: String) {
        if (state != State.SUCCESS) {
            this.setView(successView)
        }
        recvTimeTV.text = context.getString(R.string.transfer_receiving_time_tv, time)
        recvSizeTV.text = context.getString(R.string.transfer_receiving_size_tv, size)
        recvTypeTV.text = context.getString(R.string.transfer_receiving_type_tv, type)
        state = State.SUCCESS
    }

    fun setFailure(time: String, msg: String) {
        if (state != State.FAILURE) {
            this.setView(failureView)
        }
        errorTimeTV.text = context.getString(R.string.time_is_, time)
        errorMsgTV.text = msg
        state = State.FAILURE
    }

    private enum class State {
        SUCCESS, FAILURE, UNINIT
    }
}
