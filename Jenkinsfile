def odsNamespace = env.ODS_NAMESPACE ?: 'ods'
def odsImageTag = env.ODS_IMAGE_TAG ?: 'latest'
def odsGitRef = env.ODS_GIT_REF ?: 'production'

library("ods-jenkins-shared-library@${odsGitRef}")

odsComponentPipeline(
  imageStreamTag: "${odsNamespace}/jenkins-slave-maven:${odsImageTag}",
  componentId: 'prov-app',
  branchToEnvironmentMapping: [
    'production': 'test',
    '*': 'dev'
  ],
  sonarQubeBranch: '*'
) { context ->
  stageBuild(context)
  odsComponentStageScanWithSonar(context)
  odsComponentStageBuildOpenShiftImage(context)
  odsComponentStageRolloutOpenShiftDeployment(context)
}

def stageBuild(def context) {
  def javaOpts = "-Xmx512m"
  def gradleTestOpts = "-Xmx128m"
  def springBootEnv = context.environment
  if (springBootEnv.contains('-dev')) {
    springBootEnv = 'dev'
  }
  stage('Build') {
    withEnv(["TAGVERSION=${context.tagversion}", "NEXUS_USERNAME=${context.nexusUsername}", "NEXUS_PASSWORD=${context.nexusPassword}", "NEXUS_HOST=${context.nexusHost}", "JAVA_OPTS=${javaOpts}","GRADLE_TEST_OPTS=${gradleTestOpts}","ENVIRONMENT=${springBootEnv}"]) {
      def status = sh(script: "./gradlew clean build --stacktrace --no-daemon", returnStatus: true)
      if (status != 0) {
        error "Build failed!"
      }
    }
  }
}
