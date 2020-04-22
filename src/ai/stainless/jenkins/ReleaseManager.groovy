package ai.stainless.jenkins

import ai.stainless.Semver

class ReleaseManager {

    private final def script

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
        trimOutput("git rev-parse HEAD", 7)
    }

    String shortCommit() {
        trimOutput("git log -n 1 --pretty=format:'%h'", 7)
    }

    private String trimOutput(String script, int maxLength) {
        String content = this.script.sh(script: script, returnStdout: true)
        content.substring(0, Math.min(maxLength, content.length())).trim()
    }

    boolean isMasterBranch() {
        script.env.BRANCH_NAME == 'master'
    }

    def getTags() {
        return this.script.sh(script: "git for-each-ref --sort=creatordate --format '%(refname)' refs/tags", returnStdout: true)
    }

    /**
     * Returns the latest semantic version according to the tag history in the underlying repository
     * @return
     */
    String semanticVersion(boolean allowNonZeroPatchBranches = false) {
        // parse latest tag
        if (tags.readLines().empty) throw new IllegalArgumentException("Can't determine semantic version: no tags")
        def tagSemver = tags.readLines().collect { Semver.fromRef(it.replaceAll('\'', ''), true) }.last()
        def branchSemver = Semver.fromRef(script.env.BRANCH_NAME, true)
        if (!allowNonZeroPatchBranches && branchSemver > tagSemver && branchSemver.patch > 0)
            throw new IllegalArgumentException("Invalid patch version in branch: ${script.env.BRANCH_NAME} (patch must be zero)")
        return [tagSemver, branchSemver].sort().last().versionString()
    }

    /**
     * Parse a branch ref of the form ((pathElement/)*branchName).
     * If branchName is a valid semantic version (with optional prefix) this branch is considered a release branch
     * @param branchName
     * @return
     */
    boolean isReleaseBranch(String branchName) {
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
        def semver = Semver.fromRef(semanticVersion())

        if (!isMasterBranch()) {
            if (!isReleaseBranch(script.env.BRANCH_NAME)) {
                semver.bumpMinor()
                semver.prerelease = "${script.env.BUILD_NUMBER}-${script.env.BRANCH_NAME}-SNAPSHOT"
            } else {
                semver = Semver.fromRef(script.env.BRANCH_NAME)
            }
        }

        // if version == 0.0.0 make it 0.0.1
        if (!semver.major && !semver.minor && !semver.patch) semver.bumpPatch()
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
