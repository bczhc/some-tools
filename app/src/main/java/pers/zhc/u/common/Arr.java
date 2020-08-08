package pers.zhc.u.common;


@SuppressWarnings("Duplicates")
public class Arr {
    /*private String[] ss;
    private int[] is;
    private char[] cs;
    private byte[] bs;
    private long[] ls;
    private boolean[] bos;
    private double[] ds;
    private float[] fs;

    Arr(String[] strings) {
        this.ss = strings;
    }

    Arr(int[] ints) {
        this.is = ints;
    }

    Arr(char[] chars) {
        this.cs = chars;
    }

    Arr(byte[] bytes) {
        this.bs = bytes;
    }

    Arr(long[] longs) {
        ls = longs;
    }

    Arr(boolean[] booleans) {
        this.bos = booleans;
    }

    Arr(double[] doubles) {
        this.ds = doubles;
    }

    Arr(float[] floats) {
        fs = floats;
    }
*/
    public String toString(String[] strings) {
        if (strings == null)
            return "null";

        int iMax = strings.length - 1;
        if (iMax == -1)
            return "";

        StringBuilder b = new StringBuilder();
        for (int i = 0; ; i++) {
            b.append(strings[i]);
            if (i == iMax)
                return b.toString();
            b.append(",");
        }
    }

    public String toString(int[] ints) {
        if (ints == null)
            return "null";
        int iMax = ints.length - 1;
        if (iMax == -1)
            return "";

        StringBuilder b = new StringBuilder();
        for (int i = 0; ; i++) {
            b.append(ints[i]);
            if (i == iMax)
                return b.toString();
            b.append(",");
        }
    }

    public String toString(char[] chars) {
        if (chars == null)
            return "null";
        int iMax = chars.length - 1;
        if (iMax == -1)
            return "";

        StringBuilder b = new StringBuilder();
        for (int i = 0; ; i++) {
            b.append(chars[i]);
            if (i == iMax)
                return b.toString();
            b.append(",");
        }
    }

    public String toString(byte[] bytes) {
        if (bytes == null)
            return "null";
        int iMax = bytes.length - 1;
        if (iMax == -1)
            return "";

        StringBuilder b = new StringBuilder();
        for (int i = 0; ; i++) {
            b.append(bytes[i]);
            if (i == iMax)
                return b.toString();
            b.append(",");
        }
    }

    public String toString(boolean[] booleans) {
        if (booleans == null)
            return "null";
        int iMax = booleans.length - 1;
        if (iMax == -1)
            return "";

        StringBuilder b = new StringBuilder();

        for (int i = 0; ; i++) {
            b.append(booleans[i]);
            if (i == iMax)
                return b.toString();
            b.append(",");
        }
    }

    public String toString(float[] floats) {
        if (floats == null)
            return "null";

        int iMax = floats.length - 1;
        if (iMax == -1)
            return "";

        StringBuilder b = new StringBuilder();

        for (int i = 0; ; i++) {
            b.append(floats[i]);
            if (i == iMax)
                return b.toString();
            b.append(",");
        }
    }

    public String toString(double[] doubles) {
        if (doubles == null)
            return "null";
        int iMax = doubles.length - 1;
        if (iMax == -1)
            return "";

        StringBuilder b = new StringBuilder();

        for (int i = 0; ; i++) {
            b.append(doubles[i]);
            if (i == iMax)
                return b.toString();
            b.append(",");
        }
    }

    public interface SeparateArrDo<A> {
        void f(A[] a);
    }

    public static class SeparateArr<T> {
        final T[] arr;
        final int perArrLength;

        public SeparateArr(T[] a, int perArrLength) {
            this.arr = a;
            this.perArrLength = perArrLength;
        }

        public void separate(SeparateArrDo<T> target) {
            int arrLength = arr.length;
            int i1 = arrLength % perArrLength;
            boolean b = i1 == 0;
            int t = b ? arrLength / perArrLength : arrLength / perArrLength + 1;
            @SuppressWarnings("unchecked") T[] r_arr = (T[]) new Object[perArrLength];
            for (int i = 0; i < t - 1; i++) {
                for (int j = i * perArrLength; j < (i + 1) * perArrLength; j++) {
                    r_arr[j % perArrLength] = arr[j];
                }
                target.f(r_arr);
            }
            if (!b) {//noinspection unchecked
                r_arr = (T[]) new Object[i1];
                for (int i = t - 1; i < t; i++) {
                    for (int j = i * perArrLength; j < i * perArrLength + i1; j++) {
//                    System.out.println("j = " + j);
                        r_arr[j % perArrLength] = arr[j];
                    }
                    target.f(r_arr);
                }
            }
        }
    }
}

class s0 {
    public static void main(String[] args) {
        new Arr.SeparateArr<>(new String[]{"A"}, 2).separate((Arr.SeparateArrDo) a -> {

        });
    }
}