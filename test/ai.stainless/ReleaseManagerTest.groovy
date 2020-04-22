import ai.stainless.jenkins.ReleaseManager

class TestScript {
    public TestScript(Map env) {
        this.env = env
    }
    def env = [:]

    def sh(Map params) {
        def cmd = params.script.execute()
        def out = new StringBuffer()
        def err = new StringBuffer()
        cmd.consumeProcessOutput(out, err)
        cmd.waitFor()
        return out.toString()
    }
}

//class TestReleaseManager extends ReleaseManager {
//    TestReleaseManager(script) { super(script) }
//    def getTags() { return "blah\n" }
//}

ReleaseManager.metaClass.getTags = {
    return "blah"
}

assert "0.0.1" == new ReleaseManager(new TestScript(BUILD_NUMBER: "18", JOB_NAME: "jobby", BRANCH_NAME: "master")).semanticVersion()
assert "jobby-0.0.1" == new ReleaseManager(new TestScript(BUILD_NUMBER: "18", JOB_NAME: "jobby", BRANCH_NAME: "master")).artifactName()
assert "0.0.1" == new ReleaseManager(new TestScript(BUILD_NUMBER: "18", JOB_NAME: "jobby", BRANCH_NAME: "master")).artifactVersion()
assert "jobby-0.1.0-18-develop-SNAPSHOT" == new ReleaseManager(new TestScript(BUILD_NUMBER: "18", JOB_NAME: "jobby", BRANCH_NAME: "develop")).artifactName()
assert "jobby-0.1.0-18-myfeaturebranch-SNAPSHOT" == new ReleaseManager(new TestScript(BUILD_NUMBER: "18", JOB_NAME: "jobby", BRANCH_NAME: "myfeaturebranch")).artifactName()
assert "0.1.0-18-myfeaturebranch-SNAPSHOT" == new ReleaseManager(new TestScript(BUILD_NUMBER: "18", JOB_NAME: "jobby", BRANCH_NAME: "myfeaturebranch")).artifactVersion()

ReleaseManager.metaClass.getTags = {
    return "blah"
}
assert "jobby-0.0.1" == new ReleaseManager(new TestScript(BUILD_NUMBER: "18", JOB_NAME: "jobby", BRANCH_NAME: "master")).artifactName()

// FIXME make release candidate?
assert "jobby-0.0.1" == new ReleaseManager(new TestScript(BUILD_NUMBER: "18", JOB_NAME: "jobby", BRANCH_NAME: "v0.0.0")).artifactName()

assert "0.0.1" == new ReleaseManager(new TestScript(BUILD_NUMBER: "18", JOB_NAME: "jobby", BRANCH_NAME: "v0.0.0")).artifactVersion()


ReleaseManager.metaClass.getTags = {
    return "v1.0.0\nv2.0.0"
}

assert "jobby-2.1.0-1318-develop-SNAPSHOT" == new ReleaseManager(new TestScript(BUILD_NUMBER: "1318", JOB_NAME: "jobby", BRANCH_NAME: "develop")).artifactName()

// TODO below
// if no prefix is found, remove "v"?
assert "2.0.0" == new ReleaseManager(new TestScript(BUILD_NUMBER: "18", JOB_NAME: "jobby", BRANCH_NAME: "master")).semanticVersion()

ReleaseManager.metaClass.getTags = {
    return "v1.0.0\nmyprefix@2.0.6"
}

assert "2.0.6" == new ReleaseManager(new TestScript(BUILD_NUMBER: "18", JOB_NAME: "jobby", BRANCH_NAME: "origin/myprefix@2.0.0")).semanticVersion()
assert "2.1.0" == new ReleaseManager(new TestScript(BUILD_NUMBER: "18", JOB_NAME: "jobby", BRANCH_NAME: "origin/myprefix@2.1.0")).semanticVersion()

try {
// This shouldn't be allowed, make configurable
    assert "2.1.1" == new ReleaseManager(new TestScript(BUILD_NUMBER: "18", JOB_NAME: "jobby", BRANCH_NAME: "origin/myprefix@2.1.1")).semanticVersion()
} catch (IllegalArgumentException e) {
    assert e.message =~ /^Invalid patch/
}