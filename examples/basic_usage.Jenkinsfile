@Library('jenkins-semci')
import ai.stainless.jenkins.ReleaseManager

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