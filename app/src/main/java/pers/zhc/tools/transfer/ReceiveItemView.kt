package pers.zhc.tools.transfer

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import pers.zhc.tools.R
import pers.zhc.tools.databinding.TransferErrorReceivingItemBinding
import pers.zhc.tools.databinding.TransferReceivedItemBinding
import pers.zhc.tools.views.WrapLayout

class ReceiveItemView : WrapLayout {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    private var state = State.UNINIT

    private val successViewBindings by lazy { TransferReceivedItemBinding.inflate(LayoutInflater.from(context)) }
    private val failureViewBindings by lazy { TransferErrorReceivingItemBinding.inflate(LayoutInflater.from(context)) }

    private val successView by lazy { successViewBindings.root }
    private val failureView by lazy { failureViewBindings.root }

    private val recvTimeTV by lazy { successViewBindings.receivingTimeTv }
    private val recvSizeTV by lazy { successViewBindings.receivingSizeTv }
    private val recvTypeTV by lazy { successViewBindings.receivingTypeTv }

    private val errorMsgTV by lazy { failureViewBindings.errMsgTv }
    private val errorTimeTV by lazy { failureViewBindings.errorTimeTv }

    init {
        setBackgroundResource(R.drawable.selectable_bg)
    }

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
