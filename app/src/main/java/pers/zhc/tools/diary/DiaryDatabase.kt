package pers.zhc.tools.diary

import org.intellij.lang.annotations.Language
import pers.zhc.jni.sqlite.Cursor
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.jni.sqlite.Statement
import pers.zhc.tools.MyApplication
import pers.zhc.tools.utils.*
import pers.zhc.tools.utils.rc.Ref
import pers.zhc.tools.utils.rc.ReusableRcManager

/**
 * @author bczhc
 * TODO: avoid directly operating with this [database] inner [SQLite3] object
 */
class DiaryDatabase(path: String) {
    val database = SQLite3.open(path)
    private val updateDiaryContentStatement: Statement

    init {
        @Language("SQLite") val statements =
            """PRAGMA foreign_keys = ON;
-- main diary content table
CREATE TABLE IF NOT EXISTS diary
(
    "date"  INTEGER PRIMARY KEY,
    content TEXT NOT NULL
);
-- diary attachment file info table
-- identifier: SHA1(hex(file).concat(packIntLittleEndian(file.length)))
CREATE TABLE IF NOT EXISTS diary_attachment_file
(
    identifier         TEXT NOT NULL PRIMARY KEY,
    -- Enum:
    -- RAW 0
    -- TEXT 1
    -- IMAGE 2
    -- AUDIO 3
    storage_type       INTEGER,
    addition_timestamp INTEGER UNIQUE,
    filename           TEXT,
    description        TEXT NOT NULL
);
-- diary attachment text storage table
CREATE TABLE IF NOT EXISTS diary_attachment_text
(
    identifier TEXT NOT NULL PRIMARY KEY,
    content    TEXT NOT NULL,

    FOREIGN KEY (identifier) REFERENCES diary_attachment_file (identifier)
);
-- diary attachment file reference table; an attachment can have multiple file references
CREATE TABLE IF NOT EXISTS diary_attachment_file_reference
(
    attachment_id INTEGER,
    identifier    TEXT NOT NULL,

    FOREIGN KEY (attachment_id) REFERENCES diary_attachment (id),
    FOREIGN KEY (identifier) REFERENCES diary_attachment_file (identifier)
);
-- diary attachment data table
CREATE TABLE IF NOT EXISTS diary_attachment
(
    id          INTEGER PRIMARY KEY,
    title       TEXT NOT NULL,
    description TEXT NOT NULL
);
-- diary attachment settings info table
CREATE TABLE IF NOT EXISTS diary_attachment_info
(
    info_json TEXT NOT NULL PRIMARY KEY
);
-- a mapping table between diary and attachment; a diary can have multiple attachments
CREATE TABLE IF NOT EXISTS diary_attachment_mapping
(
    diary_date             INTEGER,
    referred_attachment_id INTEGER,

    FOREIGN KEY (diary_date) REFERENCES diary ("date"),
    FOREIGN KEY (referred_attachment_id) REFERENCES diary_attachment (id)
);""".split(";\n")
        statements.forEach {
            database.exec(it)
        }
        updateDiaryContentStatement = database.compileStatement("UPDATE diary SET content = ? WHERE \"date\" IS ?")
    }

    fun getCharsCount(dateInt: Int): Int {
        return database.withCompiledStatement("SELECT length(content) FROM diary WHERE \"date\" IS ?") {
            it.bind(arrayOf(dateInt))
            val cursor = it.cursor
            androidAssert(cursor.step())
            cursor.getInt(0)
        }
    }

    fun insertFileRecord(fileInfo: FileInfo) {
        database.execBind(
            """INSERT INTO diary_attachment_file(identifier, addition_timestamp, filename, storage_type, description)
VALUES (?, ?, ?, ?, ?)""",
            arrayOf(
                fileInfo.identifier,
                fileInfo.additionTimestamp,
                fileInfo.filename,
                fileInfo.storageType.enumInt,
                fileInfo.description
            )
        )
    }

    fun insertTextRecord(identifier: String, content: String, description: String) {
        insertFileRecord(
            FileInfo(
                null,
                System.currentTimeMillis(),
                StorageType.TEXT,
                description, identifier
            )
        )
        database.execBind(
            """INSERT INTO diary_attachment_text(identifier, content)
VALUES (?, ?)""", arrayOf(identifier, content)
        )
    }

    fun hasFileRecord(identifier: String): Boolean {
        return database.hasRecord(
            "SELECT * FROM diary_attachment_file WHERE identifier IS ?",
            arrayOf(identifier)
        )
    }

    fun updateFileRecord(
        identifier: String,
        filename: String,
        storageType: StorageType,
        description: String
    ) {
        database.execBind(
            """UPDATE diary_attachment_file
SET filename     = ?,
    storage_type = ?,
    description  = ?
WHERE identifier IS ?""",
            arrayOf(filename, storageType.enumInt, description, identifier)
        )
    }

    fun queryAttachments(dateInt: Int?): ArrayList<Attachment> {
        val mapRow: (row: Cursor) -> Attachment = {
            Attachment(it.getText(1), it.getText(2), it.getLong(0))
        }
        return if (dateInt == null) {
            database.queryRows(
                "SELECT id, title, description FROM diary_attachment",
                mapRow = mapRow
            )
        } else {
            database.queryRows(
                """SELECT id, title, description
FROM diary_attachment
         INNER JOIN diary_attachment_mapping ON diary_attachment.id IS diary_attachment_mapping.referred_attachment_id
WHERE diary_attachment_mapping.diary_date IS ?""",
                arrayOf(dateInt), mapRow
            )
        }
    }

    fun queryAttachment(id: Long): Attachment {
        return database.queryOne(
            "SELECT id, title, description FROM diary_attachment WHERE id IS ?",
            arrayOf(id)
        ) {
            Attachment(
                id = it.getLong(0),
                title = it.getText(1),
                description = it.getText(2),
            )
        }!!
    }

    fun checkDiaryAttachmentExists(dateInt: Int, attachmentId: Long): Boolean {
        return database.hasRecord(
            """SELECT *
FROM diary_attachment_mapping
WHERE diary_date IS ?
  AND referred_attachment_id IS ?""",
            arrayOf(dateInt, attachmentId)
        )
    }

    fun deleteAttachment(id: Long) {
        database.execBind(
            "DELETE FROM diary_attachment_file_reference WHERE attachment_id IS ?",
            arrayOf(id)
        )
        database.execBind("DELETE FROM diary_attachment WHERE id IS ?", arrayOf(id))
    }

    fun deleteAttachmentFromDiary(dateInt: Int, attachmentId: Long) {
        database.execBind(
            """DELETE
FROM diary_attachment_mapping
WHERE diary_date IS ?
  AND referred_attachment_id IS ?""",
            arrayOf(dateInt, attachmentId)
        )
    }

    /**
     * attach an attachment to diary
     */
    fun attachAttachment(dateInt: Int, attachmentId: Long) {
        database.execBind(
            "INSERT INTO diary_attachment_mapping (diary_date, referred_attachment_id) VALUES (?, ?)",
            arrayOf(dateInt, attachmentId)
        )
    }

    fun queryAttachmentFileIdentifiers(attachmentId: Long): ArrayList<String> {
        return database.queryRows(
            """SELECT identifier
FROM diary_attachment_file_reference
WHERE attachment_id IS ?""",
            arrayOf(attachmentId)
        ) {
            it.getText(0)
        }
    }

    fun queryAttachmentFiles(attachmentId: Long? = null): ArrayList<FileInfo> {
        val mapRow = { cursor: Cursor ->
            FileInfo(
                identifier = cursor.getText(0),
                storageType = StorageType.from(cursor.getInt(1)),
                additionTimestamp = cursor.getLong(2),
                filename = cursor.getText(3),
                description = cursor.getText(4),
            )
        }

        return if (attachmentId == null) {
            database.queryRows(
                """SELECT identifier, storage_type, addition_timestamp, filename, description
FROM diary_attachment_file""", mapRow = mapRow
            )
        } else {
            database.queryRows(
                """SELECT b.identifier, b.storage_type, b.addition_timestamp, b.filename, b.description
FROM diary_attachment_file_reference a
         INNER JOIN diary_attachment_file b
                    ON a.identifier == b.identifier
WHERE a.attachment_id IS ?""",
                arrayOf(attachmentId),
                mapRow
            )
        }
    }

    fun queryTextAttachment(identifier: String): String {
        return database.queryOne(
            "SELECT content FROM diary_attachment_text WHERE identifier IS ?",
            arrayOf(identifier)
        ) {
            it.getText(0)
        }!!
    }

    fun queryExtraInfo(): ExtraInfo? {
        val jsonString = database.queryOne("SELECT info_json FROM diary_attachment_info") {
            it.getText(0)
        } ?: return null
        return MyApplication.GSON.fromJsonOrNull(jsonString, ExtraInfo::class.java)
    }

    fun updateExtraInfo(info: ExtraInfo) {
        val jsonString = MyApplication.GSON.toJson(info)

        val hasRecord = database.hasRecord("SELECT info_json FROM diary_attachment_info")
        if (hasRecord) {
            @Suppress("SqlWithoutWhere")
            database.execBind(
                "UPDATE diary_attachment_info SET info_json = ?",
                arrayOf(jsonString)
            )
        } else {
            database.execBind(
                "INSERT INTO diary_attachment_info (info_json) VALUES (?)",
                arrayOf(jsonString)
            )
        }
    }

    fun queryDiaryContent(dateInt: Int): String {
        return database.queryOne("SELECT content FROM diary WHERE \"date\" IS ?", arrayOf(dateInt)) {
            it.getText(0)
        }!!
    }

    fun queryAttachmentFile(identifier: String): FileInfo? {
        return database.queryOne(
            """SELECT identifier, storage_type, addition_timestamp, filename, description
FROM diary_attachment_file
WHERE identifier IS ?""",
            arrayOf(identifier)
        ) {
            FileInfo(
                identifier = it.getText(0),
                storageType = StorageType.from(it.getInt(1)),
                additionTimestamp = it.getLong(2),
                filename = it.getText(3),
                description = it.getText(4),
            )
        }
    }

    fun checkIfFileUsedInAttachments(identifier: String): Boolean {
        return database.hasRecord(
            """SELECT *
FROM diary_attachment_file
WHERE diary_attachment_file.identifier IS ?
  AND diary_attachment_file.identifier IN
      (SELECT diary_attachment_file_reference.identifier FROM diary_attachment_file_reference)""",
            arrayOf(identifier)
        )
    }

    fun checkIfAttachmentUsedInDiary(attachmentId: Long): Boolean {
        return database.hasRecord(
            "SELECT * FROM diary_attachment_mapping WHERE referred_attachment_id IS ?",
            arrayOf(attachmentId)
        )
    }

    fun deleteTextAttachment(identifier: String) {
        val fileInfo = queryAttachmentFile(identifier)!!
        if (fileInfo.storageType != StorageType.TEXT) {
            throw RuntimeException("File type is not TEXT")
        }
        // TODO: check if the file to delete has presences in attachments
        // and if so, here now it's excepted to throw an SQL foreign key constraint error
        database.execBind("DELETE FROM diary_attachment_text WHERE identifier IS ?", arrayOf(identifier))
        database.execBind("DELETE FROM diary_attachment_file WHERE identifier IS ?", arrayOf(identifier))
    }

    fun deleteAttachmentFile(identifier: String) {
        database.execBind("DELETE FROM diary_attachment_file WHERE identifier IS ?", arrayOf(identifier))
    }

    fun queryDiaries(@Language("SQLite") sql: String, binds: Array<Any>? = null): ArrayList<Diary> {
        return database.queryRows(sql, binds) {
            Diary(it.getInt(0), it.getText(1))
        }
    }

    fun pickRandomDiary(): Int? {
        return database.queryOne("SELECT \"date\" FROM diary ORDER BY random() LIMIT 1") {
            it.getInt(0)
        }
    }

    fun hasDiary(dateInt: Int): Boolean {
        return database.hasRecord("SELECT \"date\" FROM diary WHERE \"date\" IS ?", arrayOf(dateInt))
    }

    fun updateDiaryContent(dateInt: Int, content: String) {
        updateDiaryContentStatement.stepBind(arrayOf(content, dateInt))
    }

    fun beginTransaction() {
        database.beginTransaction()
    }

    fun commit() {
        database.commit()
    }

    private fun close() {
        updateDiaryContentStatement.release()
        database.close()
    }

    fun getForeignKeyState(): Int {
        return database.queryOne("PRAGMA foreign_keys") { it.getInt(0) }!!
    }

    fun setForeignKeyState(state: Int) {
        database.exec("PRAGMA foreign_keys=$state")
    }

    companion object {
        val internalDatabasePath by lazy {
            Common.getInternalDatabaseDir(MyApplication.appContext, "diary.db")
        }

        private val databaseManager = object : ReusableRcManager<DiaryDatabase>() {
            override fun create(): DiaryDatabase {
                return DiaryDatabase(internalDatabasePath.path)
            }

            override fun release(obj: DiaryDatabase) {
                obj.close()
            }
        }

        fun getDatabaseRef(): Ref<DiaryDatabase> {
            return databaseManager.getRefOrCreate()
        }

        fun getDatabaseRefCount(): Int {
            return databaseManager.getRefCount()
        }
    }
}