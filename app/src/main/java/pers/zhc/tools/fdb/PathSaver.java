package pers.zhc.tools.fdb;

import android.view.MotionEvent;
import androidx.annotation.Nullable;
import com.google.gson.Gson;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import pers.zhc.jni.sqlite.Cursor;
import pers.zhc.jni.sqlite.SQLite3;
import pers.zhc.jni.sqlite.Statement;
import pers.zhc.tools.utils.SQLite3UtilsKt;
import pers.zhc.util.Assertion;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Path saver.
 *
 * <h3>Database structure:</h3>
 * <ol>
 *     <li>mark INTEGER</li>
 *     <li>info BLOB</li>
 *     <li>x REAL</li>
 *     <li>y REAL</li>
 * </ol>
 *
 * <ul>
 *     <li>
 *         drawing:<br/>
 *         let action = {@link MotionEvent#getAction()}<br/>
 *         if action is:
 *         <ul>
 *             <li>
 *                 {@link MotionEvent#ACTION_DOWN}:
 *                 <ul>
 *                     <li>mark: {@code 0x01}</li>
 *                     <li>info: some info about the stroke</li>
 *                     <li>x: the x coordinate</li>
 *                     <li>y: the y coordinate</li>
 *                 </ul>
 *             </li>
 *             <li>
 *                 {@link MotionEvent#ACTION_MOVE}:
 *                 <ul>
 *                     <li>mark: {@code 0x02}</li>
 *                     <li>info: NULL</li>
 *                     <li>x: the x coordinate</li>
 *                     <li>y: the y coordinate</li>
 *                 </ul>
 *             </li>
 *             <li>
 *                 {@link MotionEvent#ACTION_UP}
 *                 <ul>
 *                     <li>mark: {@code 0x03}</li>
 *                     <li>info: NULL</li>
 *                     <li>x: the x coordinate</li>
 *                     <li>y: the y coordinate</li>
 *                 </ul>
 *             </li>
 *         </ul>
 *     </li>
 *     <li>
 *         erasing:<br/>
 *         let action = {@link MotionEvent#getAction()}<br/>
 *         if action is:
 *         <ul>
 *             <li>
 *                 {@link MotionEvent#ACTION_DOWN}:
 *                 <ul>
 *                     <li>mark: {@code 0x11}</li>
 *                     <li>info: some info about the stroke</li>
 *                     <li>x: the x coordinate</li>
 *                     <li>y: the y coordinate</li>
 *                 </ul>
 *             </li>
 *             <li>
 *                 {@link MotionEvent#ACTION_MOVE}:
 *                 <ul>
 *                     <li>mark: {@code 0x12}</li>
 *                     <li>info: NULL</li>
 *                     <li>x: the x coordinate</li>
 *                     <li>y: the y coordinate</li>
 *                 </ul>
 *             </li>
 *             <li>
 *                 {@link MotionEvent#ACTION_UP}
 *                 <ul>
 *                     <li>mark: {@code 0x13}</li>
 *                     <li>info: NULL</li>
 *                     <li>x: the x coordinate</li>
 *                     <li>y: the y coordinate</li>
 *                 </ul>
 *             </li>
 *         </ul>
 *     </li>
 *     <li>
 *         Undo:
 *         <ul>
 *             <li>mark: {@code 0x20}</li>
 *             <li>info: NULL</li>
 *             <li>x: NULL</li>
 *             <li>y: NULL</li>
 *         </ul>
 *     </li>
 *     <li>
 *         Redo:
 *         <ul>
 *             <li>mark: {@code 0x30}</li>
 *             <li>info: NULL</li>
 *             <li>x: NULL</li>
 *             <li>y: NULL</li>
 *         </ul>
 *     </li>
 * </ul>
 */
public class PathSaver {
    private final SQLite3 pathDatabase;
    private final ArrayList<LayerPathSaver> layerPathSaverList = new ArrayList<>();

    public static final PathVersion PATH_VERSION = PathVersion.VERSION_4_0;

    public PathSaver(String path) {
        this.pathDatabase = SQLite3.open(path);

        configureDatabase();

        beginTransaction();
    }

    private void configureDatabase() {
        // create table
        pathDatabase.exec("CREATE TABLE IF NOT EXISTS info\n" +
                "(\n" +
                "    version          TEXT NOT NULL,\n" +
                "    create_timestamp INTEGER,\n" +
                "    extra_infos      TEXT NOT NULL\n" +
                ")");

        Statement infoStatement = pathDatabase.compileStatement("INSERT INTO info VALUES(?,?,?)");
        infoStatement.reset();
        infoStatement.bindText(1, PATH_VERSION.getVersionName());
        infoStatement.bind(2, System.currentTimeMillis());
        infoStatement.bindText(3, "");
        infoStatement.step();
        infoStatement.release();
    }

    public void addNewLayerPathSaver(long id) {
        final LayerPathSaver layerPathSaver = new LayerPathSaver(pathDatabase, id);
        layerPathSaverList.add(layerPathSaver);
    }

    public void setExtraInfos(@NotNull ExtraInfo extraInfo) {
        final String extraInfosJson = new Gson().toJson(extraInfo);
        Statement infoStatement = pathDatabase.compileStatement("UPDATE info\n" +
                "SET extra_infos = ?");
        infoStatement.bind(new Object[]{extraInfosJson});
        infoStatement.step();
        infoStatement.release();
    }

    public void flush() {
        pathDatabase.exec("COMMIT");
        pathDatabase.exec("BEGIN TRANSACTION");
    }

    public void reset() {
        pathDatabase.exec("DROP TABLE IF EXISTS info");
        for (LayerPathSaver layerPathSaver : layerPathSaverList) {
            layerPathSaver.reset();
        }
        configureDatabase();
    }

    public void close() {
        pathDatabase.commit();
        pathDatabase.close();
    }

    public void commit() {
        pathDatabase.commit();
    }

    public void beginTransaction() {
        pathDatabase.beginTransaction();
    }

    @org.jetbrains.annotations.Nullable
    @Nullable
    public LayerPathSaver getLayerPathSaver(long id) {
        for (LayerPathSaver layerPathSaver : layerPathSaverList) {
            if (layerPathSaver.getLayerId() == id) {
                return layerPathSaver;
            }
        }
        return null;
    }

    public String getDatabasePath() {
        return pathDatabase.getDatabasePath();
    }

    public File save() {
        flush();
        return new File(pathDatabase.getDatabasePath());
    }

    public static int getPathCount(SQLite3 db) {
        AtomicInteger count = new AtomicInteger();
        SQLite3UtilsKt.withCompiledStatement(db, "SELECT COUNT() FROM path", statement -> {
            final Cursor cursor = statement.getCursor();
            Assertion.doAssertion(cursor.step());
            count.set(cursor.getInt(0));

            return Unit.INSTANCE;
        });

        return count.get();
    }

    public static int getPathCount(String path) {
        final SQLite3 db = SQLite3.open(path);
        int count = getPathCount(db);
        db.close();
        return count;
    }
}
