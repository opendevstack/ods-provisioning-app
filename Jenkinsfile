def odsImageTag
def final projectId = 'prov'
def final componentId = 'prov-app'
def final credentialsId = "${projectId}-cd-cd-user-with-password"
def dockerRegistry
node {
  odsImageTag = env.ODS_IMAGE_TAG ?: 'latest'
  dockerRegistry = env.DOCKER_REGISTRY
}

@Library('ods-jenkins-shared-library@2.x') _

// See readme of shared library for usage and customization.
odsPipeline(
  image: "${dockerRegistry}/cd/jenkins-slave-maven:${odsImageTag}",
  projectId: projectId,
  componentId: componentId,
  branchToEnvironmentMapping: [
    'production': 'test',
    '*': 'dev'
  ],
  sonarQubeBranch: '*'
) { context ->
  stageBuild(context)
  stageScanForSonarqube(context)
  stageStartOpenshiftBuild(context)
  stageDeployToOpenshift(context)
}

def stageBuild(def context) {
  def javaOpts = "-Xmx512m"
  def gradleTestOpts = "-Xmx128m"
  def springBootEnv = context.environment
  if (springBootEnv.contains('-dev')) {
    springBootEnv = 'dev'
  }
  stage('Build') {
    sh 'echo ${APP_DNS}'
    sh 'openssl s_client -showcerts -connect ${APP_DNS}:443 < /dev/null | openssl x509 -outform DER > docker/derp.der'
    withEnv(["TAGVERSION=${context.tagversion}", "NEXUS_USERNAME=${context.nexusUsername}", "NEXUS_PASSWORD=${context.nexusPassword}", "NEXUS_HOST=${context.nexusHost}", "JAVA_OPTS=${javaOpts}","GRADLE_TEST_OPTS=${gradleTestOpts}","ENVIRONMENT=${springBootEnv}"]) {
      def status = sh(script: "./gradlew clean build --stacktrace --no-daemon", returnStatus: true)
      if (status != 0) {
        error "Build failed!"
      }
    }
  }
}
