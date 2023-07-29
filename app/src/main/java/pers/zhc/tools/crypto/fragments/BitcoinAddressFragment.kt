package pers.zhc.tools.crypto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import pers.zhc.tools.BaseFragment
import pers.zhc.tools.databinding.BitcoinAddressFragmentBinding

class BitcoinAddressFragment: BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val bindings = BitcoinAddressFragmentBinding.inflate(inflater, container, false)
        return bindings.root
    }
}
