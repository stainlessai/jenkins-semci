package ai.stainless.jenkins

import ai.stainless.IllegalBranchNameException
import ai.stainless.MissingTagException
import ai.stainless.Semver
import com.cloudbees.groovy.cps.NonCPS

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

    /**
     * A prefix filter regex for branches and tags. If null will just use full tag or branch list.
     */
    def prefixFilterRegex = null

    ReleaseManager(def script) {
        this.script = script
    }

    ReleaseManager(def script, String prefixFilter) {
        this.script = script
        this.prefixFilterRegex = prefixFilter
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
     *
     * @param allowNonZeroPatchBranches
     * @return
     */
    String buildSemanticVersion(boolean allowNonZeroPatchBranches = false) {
        def tags = Tags.parse(getTags())

        if (tags.empty) {
            String tagsChrono = this.script.sh(script: 'git for-each-ref --sort=creatordate --format \'%(refname)=%(objectname:short=7)\' refs/tags', returnStdout: true)
            this.script.echo tagsChrono
            tags = Tags.parse(tagsChrono)
        }

        if (tags.empty) {
            this.script.echo "WARNING: No tags found for build (this may not be a problem)"
        } // no tags!

        // Don't use chronology when versioning develop or master, use version ordering
        def lastTagSemverByTime = tags.toSemverList(prefixFilterRegex)?.last()
        def lastTagSemverByVersion = tags.sortedByVersion(prefixFilterRegex)?.last()
        // sort by natural order
        def releaseBranchSemver = Semver.fromRef(script.env.BRANCH_NAME, true)

        def lastTaggedSemverOnThisReleaseBranch = tags.findAllByMajorAndMinorAndPrefixFilter(releaseBranchSemver.major, releaseBranchSemver.minor, prefixFilterRegex)?.last()
        def releaseBranchVersion = releaseBranchSemver

        if (lastTaggedSemverOnThisReleaseBranch) {
            // find the latest version on this minor-patch subtree
            releaseBranchVersion = Tags.sortByVersion([lastTaggedSemverOnThisReleaseBranch, releaseBranchSemver]).last()
        }

//        println "branch=${script.env.BRANCH_NAME}"
//        println "prefixFilterRegex=${prefixFilterRegex}"
//        println "tags=${tags.tags}"
//        println "lastTagSemverByTime=$lastTagSemverByTime"
//        println "lastTagSemverByVersion=$lastTagSemverByVersion"
//        println "releaseBranchSemver=$releaseBranchSemver"
//        println "releaseBranchVersion=$releaseBranchVersion"

        if (isMasterBranch()) {
            if (commitHash() != lastTagSemverByTime?.objectname) {
                throw new MissingTagException("No version can be calculated: branch ${script.env.BRANCH_NAME} requires a version tag")
            }

            return lastTagSemverByTime.versionString()
        }

        def result
        if (isReleaseBranch()) {
            if (!allowNonZeroPatchBranches && releaseBranchSemver.patch > 0)
                throw new IllegalBranchNameException("Patch is not zero: ${script.env.BRANCH_NAME}")
            // only bump patch if there is no patch tag > 0 on this branch
//            println "diff=${releaseBranchVersion-lastTagSemverByVersion}"
            if (lastTagSemverByTime && (releaseBranchVersion - lastTagSemverByVersion == 0))
                releaseBranchVersion.bumpPatch()
            result = releaseBranchVersion
        } else {
            if (!lastTagSemverByVersion) {
                throw new MissingTagException("Prefix provided, but no tags found: '$prefixFilterRegex'")
            }
            result = lastTagSemverByVersion.bumpMinor()
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
        if (branchName == masterBranch) return false
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

    /**
     * Returns true if any files checked into the repo, in the current sub-directory tree, have changed. You can supply
     * an optional regex filter.
     * @return
     */
    @NonCPS
    boolean directoryAtThisRootChanged(String regexFilter = null) {
        this.script.echo("I'm in directory ${this.script.pwd()}")
        def changeSet = this.script.currentBuild.changeSets[0]
        if (changeSet && changeSet.items.size() > 0) {
            for (change in changeSet.items) {
                this.script.echo(change.commitId)
                for (path in change.paths) {
                    this.script.echo("path=${path.path}")
                    this.script.echo("dst=${path.dst}")
                    this.script.echo("src=${path.src}")
                    this.script.echo("editType=${path.editType}")
                    // is path in cwd
                }
            }
        } else {
            this.script.echo("No changes!")
        }
    }

}
