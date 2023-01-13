package ai.stainless

/**
 * <p>A class that formats a Semver object into a semantic version based on a format string.</p>
 *
 * <h2>Patterns for formatting and parsing</h2>
 *
 * <pre>
 * Symbol   Meaning
 * _____    _______
 * M        Major version number
 * m        Minor version number
 * p        Patch version number
 * P        Prerelease
 * B        Build metadata
 * .        Semantic version separator
 * '        Quote for static text
 * ?        Omit previous literal if next field is empty, e.g., '-'?B Will omit the dash if B is empty
 * </pre>
 *
 *
 * <h2>Examples</h2>
 * <pre>
 * Semver s = new Semver(prerelease:'mybranch',buildMetadata:'mybuildnumber')
 * SemverFormatter.ofPattern("M.m.p'-'P'-SNAPSHOT').format(s) --> 0.1.0-mybranch-SNAPSHOT
 * SemverFormatter.ofPattern("M.m.p'-'B'-'P'-SNAPSHOT').format(s) --> 0.1.0-mybuildnumber-mybranch-SNAPSHOT
 * SemverFormatter.ofPattern("M.m.p'+'P).format(s) --> 0.1.0+mybuildnumber
 * </pre>
 */
class SemverFormatter {

    String pattern

    protected SemverFormatter(pattern) {
        this.pattern = pattern
    }

    public static SemverFormatter ofPattern(String pattern) {
        return new SemverFormatter(pattern)
    }

    public String format(Semver semver) {
        String template = ''
        String buf = ''
        Boolean literal = false
        Boolean emptyOmit = false
        
        pattern.each { c ->
            // println c
            if (literal) {
                if (c == "'") {
                    // println "closing literal with $buf"
                    literal = false
                }
                else buf += c
            } else {
                if (c == 'M') template += semver.major
                else if (c == '?') {
                    // println "setting emptyOmit"
                    emptyOmit = true
                } else if (c == 'm') template += semver.minor
                else if (c == 'p') template += semver.patch
                else if (c == 'B') {
//                    println "in B with $buf"
                    if (!buf.trim()?.equals('') && semver.buildMetadata && !semver.buildMetadata?.trim()?.equals('')) {
                        template += buf
                        buf = ''
                    }
                    if (emptyOmit) {
                        buf = ''
                    }
                    if (semver.buildMetadata) template += semver.buildMetadata
                } else if (c == 'P') {
//                    println "in P with $buf"
//                    println "${!buf.empty}"
//                    println "${semver.prerelease}"
//                    println "${!semver.prerelease?.empty}"
//                    println "${!emptyOmit}"
                    if (!buf.trim()?.equals('') && semver.prerelease && !semver.prerelease?.trim()?.equals('')) {
                        // println "appending $buf"
                        template += buf
                        buf = ''
                    }
                    if (emptyOmit) {
                        buf = ''
                    }
                   if (semver.prerelease) template += semver.prerelease
                } else if (c == '.') template += '.'
                else if (c == "'") {
                    // println "resetting emptyOmit"
                    literal = true
                    emptyOmit = false
                } else throw new IllegalArgumentException("Bad format character '$c'".toString())
            }
        }

        if (!buf.isEmpty() && !emptyOmit) {
            template += buf
        }

        return template
    }
}
