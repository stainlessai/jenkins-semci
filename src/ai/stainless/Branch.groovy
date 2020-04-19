package ai.stainless;

class Branch {

//    static def REGEX = ~/^(.*-)?\/(.*-)?@?([0-9]*)\.([0-9]*)\.0/
    static def REGEX_STR = '^(?!.*/\\.)(?!.*\\.\\.)(?!/)(?!.*//)(?!.*@\\{)(?!@$)(?!.*\\\\)[^\000-\037\177 ~^:?*\\[]+/[^\000-\037\177 ~^:?*\\[]+(?<!\\.lock)(?<!/)(?<!\\.)'
    static def REGEX = ~REGEX_STR

    def remote = 'origin'
    Semver semver

    static Branch fromBranchName(String branch) {
        def matcher = branch =~ REGEX

        if (matcher.matches()) {
//            (0..matcher.groupCount()-1).each {
//                println "$it": matcher.group(it)
//            }

            return new Branch(remote:'origin', semver:Semver.fromBranch(branch))
        }
        
        throw new IllegalArgumentException("Unable to parse ReleaseBranch: $branch")
    }

    String toString() {
        return "$remote/@$semver"
    }

    Map toMap() {
        this.properties.subMap(['remote']) + semver.toMap()
    }
}
