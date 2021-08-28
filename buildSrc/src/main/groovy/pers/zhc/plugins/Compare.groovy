package pers.zhc.plugins

/**
 * @author bczhc
 */
enum Compare {
    GREATER(1),
    LESS(-1),
    EQUAL(0)

    int intValue

    Compare(int intValue) {
        this.intValue = intValue
    }
}
