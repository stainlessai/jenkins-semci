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
cmd.consumeProcessOutput( out, err )
cmd.waitFor()
def lines = out.readLines()
println lines.collect { Semver.fromRef(it.replaceAll('\'',''),true) }.last().toMap()

class TestScript {
    public TestScript(Map env) {
        this.env = env
    }
    def env = [:]
    def sh(Map params)  {
        def cmd = params.script.execute()
        def out = new StringBuffer()
        def err = new StringBuffer()
        cmd.consumeProcessOutput( out, err )
        cmd.waitFor()
        return out.toString()
    }
}

println ">>>\n\n"
println new ReleaseManager(new TestScript(BUILD_NUMBER:"18",JOB_NAME:"jobby",BRANCH_NAME:"master")).artifactName()
println new ReleaseManager(new TestScript(BUILD_NUMBER:"256",JOB_NAME:"jobby",BRANCH_NAME:"develop")).artifactName()
println new ReleaseManager(new TestScript(BUILD_NUMBER:"512",JOB_NAME:"jobby",BRANCH_NAME:"v1.2.3")).artifactName()
println new ReleaseManager(new TestScript(BUILD_NUMBER:"1024",JOB_NAME:"jobby",BRANCH_NAME:"jobbyjob-v4.5.6")).artifactName()
println new ReleaseManager(new TestScript(BUILD_NUMBER:"1024",JOB_NAME:"jobby",BRANCH_NAME:"jobbyjob@4.5.6")).artifactName()

//try {
//    println Semver.fromBranch("origin/kallisto-admin-v1.2")
//} catch (IllegalArgumentException e) {
//    println "Invalid semver detected: ${e.message}"
//}