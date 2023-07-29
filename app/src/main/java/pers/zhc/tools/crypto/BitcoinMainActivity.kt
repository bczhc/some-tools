package pers.zhc.tools.crypto

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.crypto.fragments.BitcoinAddressFragment
import pers.zhc.tools.databinding.BitcoinActivityMainBinding

class BitcoinMainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = BitcoinActivityMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val updateFragment = { f: Fragment ->
            val containerId = bindings.fragmentContainer.id
            supportFragmentManager.commit {
                replace(containerId, f)
            }
        }
        val showInitialFragment = {
            updateFragment(BitcoinAddressFragment())
        }.also { it() }

        val bottomNavigator = bindings.bottomNavigator
        bottomNavigator.setOnItemSelectedListener {
            when (it.itemId) {
                bottomNavigator.selectedItemId -> return@setOnItemSelectedListener false
                R.id.address -> {
                    showInitialFragment()
                }

                else -> return@setOnItemSelectedListener false
            }
            true
        }
    }
}
