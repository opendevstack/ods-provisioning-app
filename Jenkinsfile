def odsNamespace
def odsGitRef
def odsImageTag
node {
  odsNamespace = env.ODS_NAMESPACE ?: 'ods'
  odsImageTag = env.ODS_IMAGE_TAG ?: 'latest'
  odsGitRef = env.ODS_GIT_REF ?: 'master'
}

library("ods-jenkins-shared-library@${odsGitRef}")

odsComponentPipeline(
  imageStreamTag: "${odsNamespace}/jenkins-agent-maven:${odsImageTag}",
  branchToEnvironmentMapping: [:]
) { context ->
  stageBuild(context)
  odsComponentStageScanWithSonar(context, [branch: '*'])
  odsComponentStageBuildOpenShiftImage(context, [branch: '*'])
  stageTagImageWithBranch(context)
}

def stageBuild(def context) {
  def javaOpts = "-Xmx512m"
  def gradleTestOpts = "-Xmx128m"
  def springBootEnv = context.environment
  if (springBootEnv.contains('-dev')) {
    springBootEnv = 'dev'
  }
  stage('Build and Unit Test') {
    withEnv(["TAGVERSION=${context.tagversion}", "NEXUS_USERNAME=${context.nexusUsername}", "NEXUS_PASSWORD=${context.nexusPassword}", "NEXUS_HOST=${context.nexusHost}", "JAVA_OPTS=${javaOpts}","GRADLE_TEST_OPTS=${gradleTestOpts}","ENVIRONMENT=${springBootEnv}"]) {
      def status = sh(script: '''
        ./gradlew --version
        java -version

        retryNum=0
        downloadResult=1
        while [ 0 -ne $downloadResult ] && [ 5 -gt $retryNum ]; do
            ./gradlew dependencies
            downloadResult=$?
            let "retryNum=retryNum+1"
        done
        
        ./gradlew clean build --stacktrace --no-daemon
      ''', returnStatus: true)
      if (status != 0) {
        error "Build failed!"
      }
    }
  }
}

def stageTagImageWithBranch(def context) {
  stage('Tag created image') {
    def targetImageTag = context.gitBranch.replace('/','_').replace('-','_')
    sh(
      script: "oc -n ${context.cdProject} tag ${context.componentId}:${context.shortGitCommit} ${context.componentId}:${targetImageTag}",
      label: "Set tag '${targetImageTag}' on is/${context.componentId}"
    )
  }
}
