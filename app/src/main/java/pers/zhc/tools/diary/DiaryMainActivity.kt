package pers.zhc.tools.diary

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.Toolbar
import pers.zhc.tools.R
import pers.zhc.tools.databinding.DiaryMainActivityBinding
import pers.zhc.tools.diary.fragments.AttachmentFragment
import pers.zhc.tools.diary.fragments.DiaryFragment
import pers.zhc.tools.diary.fragments.FileLibraryFragment
import pers.zhc.tools.utils.ToastUtils

/**
 * @author bczhc
 */
class DiaryMainActivity : DiaryBaseActivity() {
    private var drawerToggle: ActionBarDrawerToggle? = null
    private var drawerArrowDrawable: DrawerArrowDrawable? = null
    private lateinit var bindings: DiaryMainActivityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private fun load() {
        bindings = DiaryMainActivityBinding.inflate(layoutInflater)
        setContentView(bindings.root)
        invalidateOptionsMenu()
        initDrawerLayout()
    }

    override fun onDatabaseValidated() {
        load()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle?.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle?.onConfigurationChanged(newConfig)
    }

    private fun initDrawerLayout() {
        val fragmentManager = supportFragmentManager

        val drawerLayout = bindings.drawerLayout
        val navigationView = bindings.navigationView.also { it.setCheckedItem(R.id.diary) }

        // the initial diary view
        fragmentManager.beginTransaction().add(R.id.diary_fragment_container, DiaryFragment()).commit()
        navigationView.setNavigationItemSelectedListener { item: MenuItem ->
            val itemId = item.itemId
            val checkedItem = navigationView.checkedItem
            if (checkedItem != null && checkedItem.itemId == itemId) {
                drawerLayout.closeDrawers()
                return@setNavigationItemSelectedListener true
            }
            when (itemId) {
                R.id.diary -> {
                    fragmentManager.beginTransaction().replace(R.id.diary_fragment_container, DiaryFragment()).commit()
                }

                R.id.attachment -> {
                    fragmentManager.beginTransaction().replace(
                        R.id.diary_fragment_container,
                        AttachmentFragment(fromDiary = false, pickMode = false, dateInt = -1)
                    ).commit()
                }

                R.id.file_library -> {
                    fragmentManager.beginTransaction().replace(R.id.diary_fragment_container, FileLibraryFragment())
                        .commit()
                }

                R.id.settings -> {
                    startActivity(Intent(this, DiaryAttachmentSettingsActivity::class.java))
                    drawerLayout.closeDrawers()
                    return@setNavigationItemSelectedListener false
                }
            }
            drawerLayout.closeDrawers()
            return@setNavigationItemSelectedListener true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            else -> {
                // let it consumed by the fragments in this activity
                return false
            }
        }
        return true
    }

    fun configureDrawerToggle(toolbar: Toolbar) {
        drawerToggle = ActionBarDrawerToggle(this, findViewById(R.id.drawer_layout), toolbar, 0, 0)
        drawerToggle?.isDrawerIndicatorEnabled = true
        drawerToggle?.syncState()
        drawerArrowDrawable = drawerToggle?.drawerArrowDrawable
        drawerToggle?.drawerArrowDrawable = drawerArrowDrawable!!
        drawerToggle?.syncState()
    }
}
