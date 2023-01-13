package ai.stainless

import com.cloudbees.groovy.cps.NonCPS

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
    int minor = 1
    int patch = 0
    def prerelease = ''
    def objectname = ''
    // anything after plus sign
    def buildMetadata = ''

    @NonCPS
    static def nullIfEmpty(string) {
        if (!string || string.length() == 0) return null
        string
    }

    @NonCPS
    Semver bumpPatch() {
        patch++
        return this
    }

    @NonCPS
    Semver bumpMinor() {
        minor++
        patch = 0
        return this
    }

    @NonCPS
    Semver bumpMajor() {
        major++
        minor = patch = 0
        return this
    }

    @NonCPS
    Semver withObjectName(String objectName) {
        this.objectname = objectName
        return this
    }

    /**
     * Return a semantic version from a branch string which may include a remote identifier, e.g. "origin/"
     * @param branch
     * @return
     */
    @NonCPS
    static Semver parse(String semverString, boolean ignoreErrors = false) {
//        if (ignoreErrors && !semverString) return null;
//        else if (!semverString) throw new IllegalArgumentException("Can't to parse null semverString")

        def matcher = semverString =~ REGEX

        if (matcher.matches()) {
//            (0..matcher.groupCount()-1).each {
//                println "$it": matcher.group(it)
//            }

            def delim = matcher.group(4) ? nullIfEmpty(matcher.group(4)) : null
            def prefix = matcher.group(2) ? nullIfEmpty(matcher.group(2)) : null

            // crop off trailing '-' if delimiter=='v', should be able to do this with regex tbh
            if (delim?.equalsIgnoreCase('v') && prefix?.endsWith('-')) prefix = prefix[0..prefix.length() - 2]

            return new Semver(prefix: prefix,
                    v: delim,
                    major: matcher.group(5) as int,
                    minor: matcher.group(6) as int,
                    patch: matcher.group(7) as int,
                    prerelease: matcher.group(8),
                    buildMetadata: matcher.group(9))
        }

        if (!ignoreErrors) throw new IllegalArgumentException("Unable to parse semantic version string: $semverString")
    }

    /**
     * Return a semantic version from a branch string which may include a remote identifier, e.g. "origin/"
     * @param branch
     * @return
     */
    @NonCPS
    static Semver fromRef(String ref, boolean ignoreErrors = false) {
        def spl = []
        def path = null
        if (ref == null && !ignoreErrors) {
            throw new IllegalArgumentException("Can't create a Semver from a null ref")
        } else if (ref == null) {
            return new Semver()
        }
        if (ref.contains('/')) {
            spl = ref.split('/')
            ref = spl.size() == 0 ? null : spl.last()
            path = spl[0..spl.size() - 2].join('/')
        } // branch can start with a path

        def semver = Semver.parse(ref, ignoreErrors)
        if (!semver) {
            semver = new Semver()
        }
        semver.setPath(path)
        return semver
    }

    @NonCPS
    String fullPrefix() {
        if (prefix && v && v == 'v') return "$prefix$prefixDelim"
        else if (prefix) return "$prefix"
        return ''
    }

    @NonCPS
    String fullPreRelease() {
        if (prerelease) return "$preReleaseDelim$prerelease"
        return ''
    }

    @NonCPS
    String toString() {
        return "${this.fullPrefix()}-${SemverFormatter.ofPattern("M.m.p'-'?P'+'?B").format(this)}"
    }

    /**
     * Use a custom pattern to return a version string. Useful when you want to use different formats
     * for the same version, e.g., when Docker doesn't accept a "+" in a version tag and you want to use
     * a dash in some places.
     * @param withPattern
     * @return
     */
    String toString(String withPattern) {
        return "${this.fullPrefix()}-${SemverFormatter.ofPattern(withPattern).format(this)}"
    }

    @NonCPS
    @Deprecated
    String artifactName() {
        return "${this.fullPrefix()}-${versionString()}"
    }

    /**
     * @deprecated - use a SemverFormatter instead
     * @return
     */
    @NonCPS
    @Deprecated
    String versionString() {
        return "$major.$minor.$patch${this.fullPreRelease()}${buildMetadata ? '+' + buildMetadata : ''}"
    }

    // Use of this method will be rejected by Jenkins' Groovy sandbox
    @NonCPS
    Map toMap() {
        this.properties.subMap(['path', 'prefix', 'v', 'major', 'minor', 'patch', 'prerelease', 'objectname'])
    }

    // FIXME CPS problems running this on Jenkins
    @Override
    @NonCPS
    int compareTo(Semver o) {
        if (!o) return 0;
        return ((this.major - o.major) * 100) + ((this.minor - o.minor) * 10) + this.patch - o.patch
    }

    @NonCPS
    int minus(Semver o) {
        if (!o) return 0;
        return ((this.major - o.major) * 100) + ((this.minor - o.minor) * 10) + this.patch - o.patch
    }
}
