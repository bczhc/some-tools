package pers.zhc.plugins

/**
 * @author bczhc
 */
class Version {
    int major
    int minor
    long build

    Version(int major, int minor, long build) {
        this.major = major
        this.minor = minor
        this.build = build
    }

    Version(String versionString) {
        def split = versionString.split("\\.")
        if (split.length != 3) {
            throw new ResolveVersionException()
        }
        try {
            this.major = split[0] as int
            this.minor = split[1] as int
            this.build = split[2] as long
        } catch (ignored) {
            throw new ResolveVersionException()
        }
    }

    Compare compare(Version other) {
        if (this.major == other.major) {
            if (this.minor == other.minor) {
                if (this.build == other.build) {
                    return Compare.EQUAL
                }
                return this.build > other.build ? Compare.GREATER : Compare.LESS
            }
            return this.minor > other.minor ? Compare.GREATER : Compare.LESS
        }
        return this.major > other.major ? Compare.GREATER : Compare.LESS
    }

    @Override
    String toString() {
        return "$major.$minor.$build"
    }

    static class ResolveVersionException extends RuntimeException {}

    static String[] sortVersionStrings(String[] versionStrings) {
        def versions = []
        versionStrings.toList().forEach {
            versions.add(new Version(it))
        }

        def r = []
        def sorted = sortVersions(versions as Version[])
        sorted.toList().forEach {
            r.add(it.toString())
        }
        return r as String[]
    }

    static Version[] sortVersions(Version[] versions) {
        return versions.sort { o1, o2 ->
            def version1 = o1 as Version
            def version2 = o2 as Version
            return version1.compare(version2)
        }
    }
}
