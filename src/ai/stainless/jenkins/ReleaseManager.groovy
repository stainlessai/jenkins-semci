package ai.stainless.jenkins

import ai.stainless.IllegalBranchNameException
import ai.stainless.Semver

class ReleaseManager {

    private final def script
    
    /**
     * Releases are cut from the master branch
     */
    def masterBranch = 'master'

    /**
     * Branches in this list build prereleases
     * @param script
     */
    def prereleaseBranches = ['develop']

    ReleaseManager(def script) {
        this.script = script
    }

    String commitMessage() {
        trimOutput("git log --format=%B -n 1 HEAD | head -n 1", 180)
    }

    String commitAuthor() {
        trimOutput("git log --format=\'%an\' -n 1 HEAD", 80)
    }

    String commitHash() {
        trimOutput("git rev-parse --short=7 HEAD", 7)
    }
    
    private String trimOutput(String script, int maxLength) {
        String content = this.script.sh(script: script, returnStdout: true)
        content.substring(0, Math.min(maxLength, content.length())).trim()
    }

    boolean isMasterBranch() {
        script.env.BRANCH_NAME == masterBranch
    }

    boolean isReleaseBranch() {
        return checkReleaseBranch(script.env.BRANCH_NAME)
    }
    
    /**
     * Can't pass information between calls without keeping state, but need this method for testing, so it's left
     * here in default form.
     */
    def getTags() {
        return ""
    }

    /**
     * Calculates the current semantic version for the current build.
     * Returns a prerelease version if the current build is in 'prerelease' and a release version if on the masterBranch,
     * and there is a tag corresponding to the masterBranch/HEAD
     * @param allowNonZeroPatchBranches
     * @return
     */
    String buildSemanticVersion(boolean allowNonZeroPatchBranches = false) {
        // parse latest tag
        def tagsChrono = getTags()
//        println tagsChrono
        def props = new Properties()
        
        if (props.load(new StringReader(tagsChrono))?.empty) {
            tagsChrono = this.script.sh(script: 'git for-each-ref --sort=creatordate --format \'%(refname)=%(objectname:short=7)\' refs/tags', returnStdout: true)
//            if (tags.readLines().empty) {
//                throw new IllegalArgumentException("Can't determine semantic version: no tags. If this is a new repo, create a tag for version zero (0.0.0)")
//            }
        }

        if (props.load(new StringReader(tagsChrono))?.empty) println "no tags!"// no tags!
        def taggedSemverListByTime = props.collect { e-> Semver.fromRef(e.key.replaceAll('\'', ''), true).withObjectName(e.value) }
//        println taggedSemverListByTime*.toMap()

        // Don't use chronology when versioning develop or master, use version ordering
        def lastSemverChrono = taggedSemverListByTime.last()
        def lastSemverByVersionOrdering = taggedSemverListByTime.sort().last() // sort by natural order
        def releaseBranchSemver = Semver.fromRef(script.env.BRANCH_NAME, true)
        
        def lastTaggedSemverOnThisReleaseBranch = lastSemverByVersionOrdering.findAll {
            it.major==releaseBranchSemver.major && it.minor==releaseBranchSemver.minor
        }
        def releaseBranchVersion = releaseBranchSemver

        if (!lastTaggedSemverOnThisReleaseBranch.empty) {
            // find the latest version on this minor-patch subtree
            releaseBranchVersion = [lastTaggedSemverOnThisReleaseBranch.last(), releaseBranchSemver].sort().last()
        }

        // TODO if builds require tags and tag doesn't match HEAD, throw an error
        if (isMasterBranch()) {
//            println "tag=${taggedSemverListByTime?.last()?.objectname}"
//            println "hash=${commitHash()}"
            if (commitHash()!=lastSemverChrono?.objectname) {
                throw new IllegalArgumentException("No version can be calculated: branch ${script.env.BRANCH_NAME} requires a version tag")
            }
            
            return lastSemverChrono.versionString()
        }

//        println "branch=${script.env.BRANCH_NAME}"
//        println "lastSemverChrono=$lastSemverChrono"
//        println "lastSemverByVersionOrdering=$lastSemverByVersionOrdering"
//        println "releaseBranchSemver=$releaseBranchSemver"
//        println "releaseBranchVersion=$releaseBranchVersion"

        def result
        if (isReleaseBranch()) {
            if (!allowNonZeroPatchBranches && releaseBranchSemver.patch > 0)
                throw new IllegalBranchNameException("Patch is not zero: ${script.env.BRANCH_NAME}")
            result = releaseBranchVersion.bumpPatch()
        } else {
            result = lastSemverByVersionOrdering.bumpMinor()
        }
        
        if (!result.major && !result.minor && !result.patch) result.bumpPatch()
        return result.versionString()
    }

    /**
     * Parse a branch ref of the form ((pathElement/)*branchName).
     * If branchName is a valid semantic version (with optional prefix) this branch is considered a release branch
     * @param branchName
     * @return
     */
    private boolean checkReleaseBranch(String branchName) {
        if (branchName==masterBranch) return false
        try {
            def semver = Semver.fromRef(branchName)
//            println "isReleaseBranch($branchName)-> ${semver.toMap()}"
            if (semver.v) return true
            // is a valid semantic version starting with 'v' or '@'
        } catch (Throwable t) {
            return false
        }

        return false
    }

    /**
     * Construct an artifact name from the branchName, build number, and tag history of the repo.
     * @param branchName
     * @param buildNumber
     * @param usePrefix - if the semver prefix is not null, use it as the artifact name. Otherwise, use the repo name.
     * @return
     */
    private Semver artifact() {
        def semver = Semver.parse(buildSemanticVersion())

        if (!isMasterBranch()) {
            if (!isReleaseBranch()) {
                semver.prerelease = "${script.env.BUILD_NUMBER}-${script.env.BRANCH_NAME}-SNAPSHOT"
            } else {
                semver.prerelease = "${script.env.BUILD_NUMBER}-SNAPSHOT"
            }
        }

        // if version == 0.0.0 make it 0.0.1
        if (!semver.prefix) semver.prefix = script.env.JOB_NAME
        semver.v = null // don't add "v" to artifact name
        return semver
    }

    String artifactName() {
        return artifact().artifactName()
    }

    String artifactVersion() {
        return artifact().versionString()
    }

}
