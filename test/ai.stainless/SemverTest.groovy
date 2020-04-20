import ai.stainless.Branch
import ai.stainless.Semver

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

def cmd = "git for-each-ref --sort=creatordate --format '%(refname)' refs/tags".execute()
def out = new StringBuffer()
def err = new StringBuffer()
cmd.consumeProcessOutput( out, err )
cmd.waitFor()
def result = out.toString()
println result
println result.lines().collect { Semver.fromRef(it.replaceAll('\'',''),true) }.last().toMap()


//try {
//    println Semver.fromBranch("origin/kallisto-admin-v1.2")
//} catch (IllegalArgumentException e) {
//    println "Invalid semver detected: ${e.message}"
//}