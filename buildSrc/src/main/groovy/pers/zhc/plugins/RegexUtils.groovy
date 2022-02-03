package pers.zhc.plugins

import org.intellij.lang.annotations.Language

import java.util.regex.Pattern

/**
 * @author bczhc
 */
class RegexUtils {
    static List<List<String>> capture(String text, @Language("RegExp") String regex) {
        def captured = []
        def matcher = Pattern.compile(regex).matcher(text)
        while (matcher.find()) {
            def group = []
            def groupCount = matcher.groupCount()
            for (i in 0..<groupCount + 1) {
                group.add(matcher.group(i))
            }
            captured.add(group)
        }
        return captured
    }
}
