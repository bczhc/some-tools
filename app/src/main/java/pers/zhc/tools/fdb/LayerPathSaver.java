package pers.zhc.tools.fdb;

import org.jetbrains.annotations.NotNull;
import pers.zhc.jni.sqlite.SQLite3;
import pers.zhc.jni.sqlite.Statement;

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

    public LayerPathSaver(@NotNull SQLite3 mainDatabase, long layerId) {
        this.layerId = layerId;
        this.mainDatabase = mainDatabase;

        pathTableName = "path_layer_" + layerId;
        tempTableName = "temp_layer_" + layerId;

        configureDatabase();

        tmpStatement = mainDatabase.compileStatement("INSERT INTO " + tempTableName + "(mark, p1, p2) VALUES (?, ?, ?)");
        pathStatement = mainDatabase.compileStatement("INSERT INTO " + pathTableName + "(mark, p1, p2) VALUES (?, ?, ?)");
        // noinspection SqlWithoutWhere
        clearTempStatement = mainDatabase.compileStatement("DELETE FROM " + tempTableName);
        transferToPathTableStatement = mainDatabase.compileStatement("INSERT INTO " + pathTableName + " SELECT mark, p1, p2 FROM " + tempTableName);
    }

    private void configureDatabase() {
        mainDatabase.exec("CREATE TABLE IF NOT EXISTS " + pathTableName +
                " (\n" +
                "    mark INTEGER,\n" +
                "    p1   NUMERIC,\n" +
                "    p2   NUMERIC\n" +
                ")");
        mainDatabase.exec("CREATE TABLE IF NOT EXISTS " + tempTableName +
                " (\n" +
                "    mark INTEGER,\n" +
                "    p1   NUMERIC,\n" +
                "    p2   NUMERIC\n" +
                ")");
    }

    public void undo() {
        pathStatement.reset();
        pathStatement.bind(1, 0x20);
        pathStatement.bind(2, 0);
        pathStatement.bind(3, 0);
        pathStatement.step();
    }

    public void redo() {
        pathStatement.reset();
        pathStatement.bind(1, 0x30);
        pathStatement.bind(2, 0);
        pathStatement.bind(3, 0);
        pathStatement.step();
    }

    private void insert(int mark, int p1, float p2) {
        tmpStatement.reset();
        tmpStatement.bind(1, mark);
        tmpStatement.bind(2, p1);
        tmpStatement.bind(3, p2);
        tmpStatement.step();
    }

    private void insert(int mark, float p1, float p2) {
        tmpStatement.reset();
        tmpStatement.bind(1, mark);
        tmpStatement.bind(2, p1);
        tmpStatement.bind(3, p2);
        tmpStatement.step();
    }

    public void onDrawingTouchDown(float x, float y, int color, float strokeWidth) {
        insert(0x01, color, strokeWidth);
        insert(0x02, x, y);
    }

    public void onErasingTouchDown(float x, float y, int alpha, float strokeWidth) {
        insert(0x11, alpha, strokeWidth);
        insert(0x12, x, y);
    }

    public void onTouchMove(float x, float y, boolean isEraserMode) {
        insert(isEraserMode ? 0x13 : 0x03, x, y);
    }

    public void onTouchUp(float x, float y, boolean isEraserMode) {
        insert(isEraserMode ? 0x14 : 0x04, x, y);
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
