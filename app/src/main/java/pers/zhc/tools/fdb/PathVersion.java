package pers.zhc.tools.fdb;

/**
 * @author bczhc
 */
public enum PathVersion {
    VERSION_1_0,
    VERSION_2_0,
    VERSION_2_1,
    VERSION_3_0,
    /**
     * multi-layer path import
     */
    VERSION_3_1,
    /**
     * use packed bytes as stroke info heads
     */
    VERSION_4_0,
    Unknown
}
