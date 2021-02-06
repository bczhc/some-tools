package pers.zhc.tools.utils.sqlite;

import pers.zhc.tools.jni.JNI;

public class Statement {
    private final long statementId;

    public Statement(long statementId) {
        this.statementId = statementId;
    }

    /**
     * Bing int value.
     *
     * @param row row, start from 1.
     * @param a   value
     * @throws Exception if failed.
     */
    public void bind(int row, int a) throws Exception {
        JNI.Sqlite3.bind(statementId, row, a);
    }

    /**
     * Bing long value.
     *
     * @param row row, start from 1.
     * @param a   value
     * @throws Exception if failed.
     */
    public void bind(int row, long a) throws Exception {
        JNI.Sqlite3.bind(statementId, row, a);
    }

    /**
     * Bing double value.
     *
     * @param row row, start from 1.
     * @param a   value
     * @throws Exception if failed.
     */
    public void bind(int row, double a) throws Exception {
        JNI.Sqlite3.bind(statementId, row, a);
    }

    /**
     * Bind text.
     *
     * @param row row, start from 1.
     * @param s   string
     * @throws Exception if failed.
     */
    public void bindText(int row, String s) throws Exception {
        JNI.Sqlite3.bindText(statementId, row, s);
    }

    /**
     * Bind null value.
     *
     * @param row row, start from 1.
     * @throws Exception if failed.
     */
    public void bindNull(int row) throws Exception {
        JNI.Sqlite3.bindNull(statementId, row);
    }

    /**
     * Reset the values bound in the statement.
     *
     * @throws Exception if failed.
     */
    public void reset() throws Exception {
        JNI.Sqlite3.reset(statementId);
    }

    /**
     * Bind bytes.
     *
     * @param row   row, start from 1.
     * @param bytes byte array
     * @throws Exception if failed.
     */
    public void bindBlob(int row, byte[] bytes) throws Exception {
        bindBlob(row, bytes, bytes.length);
    }

    /**
     * Bind bytes.
     *
     * @param row   row, start from 1.
     * @param bytes byte array
     * @param size  bind size
     * @throws Exception if failed.
     */
    public void bindBlob(int row, byte[] bytes, int size) throws Exception {
        JNI.Sqlite3.bindBlob(statementId, row, bytes, size);
    }

    /**
     * Execute this statement.
     *
     * @throws Exception if failed.
     */
    public void step() throws Exception {
        JNI.Sqlite3.step(statementId);
    }

    /**
     * Release native resource, origin: `finalize()`.
     */
    public void release() throws Exception {
        JNI.Sqlite3.finalize(statementId);
    }
}
