package pers.zhc.u.common;

public class Int {
    public static int valueOf(String s) throws Exception {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new Exception(e.toString());
        }
    }

    public static void main(String[] args) {
        try {
            Int.valueOf("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
