package ai.stainless

/**
 * Semantic version class that supports a prefix
 */
class Semver implements Comparable<Semver> {

    static def REGEX = ~/(((.*)?)?-?(v|@))?(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?(-(.*))?/
    def path
    def prefix
    def prefixDelim = '-'
    def preReleaseDelim = '-'
    def v = null
    int major = 0
    int minor = 0
    int patch = 1
    def prerelease

    static def nullIfEmpty(string) {
        if (!string || string.length()==0) return null
        string
    }

    void bumpPatch() {
        patch++
    }

    void bumpMinor() {
        minor++
        patch = 0
    }

    void bumpMajor() {
        major++
        minor = patch = 0
    }

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

            def delim = matcher.group(4)?nullIfEmpty(matcher.group(4)):null
            def prefix = matcher.group(2)?nullIfEmpty(matcher.group(2)):null

            // crop off trailing '-' if delimiter=='v', should be able to do this with regex tbh
            if (delim?.equalsIgnoreCase('v') && prefix?.endsWith('-')) prefix = prefix[0..prefix.length()-2]

            return new Semver(prefix:prefix,
                    v: delim,
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
        if (!semver) {
            semver = new Semver()
        }
        semver.setPath(path)
        return semver
    }

    String fullPrefix() {
        if (prefix && v && v=='v') return "$prefix$prefixDelim"
        else if (prefix) return "$prefix"
        return ''
    }

    String fullPreRelease() {
        if (prerelease) return "$preReleaseDelim$prerelease"
        return ''
    }

    String toString() {
        return "${this.fullPrefix()}${v?:''}$major.$minor.$patch${this.fullPreRelease()}"
    }

    String artifactName() {
        return "${this.fullPrefix()}-$major.$minor.$patch${this.fullPreRelease()}"
    }

    String versionString() {
        return "$major.$minor.$patch${this.fullPreRelease()}"
    }

    // Use of this method will be rejected by Jenkins' Groovy sandbox
    Map toMap() {
        this.properties.subMap(['path','prefix','v','major','minor','patch','prerelease'])
    }

    @Override
    int compareTo(Semver o) {
        return ((this.major - o.major) * 100) + ((this.minor - o.minor) * 10) + this.patch - o.patch
    }
}
