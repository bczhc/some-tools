package pers.zhc.tools.note

import pers.zhc.tools.BaseActivity

open class NoteBaseActivity : BaseActivity() {
    protected var databaseRef = Database.getDbRef()
        private set

    /**
     * shortcut reference to the inner database
     */
    protected var database = databaseRef.get()
        private set

    protected fun reopenDatabase() {
        if (Database.getRefCount() != 0) {
            throw RuntimeException("Reference count is not zero")
        }
        // use the new database reference
        databaseRef = Database.getDbRef()
        database = databaseRef.get()
    }

    override fun finish() {
        databaseRef.release()
        super.finish()
    }
}