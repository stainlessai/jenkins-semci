import ai.stainless.IllegalBranchNameException
import ai.stainless.MissingTagException
import ai.stainless.Semver
import ai.stainless.SemverFormatter
import ai.stainless.jenkins.ReleaseManager
import ai.stainless.jenkins.Tags

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

    def echo(str) {
        println str
    }
}

try {
    println Semver.parse(null)
} catch (Exception e) {
    assert e.message == 'Unable to parse semantic version string: null'
}

def s = new Semver(prerelease: 'mybranch', buildMetadata: 'mybuildnumber')
assert SemverFormatter.ofPattern("M.m.p'-'P'-SNAPSHOT'").format(s) == '0.1.0-mybranch-SNAPSHOT'
assert SemverFormatter.ofPattern("M.m.p'-'B'-'P'-SNAPSHOT'").format(s) == '0.1.0-mybuildnumber-mybranch-SNAPSHOT'
assert SemverFormatter.ofPattern("M.m.p'+'B").format(s) == '0.1.0+mybuildnumber'
assert SemverFormatter.ofPattern("M.m.p'+'P.B").format(s) == '0.1.0+mybranch.mybuildnumber'

try {
    SemverFormatter.ofPattern("M.m.p'+'Z").format(s)
} catch (Exception e) {
    assert e.message.startsWith('Bad')
}
s = new Semver()
assert SemverFormatter.ofPattern("M.m.p'-'B'-'P'-SNAPSHOT'").format(s) == '0.1.0---SNAPSHOT'

ReleaseManager.metaClass.getTags = {
    return Tags.parse("")
}

assert "0.1.0" == new ReleaseManager(new TestScript(BUILD_NUMBER: "1", JOB_NAME: "jobby", BRANCH_NAME: "develop")).buildSemanticVersion()

def rtest1 = new ReleaseManager(new TestScript(BUILD_NUMBER: "1", JOB_NAME: "jobby", BRANCH_NAME: "develop"))
assert "0.1.0-1-develop-SNAPSHOT" == rtest1.artifactVersion()
rtest1.prerelease = "SNAPSHOT"
assert "0.1.0-SNAPSHOT" == rtest1.artifactVersion()
rtest1.prerelease = ""
assert "0.1.0" == rtest1.artifactVersion()
rtest1.buildMetadata = "3aaaaaa"
assert "0.1.0+3aaaaaa" == rtest1.artifactVersion()
rtest1.buildMetadata = "%BUILD_NUMBER%"
assert "0.1.0+1" == rtest1.artifactVersion()

ReleaseManager.metaClass.getTags = {
    return Tags.parse("blah=schmah\n")
}

def thrown = false
try {
    assert "0.0.1" == new ReleaseManager(new TestScript(BUILD_NUMBER: "1", JOB_NAME: "jobby", BRANCH_NAME: "master")).buildSemanticVersion()
    assert "jobby-0.0.1" == new ReleaseManager(new TestScript(BUILD_NUMBER: "2", JOB_NAME: "jobby", BRANCH_NAME: "master")).artifactName()
} catch (MissingTagException e) {
    thrown = true
    assert e.message =~ /^No version/
}

assert thrown

//
//
//
//

ReleaseManager.metaClass.getTags = {
    return Tags.parse("v0.0.1=abcdef0\n")
}

ReleaseManager.metaClass.commitHash = {
    return "abcdef0"
}

thrown = false
try {
    assert "0.0.1" == new ReleaseManager(new TestScript(BUILD_NUMBER: "3", JOB_NAME: "jobby", BRANCH_NAME: "master")).buildSemanticVersion()
    assert "jobby-0.0.1" == new ReleaseManager(new TestScript(BUILD_NUMBER: "4", JOB_NAME: "jobby", BRANCH_NAME: "master")).artifactName()
} catch (MissingTagException e) {
    thrown = true
    assert e.message =~ /^No version/
}

assert !thrown

//
//
//
//

ReleaseManager.metaClass.getTags = {
    return Tags.parse("v0.1.1=abcdef0\n")
}

assert "jobby-0.2.0-5-develop-SNAPSHOT" == new ReleaseManager(new TestScript(BUILD_NUMBER: "5", JOB_NAME: "jobby", BRANCH_NAME: "develop")).artifactName()
assert "jobby-0.2.0-6-myfeaturebranch-SNAPSHOT" == new ReleaseManager(new TestScript(BUILD_NUMBER: "6", JOB_NAME: "jobby", BRANCH_NAME: "myfeaturebranch")).artifactName()
assert "0.2.0-7-myfeaturebranch-SNAPSHOT" == new ReleaseManager(new TestScript(BUILD_NUMBER: "7", JOB_NAME: "jobby", BRANCH_NAME: "myfeaturebranch")).artifactVersion()

// FIXME make release candidate?
assert "jobby-0.0.1-8-SNAPSHOT" == new ReleaseManager(new TestScript(BUILD_NUMBER: "8", JOB_NAME: "jobby", BRANCH_NAME: "v0.0.0")).artifactName()
assert "0.0.1-9-SNAPSHOT" == new ReleaseManager(new TestScript(BUILD_NUMBER: "9", JOB_NAME: "jobby", BRANCH_NAME: "v0.0.0")).artifactVersion()


ReleaseManager.metaClass.getTags = {
    return Tags.parse("v1.0.0=abcdef0\nv2.0.0=abcdef0")
}

assert "jobby-2.1.0-10-develop-SNAPSHOT" == new ReleaseManager(new TestScript(BUILD_NUMBER: "10", JOB_NAME: "jobby", BRANCH_NAME: "develop")).artifactName()

// TODO below
// if no prefix is found, remove "v"?
assert "2.1.0" == new ReleaseManager(new TestScript(BUILD_NUMBER: "11", JOB_NAME: "jobby", BRANCH_NAME: "develop")).buildSemanticVersion()

/*
 * Develop should always track the latest version ordered in terms of semantic version (not chronologically). Any releases
 * made to the 1.x.x branch must be made on the release branch. As a general rule, any releases made to any version where
 * a future release has been made can't be made on the develop branch.
 */

ReleaseManager.metaClass.getTags = {
    return Tags.parse("v2.1.0=abcdef0\nv1.1.0=abcdef0")
}

assert "2.2.0" == new ReleaseManager(new TestScript(BUILD_NUMBER: "12", JOB_NAME: "jobby", BRANCH_NAME: "develop")).buildSemanticVersion()

ReleaseManager.metaClass.getTags = {
    return Tags.parse("v1.0.0=32c39f9\nmyprefix@2.0.6=18a90bc")
}

assert "2.0.7" == new ReleaseManager(new TestScript(BUILD_NUMBER: "13", JOB_NAME: "jobby", BRANCH_NAME: "origin/myprefix@2.0.0")).buildSemanticVersion()
assert "2.1.0" == new ReleaseManager(new TestScript(BUILD_NUMBER: "14", JOB_NAME: "jobby", BRANCH_NAME: "origin/myprefix@2.1.0")).buildSemanticVersion()
assert "jobby-2.1.0-14-SNAPSHOT" == new ReleaseManager(new TestScript(BUILD_NUMBER: "14", JOB_NAME: "jobby", BRANCH_NAME: "origin/myprefix@2.1.0")).artifactName()

try {
// This shouldn't be allowed, make configurable
    assert "2.1.1" == new ReleaseManager(new TestScript(BUILD_NUMBER: "15", JOB_NAME: "jobby", BRANCH_NAME: "origin/myprefix@2.1.1")).buildSemanticVersion()
} catch (IllegalBranchNameException e) {
    assert e.message =~ /^Patch is not zero/
}

def rm = new ReleaseManager(new TestScript(BUILD_NUMBER: "16", JOB_NAME: "jobby", BRANCH_NAME: "develop"))
assert "2.1.0" == rm.buildSemanticVersion()

//
// "| v2.0.0 | v1.9.5 | v1.9.5 | (any) | 2.0.0 | example-2.0.0 | 2.0.0 |"
//

ReleaseManager.metaClass.getTags = {
    return Tags.parse("v1.0.0=32c39f9\nv1.9.5=18a90bc")
}

assert "2.0.0" == new ReleaseManager(new TestScript(BUILD_NUMBER: "17", JOB_NAME: "jobby", BRANCH_NAME: "v2.0.0")).buildSemanticVersion()

ReleaseManager.metaClass.getTags = {
    return Tags.parse("")
}

assert "2.0.0" == new ReleaseManager(new TestScript(BUILD_NUMBER: "18", JOB_NAME: "jobby", BRANCH_NAME: "v2.0.0")).buildSemanticVersion()

ReleaseManager.metaClass.getTags = {
    return Tags.parse("\n\n\n")
}

assert "2.0.0" == new ReleaseManager(new TestScript(BUILD_NUMBER: "19", JOB_NAME: "jobby", BRANCH_NAME: "v2.0.0")).buildSemanticVersion()

//
//
// Test filters
//
//

ReleaseManager.metaClass.getTags = {
    return Tags.parse('''
refs/tags/prefix2@2.0.0=0123456
refs/tags/prefix2@2.0.1=0123457
refs/tags/prefix2@2.0.2=0123458
refs/tags/prefix2@2.0.3=0123459
refs/tags/prefix2@2.1.0=0123450
refs/tags/prefix1-v1.0.0=0123456
refs/tags/prefix1-v1.0.1=0123457  
refs/tags/prefix1-v1.0.2=0123458
refs/tags/prefix1-v1.0.3=0123459
refs/tags/prefix1-v1.1.0=0123450
''')
}

assert "2.2.0" == new ReleaseManager(new TestScript(BUILD_NUMBER: "20", JOB_NAME: "jobby", BRANCH_NAME: "develop"), "prefix2").buildSemanticVersion()
assert "1.2.0" == new ReleaseManager(new TestScript(BUILD_NUMBER: "21", JOB_NAME: "jobby", BRANCH_NAME: "develop"), "prefix1").buildSemanticVersion()

//
// java.lang.NullPointerException: Cannot invoke method bumpMinor() on null object
//	at ai.stainless.Semver$bumpMinor$8.call(Unknown Source)
//	at ai.stainless.jenkins.ReleaseManager.buildSemanticVersion(ReleaseManager.groovy:128)
//	at ai.stainless.jenkins.ReleaseManager.buildSemanticVersion(ReleaseManager.groovy)
//	at ai.stainless.jenkins.ReleaseManager$buildSemanticVersion.call(Unknown Source)
//	at ReleaseManagerTest.run(ReleaseManagerTest.groovy:169)
//	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
//	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
//	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
//

ReleaseManager.metaClass.getTags = {
    return Tags.parse('''
''')
}

thrown = false
try {
    assert "0.0.1" == new ReleaseManager(new TestScript(BUILD_NUMBER: "21", JOB_NAME: "jobby", BRANCH_NAME: "develop"), "^prefix1").buildSemanticVersion()
} catch (MissingTagException e) {
    thrown = true
    assert e.message =~ /^Prefix provided/
}

assert thrown

//
// test prefixes with paths
//

ReleaseManager.metaClass.getTags = {
    return Tags.parse('''refs/tags/prefix1_0.1.0=550f5a1
refs/tags/prefix-v0.1.1=fdd19b7
refs/tags/prefix1-v0.1.2=8bba2d4
refs/tags/prefix1-v0.1.3=b0bf85b
refs/tags/prefix1-v0.1.6=37eb66a
refs/tags/prefix1-v0.1.4=7a20de3
refs/tags/prefix1-v0.1.7=8fff2ea
refs/tags/prefix1-v0.1.5=7cf62ad
''')
}

thrown = false
try {
    assert "0.2.0" == new ReleaseManager(new TestScript(BUILD_NUMBER: "21", JOB_NAME: "jobby", BRANCH_NAME: "develop"), "prefix1").buildSemanticVersion()
} catch (MissingTagException e) {
    thrown = true
    assert e.message =~ /^Prefix provided/
}

assert !thrown
