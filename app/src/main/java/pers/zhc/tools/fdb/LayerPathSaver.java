package pers.zhc.tools.fdb;

import org.jetbrains.annotations.NotNull;
import pers.zhc.jni.sqlite.SQLite3;
import pers.zhc.jni.sqlite.Statement;
import pers.zhc.tools.jni.JNI;

/**
 * @author bczhc
 */
public class LayerPathSaver {
    private final Statement tmpStatement;
    private final Statement pathStatement;
    private final Statement clearTempStatement;
    private final long layerId;
    private final SQLite3 mainDatabase;
    private final String pathTableName;
    private final String tempTableName;
    private final Statement transferToPathTableStatement;

    /**
     * 4 bytes * 3
     */
    private final byte[] packBuf = new byte[12];

    public LayerPathSaver(@NotNull SQLite3 mainDatabase, long layerId) {
        this.layerId = layerId;
        this.mainDatabase = mainDatabase;

        pathTableName = "path_layer_" + layerId;
        tempTableName = "temp_layer_" + layerId;

        configureDatabase();

        tmpStatement = mainDatabase.compileStatement("INSERT INTO " + tempTableName + "(mark, info, x, y) VALUES (?, ?, ?, ?)");
        pathStatement = mainDatabase.compileStatement("INSERT INTO " + pathTableName + "(mark, info, x, y) VALUES (?, ?, ?, ?)");
        // noinspection SqlWithoutWhere
        clearTempStatement = mainDatabase.compileStatement("DELETE FROM " + tempTableName);
        transferToPathTableStatement = mainDatabase.compileStatement("INSERT INTO " + pathTableName + " SELECT mark, info, x, y FROM " + tempTableName);
    }

    private void configureDatabase() {
        mainDatabase.exec("CREATE TABLE IF NOT EXISTS " + pathTableName +
                "\n" +
                "(\n" +
                "    mark INTEGER,\n" +
                "    info BLOB,\n" +
                "    x    REAL,\n" +
                "    y    REAL\n" +
                ")");
        mainDatabase.exec("CREATE TABLE IF NOT EXISTS " + tempTableName +
                "\n" +
                "(\n" +
                "    mark INTEGER,\n" +
                "    info BLOB,\n" +
                "    x    REAL,\n" +
                "    y    REAL\n" +
                ")");
    }

    public void undo() {
        pathStatement.reset();
        pathStatement.bind(1, 0x20);
        pathStatement.bindNull(2);
        pathStatement.bindNull(3);
        pathStatement.bindNull(4);
        pathStatement.step();
    }

    public void redo() {
        pathStatement.reset();
        pathStatement.bind(1, 0x30);
        pathStatement.bindNull(2);
        pathStatement.bindNull(3);
        pathStatement.bindNull(4);
        pathStatement.step();
    }

    private void insertPoint(int mark, float x, float y) {
        tmpStatement.reset();
        tmpStatement.bind(1, mark);
        tmpStatement.bindNull(2);
        tmpStatement.bind(3, x);
        tmpStatement.bind(4, y);
        tmpStatement.step();
    }

    private void insertWithInfo(int mark, byte[] info, float x, float y) {
        tmpStatement.reset();
        tmpStatement.bind(1, mark);
        tmpStatement.bindBlob(2, info);
        tmpStatement.bind(3, x);
        tmpStatement.bind(4, y);
        tmpStatement.step();
    }

    public void onDrawingTouchDown(float x, float y, int color, float strokeWidth, float blurRadius) {
        JNI.FloatingBoard.packStrokeInfo3_1(packBuf, color, strokeWidth, blurRadius);
        insertWithInfo(0x01, packBuf, x, y);
    }

    public void onErasingTouchDown(float x, float y, int alpha, float strokeWidth, float blurRadius) {
        JNI.FloatingBoard.packStrokeInfo3_1(packBuf, alpha, strokeWidth, blurRadius);
        insertWithInfo(0x11, packBuf, x, y);
    }

    public void onTouchMove(float x, float y, boolean isEraserMode) {
        insertPoint(isEraserMode ? 0x12 : 0x02, x, y);
    }

    public void onTouchUp(float x, float y, boolean isEraserMode) {
        insertPoint(isEraserMode ? 0x13 : 0x03, x, y);
    }

    public void clearTempTable() {
        clearTempStatement.reset();
        clearTempStatement.step();
    }

    public long getLayerId() {
        return layerId;
    }

    public void release() {
        pathStatement.release();
        tmpStatement.release();
        clearTempStatement.release();
    }

    public void reset() {
        mainDatabase.exec("DROP TABLE " + pathTableName);
        mainDatabase.exec("DROP TABLE " + tempTableName);
        configureDatabase();
    }

    private void transferToPathTable() {
        transferToPathTableStatement.reset();
        transferToPathTableStatement.step();
    }

    public void transferToPathTableAndClear() {
        transferToPathTable();
        clearTempTable();
    }
}
