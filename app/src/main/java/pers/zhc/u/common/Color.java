package pers.zhc.u.common;


import pers.zhc.u.Random;

public class Color {
    public String ranColor() {
        return "#" + Integer.toHexString(Random.ran_sc(0, 0xFF))
                + Integer.toHexString(Random.ran_sc(0, 0xFF))
                + Integer.toHexString(Random.ran_sc(0, 0xFF));
    }
}