package pers.zhc.tools.fdb;

import org.jetbrains.annotations.Nullable;
import pers.zhc.jni.sqlite.Cursor;
import pers.zhc.jni.sqlite.SQLite3;
import pers.zhc.jni.sqlite.Statement;

import java.util.ArrayList;
import java.util.LinkedList;

import static pers.zhc.tools.fdb.PathProcessor.ProgressCallback.Phase.*;

/**
 * @author bczhc
 */
public class PathProcessor {
    public static void optimizePath(String filePath, @Nullable ProgressCallback progressCallback) {
        class Lead {
            int mark;
            int p1;
            float p2;

            private void set(int mark, int p1, float p2) {
                this.mark = mark;
                this.p1 = p1;
                this.p2 = p2;
            }
        }
        class Tail {
            final int mark;
            final float p1;
            final float p2;

            public Tail(int mark, float p1, float p2) {
                this.mark = mark;
                this.p1 = p1;
                this.p2 = p2;
            }
        }

        class Path {
            final Lead lead = new Lead();
            final ArrayList<Tail> tail = new ArrayList<>();
        }

        LinkedList<Path> undoList = new LinkedList<>();
        LinkedList<Path> redoList = new LinkedList<>();

        Path path = null;

        final SQLite3 db = SQLite3.open(filePath);
        db.beginTransaction();

        final int recordCount = db.getRecordCount("path");
        int i = 0;

        final Statement statement = db.compileStatement("SELECT mark, p1, p2 FROM path");
        final Cursor cursor = statement.getCursor();
        while (cursor.step()) {
            final int mark = cursor.getInt(0);
            switch (mark) {
                case 0x01:
                case 0x11:
                    path = new Path();
                    path.lead.set(mark, cursor.getInt(1), cursor.getFloat(2));
                    break;
                case 0x02:
                case 0x03:
                case 0x12:
                case 0x13:
                    if (path != null) {
                        path.tail.add(new Tail(mark, cursor.getFloat(1), cursor.getFloat(2)));
                    }
                    break;
                case 0x04:
                case 0x14:
                    if (path != null) {
                        path.tail.add(new Tail(mark, cursor.getFloat(1), cursor.getFloat(2)));
                        undoList.addLast(path);
                        redoList.clear();
                    }
                    path = null;
                    break;
                case 0x20:
                    // undo
                    if (!undoList.isEmpty()) {
                        final Path pop = undoList.removeLast();
                        redoList.addLast(pop);
                    }
                    break;
                case 0x30:
                    // redo
                    if (!redoList.isEmpty()) {
                        final Path pop1 = redoList.removeLast();
                        undoList.addLast(pop1);
                    }
                    break;
                default:
            }
            if (progressCallback != null) {
                progressCallback.progress(PHASE_1, ((float) i) / ((float) recordCount));
            }
            ++i;
        }
        statement.release();

        // noinspection SqlWithoutWhere
        db.exec("DELETE FROM path");
        final Statement insertStatement = db.compileStatement("INSERT INTO path (mark, p1, p2) VALUES (?, ?, ?)");

        final Object[] binds = new Object[3];
        InsertFunction insert = (mark, p1, p2) -> {
            binds[0] = mark;
            binds[1] = p1;
            binds[2] = p2;
            insertStatement.reset();
            insertStatement.bind(binds);
            insertStatement.step();
        };

        i = 0;
        for (Path undoPath : undoList) {
            final Lead lead = undoPath.lead;
            insert.insert(lead.mark, lead.p1, lead.p2);
            for (Tail tail : undoPath.tail) {
                insert.insert(tail.mark, tail.p1, tail.p2);
            }
            if (progressCallback != null) {
                progressCallback.progress(PHASE_2, ((float) i) / ((float) undoList.size()));
            }
            ++i;
        }

        insertStatement.release();
        db.commit();
        db.close();

        if (progressCallback != null) {
            progressCallback.progress(DONE, 0F);
        }
    }

    private interface InsertFunction {
        void insert(int mark, Object p1, Object p2);
    }

    public interface ProgressCallback {
        enum Phase {
            PHASE_1,
            PHASE_2,
            DONE
        }

        /**
         * @param phase    {@link Phase}
         * @param progress [0-1]
         */
        void progress(Phase phase, float progress);
    }
}
