@Library('jenkins-semci')
import ai.stainless.jenkins.ReleaseManager
import ai.stainless.SemverFormatter

def releaseManager = new ReleaseManager(this)

pipeline {
  agent any

  stages {
    stage('Build') {
      steps {
        echo "${releaseManager.artifactVersion()}"
      }
    }
  }
}