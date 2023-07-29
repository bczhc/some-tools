package pers.zhc.tools.crypto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pers.zhc.tools.BaseFragment
import pers.zhc.tools.R
import pers.zhc.tools.databinding.BitcoinAddressFragmentBinding
import pers.zhc.tools.databinding.BitcoinAddressListItemBinding
import pers.zhc.tools.jni.JNI
import pers.zhc.tools.utils.AdapterWithClickListener
import pers.zhc.tools.utils.setLinearLayoutManager
import pers.zhc.tools.utils.setUpFastScroll

class BitcoinAddressFragment : BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val bindings = BitcoinAddressFragmentBinding.inflate(inflater, container, false)
        setUpUi(bindings)
        return bindings.root
    }

    private fun setUpUi(bindings: BitcoinAddressFragmentBinding) {
        val context = requireContext()

        val addressList = mutableListOf<Address>()
        val listAdapter = ListAdapter(addressList)
        bindings.recyclerView.apply {
            setUpFastScroll(context)
            setLinearLayoutManager()
            adapter = listAdapter
        }

        bindings.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.create_new -> {
                    addressList += createAddress()
                    listAdapter.notifyItemInserted(addressList.size)
                }

                else -> return@setOnMenuItemClickListener false
            }
            false
        }

        listAdapter.setOnItemClickListener { position, _ ->
            MaterialAlertDialogBuilder(context)
                .setMessage(addressList[position].privateKey)
                .show()
        }
    }

    private fun createAddress(): Address {
        val privateKey = JNI.Bitcoin.generateKey()
        val address = JNI.Bitcoin.privateKeyToAddress(privateKey)
        return Address(privateKey, address)
    }

    data class Address(
        val privateKey: String,
        val address: String,
    )

    class ListAdapter(private val data: MutableList<Address>) : AdapterWithClickListener<ListAdapter.MyHolder>() {
        class MyHolder(val bindings: BitcoinAddressListItemBinding) : ViewHolder(bindings.root)

        override fun onCreateViewHolder(parent: ViewGroup): MyHolder {
            val bindings = BitcoinAddressListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return MyHolder(bindings)
        }

        override fun onBindViewHolder(holder: MyHolder, position: Int) {
            val address = data[position].address
            holder.bindings.text1.text = address
        }

        override fun getItemCount(): Int {
            return data.size
        }
    }
}
