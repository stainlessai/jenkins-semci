import ai.stainless.Branch
import ai.stainless.Semver
import ai.stainless.jenkins.ReleaseManager

def test(string) {
    println "------------ $string ---------------"
    println Semver.fromRef(string).toMap()
}

test("1.2.0")
test("1.2.0-alpha.1")
test("v1.2.0")
test("kallisto-admin-v1.2.0")
test("origin/kallisto-admin-v1.2.0")
test("origin/kallisto-admin@1.2.0")
test("refs/remotes/release/kallisto-admin@1.2.0")

test("refs/tags/kallisto-scheduler@0.2.6")

// not supported
//test("refs/tags/helm_kallisto_0.2.3")

test("refs/tags/v0.0.1")

//def cmd = "ls -al".execute()
//def out = new StringBuffer()
//def err = new StringBuffer()
//cmd.consumeProcessOutput( out, err )
//cmd.waitFor()
//def result = out.toString()
//println result

println "---"
def cmd = "git for-each-ref --sort=creatordate --format '%(refname)' refs/tags".execute()
def out = new StringBuffer()
def err = new StringBuffer()
cmd.consumeProcessOutput(out, err)
cmd.waitFor()
def lines = out.readLines()
println lines.collect { Semver.fromRef(it.replaceAll('\'', ''), true) }.last().toMap()

assert new Semver().versionString() == '0.1.0'
assert new Semver(major: 3, minor: 2, patch: 1).versionString() == '3.2.1'

assert new Semver(major: 0, minor: 1, patch: 1) > new Semver()
assert new Semver(major: 0, minor: 1, patch: 1) > new Semver(major: 0, minor: 1, patch: 0)
assert new Semver(major: 2, minor: 1, patch: 1) > new Semver(major: 0, minor: 1, patch: 0)
assert new Semver(major: 1, minor: 0, patch: 0) > new Semver(major: 0, minor: 10, patch: 0)

def v1 = Semver.parse("0.1.0-develop-SNAPSHOT+1")
assert v1.prerelease == 'develop-SNAPSHOT'
assert v1.buildMetadata == '1'

def v2 = Semver.parse("myprefix-v0.1.0+1")
assert v2.prerelease == null
assert v2.buildMetadata == '1'

def v3 = Semver.parse("myprefix@1.0.0")
assert v3.prerelease == null
assert v3.buildMetadata == null
assert v3.toString() == "myprefix-1.0.0"

//
// Compare to tests
//
assert new Semver().compareTo(new Semver()) == 0
assert new Semver(major: 0, minor: 1, patch: 0).compareTo(new Semver()) == 0
assert new Semver(major: 0, minor: 1, patch: 2).compareTo(new Semver()) == 1
assert new Semver(major: 0, minor: 0, patch: 3).compareTo(new Semver()) == -1
assert new Semver(major: 0, minor: 1, patch: 3).compareTo(new Semver()) == 1
assert new Semver(major: 0, minor: 1, patch: 1).compareTo(new Semver(major: 0, minor: 1, patch: 1)) == 0
assert new Semver(major: 0, minor: 10, patch: 1).compareTo(new Semver(major: 0, minor: 1, patch: 1)) == 1
assert new Semver(major: 0, minor: 10, patch: 1).compareTo(new Semver(major: 0, minor: 3, patch: 1)) == 1
assert new Semver(major: 0, minor: 10, patch: 0).compareTo(new Semver(major: 0, minor: 3, patch: 2)) == 1
assert new Semver(major: 0, minor: 10, patch: 100).compareTo(new Semver(major: 0, minor: 3, patch: 2)) == 1
assert new Semver(major: 0, minor: 3, patch: 2).compareTo(new Semver(major: 0, minor: 10, patch: 100)) == -1

assert new Semver(major: 1, minor: 0, patch: 0).compareTo(new Semver(major: 0, minor: 12, patch: 1)) == 1

thrown = false
try {
    test("1.9")
} catch (java.lang.IllegalArgumentException e) {
    thrown = true
}

assert thrown

thrown = false
try {
    test(".1.9.0")
} catch (java.lang.IllegalArgumentException e) {
    thrown = true
}

assert thrown
