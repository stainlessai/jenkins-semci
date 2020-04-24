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
        def result = null
        if (tags.size() == 0) return result
        if (filter) {
            result = tags.findAll { e -> e.key =~/refs\/tags\/${filter}/ }.collect { e -> Semver.fromRef(e.key.replaceAll('\'', ''), true).withObjectName(e.value) }
        } else {
            result = tags.collect { e -> Semver.fromRef(e.key.replaceAll('\'', ''), true).withObjectName(e.value) }
        }
        // return null instead of empty list to avoid "cannot access last() from empty list exception"
        if (result.size()==0) return null
        result
    }

    @NonCPS
    def sortedByVersion(String prefixFilter = null) {
        def l = toSemverList(prefixFilter)?.sort()
        return l
    }

    @NonCPS
    static def sortByVersion(def semverCollection) {
        return semverCollection?.sort()
    }

    @NonCPS
    def findAllByMajorAndMinorAndPrefixFilter(int major, int minor, String prefixFilter = null) {
        def results = sortedByVersion(prefixFilter)?.findAll {
            it.major == major && it.minor == minor
        }
        if (!results || results.size()==0) return null
        results
    }
}
