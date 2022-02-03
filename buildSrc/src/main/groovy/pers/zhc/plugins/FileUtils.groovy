package pers.zhc.plugins

/**
 * @author bczhc
 */
class FileUtils {
    static class IORuntimeException extends RuntimeException {
        IORuntimeException() {
            super()
        }
    }

    static requireCreate(File file) {
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IORuntimeException()
            }
        }
    }

    static requireDelete(File file) {
        if (file.exists()) {
            if (!file.delete()) {
                throw new IORuntimeException()
            }
        }
    }
}
