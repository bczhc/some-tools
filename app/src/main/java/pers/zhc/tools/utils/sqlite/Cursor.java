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

    /**
     * Get blob data.
     * @param column column, the leftmost is 0.
     * @return data
     */
    public byte[] getBlob(int column) {
        return JNI.Sqlite3.Cursor.getBlob(cursorId, column);
    }

    /**
     * Get text.
     * @param column column, the leftmost is 0.
     * @return text string
     */
    public String getText(int column) {
        return JNI.Sqlite3.Cursor.getText(cursorId, column);
    }

    /**
     * Get double value.
     * @param column column, the leftmost is 0.
     * @return double value
     */
    public double getDouble(int column) {
        return JNI.Sqlite3.Cursor.getDouble(cursorId, column);
    }

    /**
     * Get float value.
     * @param column column, the leftmost is 0.
     * @return float value
     */
    public float getFloat(int column) {
        return (float) JNI.Sqlite3.Cursor.getDouble(cursorId, column);
    }

    /**
     * Get long value.
     * @param column column, the leftmost is 0.
     * @return long value.
     */
    public long getLong(int column) {
        return JNI.Sqlite3.Cursor.getLong(cursorId, column);
    }

    /**
     * Get integer value.
     * @param column column, the leftmost is 0.
     * @return integer value
     */
    public int getInt(int column) {
        return JNI.Sqlite3.Cursor.getInt(cursorId, column);
    }
}
