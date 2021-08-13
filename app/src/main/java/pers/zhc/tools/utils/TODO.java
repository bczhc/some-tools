package pers.zhc.tools.utils;

import org.jetbrains.annotations.Contract;

/**
 * @author bczhc
 */
public class TODO {
    @Contract(" -> fail")
    public static void todo() {
        throw new UnimplementedError();
    }

    @Contract("_ -> fail")
    public static void todo(String reason) {
        throw new UnimplementedError(reason);
    }

    private static class UnimplementedError extends Error {
        public UnimplementedError() {
        }

        public UnimplementedError(String reason) {
            super("An operation has not been implemented: " + reason);
        }
    }
}
