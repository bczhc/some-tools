package pers.zhc.tools.utils;

import org.jetbrains.annotations.Contract;

/**
 * @author bczhc
 */
public class NullSafeHelper {
    @Contract(value = "!null, _ -> param1; null, _ -> param2", pure = true)
    public static <T> T getNonNull(T object, T defaultValue) {
        if (object != null) {
            return object;
        }
        return defaultValue;
    }
}
