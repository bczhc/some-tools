package pers.zhc.u.util;

public class StringU {
    public static int compareString(String s1, String s2) {
        int s1Len = s1.length();
        int s2Len = s2.length();
        int min = Math.min(s1Len, s2Len);
        for (int i = 0; i < min; i++) {
            char c1 = s1.charAt(i);
            char c2 = s2.charAt(i);
            if (c1 != c2) return c1 < c2 ? -1 : 1;
        }
        return Integer.compare(s1Len, s2Len);
    }
}
