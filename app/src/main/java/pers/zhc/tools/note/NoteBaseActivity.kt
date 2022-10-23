package pers.zhc.tools.note

import pers.zhc.tools.BaseActivity

open class NoteBaseActivity: BaseActivity() {
    private val databaseRef = Database.getDbRef()
    protected val database = databaseRef.get()

    override fun finish() {
        databaseRef.release()
        super.finish()
    }
}