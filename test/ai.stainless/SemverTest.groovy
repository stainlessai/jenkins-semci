import ai.stainless.Branch
import ai.stainless.Semver

def test(string) {
    println "------------ $string ---------------"
    println Semver.fromBranch(string).toMap()
}

test("1.2.0")
test("1.2.0-alpha.1")
test("v1.2.0")
test("kallisto-admin-v1.2.0")
test("origin/kallisto-admin-v1.2.0")
test("origin/kallisto-admin@1.2.0")
test("refs/remotes/release/kallisto-admin@1.2.0")

//try {
//    println Semver.fromBranch("origin/kallisto-admin-v1.2")
//} catch (IllegalArgumentException e) {
//    println "Invalid semver detected: ${e.message}"
//}