@Library('jenkins-semci')
import ai.stainless.jenkins.ReleaseManager
import ai.stainless.SemverFormatter

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
          def semver = releaseManager.artifact()
          semver.prerelease = "${env.BRANCH_NAME}"
          echo "prerelease is ${semver.prerelease}"
          semver.buildMetadata = "${currentBuild.number}"
          echo "buildMetadata is ${semver.buildMetadata}"
          versionString = SemverFormatter.ofPattern("M.m.p'-'?P'+'?B").format(semver)
          sh "yq e -i '.version=\"${versionString}\"' pubspec.yaml"
        }
    }
  }
}
