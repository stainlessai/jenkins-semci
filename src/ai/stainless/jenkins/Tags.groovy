package ai.stainless.jenkins

import ai.stainless.Semver
import com.cloudbees.groovy.cps.NonCPS

/**
 * Utility class to parse git tag results without using Properties. Tags should be of the format
 * <pre>%(refname)=%(objectname:short=7)</pre> and line-separated, e.g.:
 * <pre>
 refs/tags/0.0.1=d15ce1b
 refs/tags/test-tag=d15ce1b
 refs/tags/v0.0.1=d15ce1b
 refs/tags/test-tag@0.0.1=5c2909a
 </pre>
 */
class Tags {

    def tags = [:]

    @NonCPS
    static Tags parse(String tagOutput) {
        def tags = new Tags()
        tagOutput.splitEachLine("=") {
            tags.tags.put(it[0], it[1])
        }
        return tags
    }

    boolean isEmpty() {
        return tags.size() == 0
    }

    /**
     * return the parsed tags as a list of semantic version objects, maybe filtered by prefix
     * @param filter if supplied, a regex to find a subset of all the versions filtered by prefix
     * @return
     */
    @NonCPS
    def toSemverList(String filter = null) {
        if (tags.size() == 0) return null
        if (filter) {
            tags.findAll { e -> e.key =~/${filter}/ }.collect { e -> Semver.fromRef(e.key.replaceAll('\'', ''), true).withObjectName(e.value) }
        } else {
            tags.collect { e -> Semver.fromRef(e.key.replaceAll('\'', ''), true).withObjectName(e.value) }
        }
    }

    @NonCPS
    def sortedByVersion(String prefixFilter = null) {
        def l = toSemverList(prefixFilter).sort()
        return l.sort()
    }

    @NonCPS
    static def sortByVersion(def semverCollection) {
        return semverCollection.sort()
    }

    @NonCPS
    def findAllByMajorAndMinorAndPrefixFilter(int major, int minor, String prefixFilter = null) {
        def results = sortedByVersion(prefixFilter).findAll {
            it.major == major && it.minor == minor
        }
        if (results.size()==0) return null
        results
    }
}
