@Library('jenkins-semci')
import ai.stainless.jenkins.ReleaseManager
import ai.stainless.SemverFormatter

// create a ReleaseManager tied to the prefix for this subproject
def releaseManager = new ReleaseManager(this)
releaseManager.prerelease = '%BRANCH_NAME%-%BUILD_NUMBER%'

pipeline {
  agent any

  environment { }

  stages {
    stage('Build') {
      steps {
        script {
          // The version in the app needs to use a "+" to separate the build number
          // but Docker doesn't support that, so we render it using a custom formatter here
          // but use a dash in Dockerhub
          flutterVersionString = SemverFormatter.ofPattern("M.m.p'-'?P'+'?B").format(releaseManager.artifact())
        }
        sh "yq e -i '.version=\"${flutterVersionString}\"' pubspec.yaml"
        // uncomment the path line and comment out the git line if pulling manually
       }
    }
  }
}
