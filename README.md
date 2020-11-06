# jenkins-semci
A shared library for Jenkins pipelines providing classes and methods supporting SEMantic versioning and Continuous Integration. 

#  Installation
Install this as a shared pipeline library for Jenkins. See https://jenkins.io/doc/book/pipeline/shared-libraries/

# Methodology & Features

For a detailed description, see [Stainless AI Dev Flow](https://dev-flow.readthedocs.io).

Summary:
- All released versions are released from a single "master" branch
- All released versions have tags
- All other artifacts are prerelease versions
- Each build creates a unique artifact name based on project name, branch name, build number and semantic version

This library helps enforce these points, calculating version and artifact names automatically and throwing errors
when required conditions are not met.

Features:
- Parses and understand common semantic version formats in tags and branches like vX.Y.Z and projectName-vX.Y.Z
- Supports prefixes like prefix@X.Y.X and prefix-vX.Y.Z
- Automatically extracts and checks commit hashes against tags to ensure release builds are correct
- Supports ordering of version both by time applied and natural semantic ordering
- Automatically ties artifact versions to branch versions where appropriate

What this library does NOT do:
- Manage branches and tags to ensure versions are what you WANT. Learn the method and do it manually, or use a tool.

## Example
In the configuration of your pipeline:
Pipeline Libraries
- Name: 'jenkins-semci'
- Default version: 'master'
- Source Code Management: Modern SCM: GitHub (select credentials and set URL here)

# Usage
Include the library in your plugin:
```$groovy
@Library('jenkins-semci')
import ai.stainless.jenkins.ReleaseManager
// other imports
```             

You can then use the library's objects in your pipeline:
```$groovy 
def releaseManager = new ReleaseManager(this)         

pipeline {
  // my pipeline
}
```        
## Classes
### ai.stainless.Semver

Class that parses and represents a semantic version. Defaults to `0.0.1`. Supports prefixes and the common delimeters `@` and `v`. For example, the following are
all valid, parse-able semantic versions:

```$text
1.2.3
1.2.3-prerelease.alpha.1
v1.2.3
@1.2.3                  
myproject-v1.2.3
myproject@1.2.3-prerelease.alpha.1     
/my/path/before/my/prefix-v1.2.3
```                               

Properties:
```$groovy
    def path                    // prefix path, optional slash delimited path with last element being prefix 
    def prefix                  // prefix, a string identifier before the version
    def prefixDelim = '-'       // delimiter to separate the prefix and version in artifact names
    def preReleaseDelim = '-'   // delimited to separate the version and prerelease string in artifact names
    def v = null                // expected version delimiter, currently supports 'v' and '@'
    int major = 0                // the major version
    int minor = 0                // the minor version
    int patch = 1                // the patch version
    def prerelease              // the prerelease string
```

Methods:
```$groovy
    void bumpPatch()                    // bump the patch version in this Semver         
    void bumpMinor()                    // bump minor and set patch to 0
    void bumpMajor()                    // bump major and set minor and patch to 0

    static Semver parse(                // parse the supplied semverString and return a Semver object.
        String semverString,            //  If ignoreErrors = false throw IllegalArgumentExecption when a parse error occurs
        boolean ignoreErrors = false)   

    static Semver fromRef(              // parse the supplied GitHub ref into a Semver object. Supports branches, tags and remotes.
        String ref,                     // the path variable will contain the github ref path.
        boolean ignoreErrors = false) 

    String artifactName()               // Return a string representing this artifact in a filename-friendly manner.
    String versionString()              // Return only the version if a prefix is present
    Map toMap()                         // Return a map with the property values (filtered to remove variables like "class", etc).   

    int compareTo(Semver o)             // Implements Comparable against other semantic versions
```

### ai.stainless.jenkins.ReleaseManager

A class that can be imported into a Jenkins pipeline. Due to Jenkins Groovy sandbox restrictions, Semver objects are difficult
to use directly in the pipeline so this class returns `String` objects.

The ReleaseManager uses GitHub branch and tag data to determine what the version of the build is. For a detailed explanation
of the CI/CD rules that inspired these tools, see [CI/CD Pipeline (PROPOSED)](https://stainlesscode.atlassian.net/wiki/spaces/STAT/pages/560922625/CI+CD+Pipeline+PROPOSED)

You can pass the values returned by these methods into your build script to set the filename, for example:

```$bash 
pipeline {
  steps {
    step {
      sh "./myscript --artifactName=${releaseManager.artifactName()} --version=${releaseManager.artifactVersion()}"
    }
  }
}
```

The release manager will examine the build properties, git repo branch and tags and return the expected artifact name and version. 
For example, if we are working in a repo called "example" with a Jenkins job of "example":

| Branch | Last Tag (by time) | Last Tag (by version) | BUILD_NUMBER | artifactName() | artifactVersion() |
|---|---|---|---|---|---|
| master | (none) | (none) | (any) | ERROR: No tag | ERROR: No tag | 
| develop | (none) | (none) | 1 | example-0.0.1-1-SNAPSHOT | 0.0.1-1-SNAPSHOT |
| master | v1.2.3 | v1.2.3 | (any) | example-1.2.3 | 1.2.3 |
| master | myprefix@1.2.3 | myprefix@1.2.3 | (any) | myprefix-1.2.3 | 1.2.3 |
| develop | myprefix-v1.2.3 | myprefix-v1.2.3 | (any) | myprefix-1.3.0-develop-SNAPSHOT | 1.3.0-develop-SNAPSHOT
| v2.0.0 | v1.9.5 | v1.9.5 | (any) | example-2.0.0 | 2.0.0 |
| origin/myprefix@2.0.0 | myprefix@2.0.6 | myprefix@2.0.6 | (any) | myprefix-2.0.6 | 2.0.6 |
| origin/myprefix@2.1.0 | myprefix@2.0.6 | | (any) | myprefix-2.1.0 | 2.1.0 |
| origin/myprefix@2.0.1 | (any) | (any) |(any)| ERROR: Invalid branch name | ERROR: Invalid branch name |

Methods:
```$groovy
    ReleaseManager(def script)   // Constructor
    String artifactName()        // Returns the artifact name. Uses the semver prefix if exists, otherwise, the Jenkins JOB_NAME
    String artifactVersion()     // Returns the latest artifact version according to the GitHub branch and tag data
```