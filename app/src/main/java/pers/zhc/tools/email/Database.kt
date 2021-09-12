package pers.zhc.tools.email

import android.content.Context
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.utils.Common
import pers.zhc.tools.utils.SharedRef
import java.io.File

class Database(val path: String) {
    var database: SQLite3 = SQLite3.open(path)

    init {
        configure()
    }

    private fun configure() {
        database.exec(
            """CREATE TABLE IF NOT EXISTS account
(
id          INTEGER,
smtp_server TEXT NOT NULL,
username    TEXT NOT NULL,
password    TEXT NOT NULL,
header_from TEXT NOT NULL
)"""
        )
    }

    fun insert(account: Account) {
        val smtpTransport = account.smtpTransport
        val credential = smtpTransport.credential
        insert(smtpTransport.server, credential.username, credential.password, account.headerFrom)
    }

    private fun insert(smtpServer: String, username: String, password: String, headFrom: String) {
        database.execBind(
            "INSERT INTO account(id, smtp_server, username, password, header_from)\nVALUES (?, ?, ?, ?, ?)",
            arrayOf(System.currentTimeMillis(), smtpServer, username, password, headFrom)
        )
    }

    fun queryAll(): ArrayList<Account> {
        val list = ArrayList<Account>()

        val statement =
            database.compileStatement("SELECT smtp_server, username, password, header_from\nFROM account ")

        val cursor = statement.cursor
        while (cursor.step()) {
            list.add(
                Account(
                    SmtpTransport(cursor.getText(0), Credential(cursor.getText(1), cursor.getText(2))),
                    cursor.getText(3)
                )
            )
        }

        statement.release()
        return list
    }

    companion object {
        private lateinit var databaseFile: File

        fun initPath(context: Context) {
            databaseFile = Common.getInternalDatabaseFile(context, "smtp-config.db")
        }

        class SharedEmailDatabase : SharedRef<Database>() {
            override fun create(): Database {
                return Database(databaseFile.path)
            }

            override fun close(obj: Database) {
                obj.database.close()
            }
        }

        private val database = SharedEmailDatabase()

        fun getDatabaseRef(): SharedRef.Ref<Database> {
            return database.newRefOrCreate()
        }
    }
}