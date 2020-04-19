package ai.stainless

/**
 * Semantic version class that supports a prefix
 */
class Semver {

    static def REGEX = ~/(((.*)?)?-?(v|@))?(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?(-(.*))?/
    def path = ''
    def prefix
    def prefixDelim = '-'
    def preReleaseDelim = '-'
    def v = ''
    int major
    int minor
    int patch
    def prerelease

    /**
     * Return a semantic version from a branch string which may include a remote identifier, e.g. "origin/"
     * @param branch
     * @return
     */
    static Semver fromBranch(String branch) {
        def spl = []
        def path = ''
        if (branch.contains('/')) {
            spl = branch.split('/')
            branch = spl.last()
            path = spl[0..spl.size()-2].join('/')
        } // branch can start with a path

        def matcher = branch =~ REGEX

        if (matcher.matches()) {
//            (0..matcher.groupCount()-1).each {
//                println "$it": matcher.group(it)
//            }

            def delim = matcher.group(4)
            def prefix =matcher.group(2)

            // crop off training '-' if delimiter=='v', should be able to do this with regex tbh
            if (delim=='v' && prefix.endsWith('-')) prefix = prefix[0..prefix.length()-2]

            return new Semver(path:path,
                    prefix:prefix,
                    major:matcher.group(5) as int,
                    minor: matcher.group(6) as int,
                    patch: matcher.group(7) as int,
                    prerelease: matcher.group(8))
        }

        throw new IllegalArgumentException("Unable to parse semantic version string from branch: $branch")
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
