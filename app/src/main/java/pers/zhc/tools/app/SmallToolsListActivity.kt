package pers.zhc.tools.app

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.charsplit.CharSplitActivity
import pers.zhc.tools.charstat.CharStatActivity
import pers.zhc.tools.charucd.CharLookupActivity
import pers.zhc.tools.clipboard.Clip
import pers.zhc.tools.databinding.ToolsActivityMainBinding
import pers.zhc.tools.pi.Pi
import pers.zhc.tools.test.RegExpTest
import pers.zhc.tools.test.SysInfo
import pers.zhc.tools.test.UnicodeTable
import pers.zhc.tools.test.toast.ToastTest
import pers.zhc.tools.test.typetest.TypeTest
import pers.zhc.tools.texteditor.MainActivity

/**
 * @author bczhc
 */
class SmallToolsListActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = ToolsActivityMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val recyclerView = bindings.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        val activities = listOf(
            ActivityItem(R.string.generate_pi, Pi::class.java),
            ActivityItem(R.string.toast, ToastTest::class.java),
            ActivityItem(R.string.put_in_clipboard, Clip::class.java),
            ActivityItem(R.string.type_test, TypeTest::class.java),
            ActivityItem(R.string.regular_expression_test, RegExpTest::class.java),
            ActivityItem(R.string.sys_info_label, SysInfo::class.java),
            ActivityItem(R.string.unicode_table_label, UnicodeTable::class.java),
            ActivityItem(R.string.chars_splitter_label, CharSplitActivity::class.java),
            ActivityItem(R.string.char_ucd_lookup_activity_label, CharLookupActivity::class.java),
            ActivityItem(R.string.char_stat_label, CharStatActivity::class.java),
            ActivityItem(R.string.text_editor_label, MainActivity::class.java),
            ActivityItem(R.string.digest_label, pers.zhc.tools.digest.MainActivity::class.java)
        )

        recyclerView.adapter = AppMenuAdapter(this, activities)
    }
}
