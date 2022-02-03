import java.util.regex.Pattern

// include:
/*
def selfDir = new File(getClass().protectionDomain.codeSource.location.path).parentFile
metaClass.mixin new GroovyScriptEngine(selfDir.canonicalPath).with {
    loadScriptByName('lib.groovy')
}
*/

class lib {
    static def printStream(InputStream stdout, PrintStream writer) {
        def b = new byte[1]
        while (stdout.read(b) != -1) {
            writer.write([b[0] as char] as char[])
        }
    }

    static def runShell(List<String> cmd, List<String> envp, File dir) {
        def process = cmd.execute(envp, dir)
        def stdout = process.inputStream
        def stderr = process.errorStream
        def threads = [
                new Thread({
                    printStream(stdout, System.out)
                }),
                new Thread({
                    printStream(stderr, System.err)
                })
        ]

        threads.forEach {
            it.start()
        }
        threads.forEach {
            it.join()
        }
        def status = process.waitFor()
        if (status != 0) {
            throw new Exception("Failed to run ${cmd}: non-zero exit value")
        }
    }

    static def runShell(String cmd) {
        runShell([cmd])
    }

    static def runShell(List<String> cmd) {
        runShell(cmd, null, null)
    }

    static def runShells(List<List<String>> cmds) {
        cmds.forEach { cmd ->
            runShell(cmd)
        }
    }

    static String s(GString gs) {
        return gs.toString()
    }

    static List<List<String>> capture(String text, String regex) {
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

    static checkRun(int status) {
        if (status != 0) {
            throw new Exception("Process executing failed: non-zero exit value")
        }
    }
}