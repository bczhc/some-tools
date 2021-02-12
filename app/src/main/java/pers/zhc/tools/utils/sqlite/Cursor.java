package pers.zhc.tools.utils.sqlite;

import pers.zhc.tools.jni.JNI;

/**
 * @author bczhc
 */
public class Cursor {
    private final long cursorId;

    public Cursor(long cursorId) {
        this.cursorId = cursorId;
    }

    public void reset() throws Exception {
        JNI.Sqlite3.Cursor.reset(cursorId);
    }

    /**
     * Step.
     * like Java iterator
     *
     * @return next
     */
    public boolean step() {
        try {
            return JNI.Sqlite3.Cursor.step(cursorId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public byte[] getBlob(int column) {
        return JNI.Sqlite3.Cursor.getBlob(cursorId, column);
    }

    public String getText(int column) {
        return JNI.Sqlite3.Cursor.getText(cursorId, column);
    }

    public double getDouble(int column) {
        return JNI.Sqlite3.Cursor.getDouble(cursorId, column);
    }

    public long getLong(int column) {
        return JNI.Sqlite3.Cursor.getLong(cursorId, column);
    }

    public int getInt(int column) {
        return JNI.Sqlite3.Cursor.getInt(cursorId, column);
    }
}
