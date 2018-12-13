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
  testProjectBranch: 'production',
  verbose: true,
) { context ->
  stageBuild(context)
  stageScanForSonarqube(context)
  stageCreateOpenshiftEnvironment(context)
  stageStartOpenshiftBuild(context)
  stageDeployToOpenshift(context)
  stageTriggerAllBuilds(context)
  stageCollectBuildArtifacts(context)
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
      junit 'build/test-results/test/*.xml'
      if (status != 0) {
        error "Build failed!"
      }
    }
  }
}

// as long as there is no support in shared lib .. 
def stageCollectBuildArtifacts(def context) 
{
  stage('Collect and archive') {
    // we need to get the sq project name - people could modify it
    sq_props = readProperties file: 'sonar-project.properties'
    sonarProjectKey = sq_props['sonar.projectKey']    
    withEnv (["SQ_PROJECT=${sonarProjectKey}"]) 
    {
      withSonarQubeEnv('SonarServerConfig') 
      {
          sh "java -jar /usr/local/cnes/cnesreport.jar -s $SONAR_HOST_URL -t $SONAR_AUTH_TOKEN -p $SQ_PROJECT"
          archiveArtifacts '*-analysis-report.docx*'
      }
    }
    // archive the jenkinsfile (== install plan)
    archiveArtifacts 'Jenkinsfile'
    
    // archive build log
  }
}
