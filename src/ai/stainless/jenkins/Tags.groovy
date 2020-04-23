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
            tags.tags.put(it[0],it[1])
        }
        return tags
    }

    boolean isEmpty() {
        return tags.size() == 0
    }

    def toSemverList() {
        if (tags.size()==0) return null
        tags.collect { e -> Semver.fromRef(e.key.replaceAll('\'', ''), true).withObjectName(e.value) }
    }

    @NonCPS
    def sortedByVersion() {
        return toSemverList().sort()
    }

    @NonCPS
    static def sortByVersion(def semverCollection) {
        return semverCollection.sort()
    }
}
