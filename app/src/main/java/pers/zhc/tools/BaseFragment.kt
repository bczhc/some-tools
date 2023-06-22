package pers.zhc.tools

import androidx.fragment.app.Fragment

open class BaseFragment : Fragment() {
    @Suppress("PropertyName")
    protected val TAG: String = this.javaClass.name
}
