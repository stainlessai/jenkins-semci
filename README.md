# jenkins-semci
A shared library for Jenkins pipelines supporting SEMantic versioning and Continuous Integration. 

#  Installation
Install this as a shared pipeline library for Jenkins. See https://jenkins.io/doc/book/pipeline/shared-libraries/

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
```

### ai.stainless.jenkins.ReleaseManager

A class that can be imported into a Jenkins pipeline. Due to Jenkins Groovy sandbox restrictions, Semver objects are difficult
to use directly in the pipeline so this class returns `String` objects.

The ReleaseManager uses GitHub branch and tag data to determine what the version of the build is. 


Methods:
```$groovy
    ReleaseManager(def script)   // Constructor
    String semanticVersion()     // Returns the latest semantic version according to the tag history in the underlying repository
    String artifactName()        // Returns the artifact name. Uses the semver prefix if exists, otherwise, the Jenkins JOB_NAME
    String artifactVersion()     // Returns the latest artifact version according to the GitHub branch and tag data
}

```