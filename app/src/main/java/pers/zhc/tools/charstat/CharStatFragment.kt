package pers.zhc.tools.charstat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import pers.zhc.tools.BaseFragment
import pers.zhc.tools.R
import pers.zhc.tools.databinding.CharStatFragmentBinding
import pers.zhc.tools.jni.JNI

class CharStatFragment(private val text: String): BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val bindings = CharStatFragmentBinding.inflate(inflater, container, false)

        val codepointCount = JNI.CharStat.codepointCount(text)
        val graphemeCount = JNI.CharStat.graphemeCount(text)

        bindings.codepointTv.text = getString(R.string.char_stat_codepoint_count_tv, codepointCount)
        bindings.graphemeTv.text = getString(R.string.char_stat_grapheme_cluster_count_tv, graphemeCount)

        return bindings.root
    }
}