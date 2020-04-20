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
        trimOutput("git log -n 1 --pretty=format:'%h'", maxLength: 7)
    }

    private String trimOutput(String script, int maxLength) {
        String content = this.script.sh(script: script, returnStdout: true)
        content.substring(0, Math.min(maxLength, content.length())).trim()
    }

    boolean isMasterBranch() {
        script.env.BRANCH_NAME == 'master'
    }

    /**
     * Returns the latest semantic version according to the tag history in the underlying repository
     * @return
     */
    Semver semanticVersion() {
        // parse latest tag
        this.script.sh(script: "git for-each-ref --sort=creatordate --format '%(refname)' refs/tags", returnStdout: true)
                .lines().collect { Semver.fromRef(it.replaceAll('\'',''),true) }.last()
    }

}

