def final projectId = 'prov'
def final componentId = 'prov-app'
def final groupId = 'com.bix-digital.prov'
def final credentialsId = "${projectId}-cd-cd-user-with-password"
def sharedLibraryRepository
def dockerRegistry
def nexusUsername
def nexusPassword
node {
  sharedLibraryRepository = env.SHARED_LIBRARY_REPOSITORY
  dockerRegistry = env.DOCKER_REGISTRY
  nexusUsername = env.NEXUS_USERNAME
  nexusPassword = env.NEXUS_PASSWORD
}

library identifier: 'ods-library@0.1-latest', retriever: modernSCM(
  [$class: 'GitSCMSource',
   remote: sharedLibraryRepository,
   credentialsId: credentialsId])

// See readme of shared library for usage and customization.
odsPipeline(
  image: "${dockerRegistry}/cd/jenkins-slave-maven",
  projectId: projectId,
  componentId: componentId,
  groupId: groupId,
  testProjectBranch: 'master',
  verbose: true,
) { context ->
  stageBuild(context)
  stageScanForSonarqube(context)
  stageCreateOpenshiftEnvironment(context)
  stageStartOpenshiftBuild(context)
  stageDeployToOpenshift(context)
  stageTriggerAllBuilds(context)
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
      sh "./gradlew clean build --stacktrace --no-daemon"
    }
    sh "cp build/libs/${context.componentId}-*.jar docker/app.jar"
  }
}
