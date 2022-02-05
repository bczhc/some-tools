package pers.zhc.tools.fdb;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pers.zhc.jni.sqlite.Cursor;
import pers.zhc.jni.sqlite.SQLite3;
import pers.zhc.jni.sqlite.Statement;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.IORuntimeException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @author bczhc
 */
public enum PathVersion {
    VERSION_1_0("1.0"),
    VERSION_2_0("2.0"),
    VERSION_2_1("2.1"),
    /**
     * start using SQLite3 database
     */
    VERSION_3_0("3.0"),
    /**
     * multi-layer path import
     */
    VERSION_3_1("3.1"),
    /**
     * use packed bytes as stroke info heads
     */
    VERSION_4_0("4.0"),
    Unknown("Unknown");

    private final String versionName;

    @Contract(pure = true)
    PathVersion(String name) {
        versionName = name;
    }

    @Contract(pure = true)
    public String getVersionName() {
        return versionName;
    }

    @NotNull
    public static PathVersion getPathVersion(@NotNull File f) {
        PathVersion version = null;

        // check paths that use SQLite database
        final SQLite3 db = SQLite3.open(f.getPath());
        if (!db.checkIfCorrupt()) {
            final Statement statement = db.compileStatement("SELECT version FROM info");
            final Cursor cursor = statement.getCursor();
            if (cursor.step()) {
                final String versionString = cursor.getText(0);

                final HashMap<String, PathVersion> map = new HashMap<>();
                map.put("3.0", VERSION_3_0);
                map.put("3.1", VERSION_3_1);
                map.put("4.0", VERSION_4_0);

                final PathVersion get = map.get(versionString);
                if (get != null) {
                    version = get;
                } else {
                    version = Unknown;
                }
            } else {
                version = Unknown;
            }
            statement.release();
        }
        db.close();

        if (version != null) {
            return version;
        }

        // check path versions 1.0, 2.0, 2.1
        try {
            FileInputStream is = new FileInputStream(f);
            byte[] buf = new byte[12];
            Common.doAssertion(is.read(buf) == 12);
            if (Arrays.equals(buf, "path ver 2.0".getBytes())) {
                version = VERSION_2_0;
            } else if (Arrays.equals(buf, "path ver 2.1".getBytes())) {
                version = VERSION_2_1;
            } else {
                version = VERSION_1_0;
            }
            is.close();
            return version;
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }
}
