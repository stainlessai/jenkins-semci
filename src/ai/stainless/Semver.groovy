package ai.stainless

/**
 * Semantic version class that supports a prefix
 */
class Semver {

    static def REGEX = ~/(((.*)?)?-?(v|@))?(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?(-(.*))?/
    def path
    def prefix
    def prefixDelim = '-'
    def preReleaseDelim = '-'
    def v = null
    int major
    int minor
    int patch
    def prerelease

    /**
     * Return a semantic version from a branch string which may include a remote identifier, e.g. "origin/"
     * @param branch
     * @return
     */
    static Semver parse(String semverString, boolean ignoreErrors = false) {
        def matcher = semverString =~ REGEX

        if (matcher.matches()) {
//            (0..matcher.groupCount()-1).each {
//                println "$it": matcher.group(it)
//            }

            def delim = matcher.group(4)
            def prefix =matcher.group(2)

            // crop off training '-' if delimiter=='v', should be able to do this with regex tbh
            if (delim=='v' && prefix.endsWith('-')) prefix = prefix[0..prefix.length()-2]

            return new Semver(prefix:prefix,
                    major:matcher.group(5) as int,
                    minor: matcher.group(6) as int,
                    patch: matcher.group(7) as int,
                    prerelease: matcher.group(8))
        }

        if (!ignoreErrors) throw new IllegalArgumentException("Unable to parse semantic version string: $semverString")
    }

    /**
     * Return a semantic version from a branch string which may include a remote identifier, e.g. "origin/"
     * @param branch
     * @return
     */
    static Semver fromRef(String ref, boolean ignoreErrors = false) {
        def spl = []
        def path = null
        if (ref.contains('/')) {
            spl = ref.split('/')
            ref = spl.last()
            path = spl[0..spl.size()-2].join('/')
        } // branch can start with a path

        def semver = Semver.parse(ref, ignoreErrors)
        if (semver) semver.setPath(path)
        return semver
    }

    String fullPrefix() {
        if (prefix) return "$prefix$prefixDelim"
        return ''
    }

    String fullPreRelease() {
        if (prerelease) return "$preReleaseDelim$prerelease"
        return ''
    }

    String toString() {
        return "${this.fullPrefix()}${v?:''}$major.$minor.$patch${this.fullPreRelease()}"
    }

    Map toMap() {
        this.properties.subMap(['path','prefix','major','minor','patch','prerelease'])
    }

}
