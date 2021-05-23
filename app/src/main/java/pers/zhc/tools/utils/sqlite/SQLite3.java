package pers.zhc.tools.utils.sqlite;

import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.jni.JNI;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author bczhc
 */
public class SQLite3 {
    private long id;
    private boolean isClosed = false;
    private String databasePath;

    /**
     * SQLITE_ROW native defined value.
     */
    public static final int SQLITE_ROW = 100;

    private SQLite3() {
    }

    @NotNull
    public static SQLite3 open(String path) {
        final SQLite3 db = new SQLite3();
        db.id = JNI.Sqlite3.open(path);
        db.databasePath = path;
        return db;
    }

    public void close() {
        JNI.Sqlite3.close(this.id);
        isClosed = true;
    }

    /**
     * execute SQLite statement with callback
     *
     * @param cmd      statement
     * @param callback callback
     */
    public void exec(String cmd, JNI.Sqlite3.SqliteExecCallback callback) {
        JNI.Sqlite3.exec(this.id, cmd, callback);
    }

    public void exec(String cmd) {
        exec(cmd, null);
    }

    public boolean isClosed() {
        return isClosed;
    }

    public boolean hasTable(String tableName) {
        AtomicBoolean r = new AtomicBoolean(false);
        try {
            exec("SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "';", contents -> {
                r.set(true);
                return 1;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return r.get();
    }

    public boolean hasRecord(String selectSql) {
        int[] c = {0};
        try {
            this.exec(selectSql, contents -> {
                ++c[0];
                return 1;
            });
        } catch (Exception ignored) {
            // terminate exception
        }
        return c[0] != 0;
    }

    /**
     * Get if a record is exist.
     *
     * @param statement <p>select SQLite statement, note it must select `count()`</p>
     *                  <p>E.g.: SELECT COUNT() FROM table_xxx WHERE blah blah</p>
     * @return existence boolean
     */
    public boolean hasRecord(@NotNull Statement statement) {
        statement.stepRow();
        return statement.getCursor().getLong(0) != 0;
    }

    public boolean checkIfCorrupt() {
        return JNI.Sqlite3.checkIfCorrupt(id);
    }

    public Statement compileStatement(String sql) {
        long statementId = JNI.Sqlite3.compileStatement(this.id, sql);
        return new Statement(statementId);
    }

    public Statement compileStatement(String sql, @NotNull Object[] binds) {
        long statementId = JNI.Sqlite3.compileStatement(this.id, sql);
        final Statement statement = new Statement(statementId);
        statement.bind(binds);
        return statement;
    }

    public String getDatabasePath() {
        return databasePath;
    }

    public void beginTransaction() {
        this.exec("BEGIN TRANSACTION ");
    }

    public void commit() {
        this.exec("COMMIT ");
    }
}