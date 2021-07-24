package pers.zhc.tools;

import org.jetbrains.annotations.NotNull;

public class Infos {
    public static @NotNull
    String serverRootURL = "";
    public static @NotNull
    String staticResourceRootURL = "https://gitlab.com/bczhc/store/-/raw/master";
    /**
     * format:
     * <ul>
     * <li>%1$s: username</li>
     * <li>%2$s: repository</li>
     * <li>%3$s: branch</li>
     * <li>%4$s: file path</li>
     * </ul>
     */
    public static String githubRawRootURL = "https://raw.fastgit.org/%1$s/%2$s/%3$s/%4$s";

    public static final Class<?> LAUNCHER_CLASS = MainActivity.class;
}
