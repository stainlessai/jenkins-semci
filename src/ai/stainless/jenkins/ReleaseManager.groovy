package ai.stainless.jenkins

import ai.stainless.IllegalBranchNameException
import ai.stainless.MissingTagException
import ai.stainless.Semver
import ai.stainless.SemverFormatter
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
    def prefixFilterRegex = ''

    /**
     * Include the word "SNAPSHOT" for snapshot (non-master) builds. This is common for Java builds.
     */
    def includeSnapshotIdentifier = true

    /**
     * GString describing the content of the buildMetadata. Can be set in the Jenkins pipeline to a project-specific
     * value. If nonempty, will be rendered after a plus sign in the semantic version.
     */
    String buildMetadata = ""

    /**
     * GString describing the content of the pre-release string. Can be set in the Jenkins pipeline to a project-specific
     * value. If nonempty, will be rendered after a dash sign in the semantic version, and before any build metadata.
     * Note: this is different for release branches (branch name is excluded)
     */
    String prerelease = '%BUILD_NUMBER%-%BRANCH_NAME%-SNAPSHOT'

    def _branchName = null

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

    /**
     * Get the branch name from the script environment if it's available, throw a meaningful error if not
     * @return
     */
    String getBranchName() {
        if (script.env.BRANCH_NAME != null) {
            return script.env.BRANCH_NAME
        } else if (this._branchName != null) {
            return this._branchName
        } else {
            throw new IllegalArgumentException("Branch name not found in environment, must be set manually using setBranchName()")
        }
    }

    void setBranchName(String branchName) {
        this._branchName = branchName
    }

    boolean isMasterBranch() {
        getBranchName() == masterBranch
    }

    boolean isReleaseBranch() {
        return checkReleaseBranch(getBranchName())
    }

    /**
     * Can't pass information between calls without keeping state, but need this method for testing, so it's left
     * here in default form.
     */
    def getTags() {
        String tagsChrono = this.script.sh(script: 'git for-each-ref --sort=creatordate --format \'%(refname)=%(*objectname:short=7)\' refs/tags', returnStdout: true)
        this.script.echo tagsChrono
        return Tags.parse(tagsChrono)
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
        Tags tags = getTags()

        if (tags.empty) {
            this.script.echo "WARNING: No tags found for build (this may not be a problem)"
        } // no tags!

        // Don't use chronology when versioning develop or master, use version ordering
        def lastTagSemverByTime = tags.toSemverList(prefixFilterRegex)?.last()
        def lastTagSemverByVersion = tags.sortedByVersion(prefixFilterRegex)?.last()
        // sort by natural order
        def releaseBranchSemver = Semver.fromRef(getBranchName(), true)

        def lastTaggedSemverOnThisReleaseBranch = tags.findAllByMajorAndMinorAndPrefixFilter(releaseBranchSemver.major, releaseBranchSemver.minor, prefixFilterRegex)?.last()
        def releaseBranchVersion = releaseBranchSemver

        if (lastTaggedSemverOnThisReleaseBranch) {
            // find the latest version on this minor-patch subtree
            releaseBranchVersion = Tags.sortByVersion([lastTaggedSemverOnThisReleaseBranch, releaseBranchSemver]).last()
        }

//        println "branch=${getBranchName()}"
//        println "prefixFilterRegex=${prefixFilterRegex}"
//        println "tags=${tags.tags}"
//        println "lastTagSemverByTime=$lastTagSemverByTime"
//        println "lastTagSemverByVersion=$lastTagSemverByVersion"
//        println "releaseBranchSemver=${releaseBranchSemver.versionString()}"
//        println "releaseBranchVersion=$releaseBranchVersion"

        if (isMasterBranch()) {
//            println commitHash()
//            println lastTagSemverByTime?.objectname
            if (commitHash() != lastTagSemverByTime?.objectname) {
                throw new MissingTagException("No version can be calculated: branch ${getBranchName()} requires a version tag")
            }

            return lastTagSemverByTime.versionString()
        }

        def result
        if (isReleaseBranch()) {
            if (!allowNonZeroPatchBranches && releaseBranchSemver.patch > 0)
                throw new IllegalBranchNameException("Patch is not zero: ${getBranchName()}")
            // only bump patch if there is no patch tag > 0 on this branch
//            println "diff=${releaseBranchVersion-lastTagSemverByVersion}"
            if (lastTagSemverByTime && (releaseBranchVersion.compareTo(lastTagSemverByVersion) == 0))
                releaseBranchVersion.bumpPatch()
            result = releaseBranchVersion
        } else {
            if (!lastTagSemverByVersion && (prefixFilterRegex != null && !prefixFilterRegex.empty)) {
                throw new MissingTagException("Prefix provided, but no tags found: '$prefixFilterRegex'")
            } else if (lastTagSemverByVersion) {
                result = lastTagSemverByVersion.bumpMinor()
            }
        }

        if (!result) return new Semver().versionString()
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
    public Semver artifact() {
        def semver = Semver.parse(buildSemanticVersion())
        def env = script.env
        if (!isMasterBranch()) {
            if (!isReleaseBranch()) {
                if (!prerelease.toString().empty) {
                    semver.prerelease = replaceAll(prerelease, envMap())
                }
            } else {
                if (!prerelease.toString().empty) {
                    def templ = '%BUILD_NUMBER%-SNAPSHOT'
                    semver.prerelease = replaceAll(templ, envMap())
                }
            }

            if (buildMetadata != null && !buildMetadata.toString().empty) {
                semver.buildMetadata = replaceAll(buildMetadata, envMap())
            }
        }
        // if version == 0.0.0 make it 0.0.1
        if (!semver.prefix) semver.prefix = script.env.JOB_NAME
        semver.v = null // don't add "v" to artifact name
        return semver
    }

    public Map<String, String> envMap() {
        return ['BUILD_NUMBER':script.env['BUILD_NUMBER'], 'BRANCH_NAME':getBranchName()]
    }

    public static String replaceAll(String text, Map<String, String> params) {
        for (entry in params) {
            text = text.replaceAll("%"+entry.key+"%", entry.value)
        }
        return text
    }


    String artifactName() {
        return artifact().toString()
    }

    String artifactVersion() {
        return SemverFormatter.ofPattern("M.m.p'-'?P'+'?B").format(artifact())
    }

    String artifactVersion(String withPattern) {
        return SemverFormatter.ofPattern(withPattern).format(artifact())
    }

    /**
     * Returns true if any files checked into the repo, in the current sub-directory tree, have changed. You can supply
     * an optional regex filter.
     * @return
     */
    @NonCPS
    boolean changesInThisSubtree(String regexFilter = null) {
        String wd = this.script.pwd().replaceFirst("${this.script.env.WORKSPACE}/", '')
//        this.script.echo("wd is $wd")
        return changesInSubtree(wd)
    }

    /**
     * Return changes, if any, in the specified subtree
     * @param relPath
     * @param regexFilter
     * @return
     */
    @NonCPS
    boolean changesInSubtree(String wd, String regexFilter = null) {
        // can't use this in Jenkins, security issue
//        Path workspacePath = Paths.get(this.script.env.WORKSPACE)
//        Path pwd = Paths.get(this.script.pwd())
//        this.script.echo("I'm in directory ${pwd.toAbsolutePath()}")
//        this.script.echo("Workspace is ${workspacePath.toAbsolutePath()}")
//        Path wd = workspacePath.relativize(pwd)
//        this.script.echo("wd is $wd")

        def changes = []

        // multiple change sets include changes to shared libraries, etc.
        if (this.script.currentBuild.changeSets.size() == 0) {
            this.script.echo("No change sets!")
        } else {
            for (changeSet in this.script.currentBuild.changeSets) {
                for (change in changeSet.items) {
//                    this.script.echo(change.commitId)
                    for (path in change.paths) {
                        this.script.echo("path=${path.path}")
//                    this.script.echo("dst=${path.dst}")  // requires admin approval
//                        this.script.echo("src=${path.src}")
//                        this.script.echo("editType=${path.editType}")
                        // is path in cwd
                        if (path.path.startsWith(wd)) {
                            if (!regexFilter || (regexFilter && path.path =~ /$regexFilter/)) {
                                changes.add(path.path)
                            }
                        }
                    }
                }
            }

            if (changes.size() == 0) {
                this.script.echo("No changes in subtree ${wd}!")
            } else {
                this.script.echo("Detected ${changes.size()} changes in subtree ${wd}")
                return true
            }
        }

        return false
    }

    @NonCPS
    boolean changesInSubtreeOrTriggeredManually(String wd, String regexFilter = null) {
        def triggeredManually = this.script.currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause')?.size() > 0
        this.script.echo("Was this build triggered manually? $triggeredManually")
        return changesInSubtree(wd, regexFilter) || triggeredManually
    }

    public void setPrerelease(String prerelease) {
        this.prerelease = prerelease
    }

    public void setBuildMetadata(String buildMetadata) {
        this.buildMetadata = buildMetadata
    }
}
