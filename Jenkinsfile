def odsImageTag = env.ODS_IMAGE_TAG ?: 'latest'
def odsGitRef = env.ODS_GIT_REF ?: 'production'
def final projectId = 'prov'
def final componentId = 'prov-app'
def final credentialsId = "${projectId}-cd-cd-user-with-password"
def sharedLibraryRepository
def dockerRegistry
node {
  sharedLibraryRepository = env.SHARED_LIBRARY_REPOSITORY
  dockerRegistry = env.DOCKER_REGISTRY
}

library identifier: "ods-library@${odsGitRef}", retriever: modernSCM(
  [$class: 'GitSCMSource',
   remote: sharedLibraryRepository,
   credentialsId: credentialsId])

// See readme of shared library for usage and customization.
odsPipeline(
  podContainers: [
    containerTemplate(
      name: 'jnlp',
      image: "${dockerRegistry}/cd/jenkins-slave-maven:${odsImageTag}",
      workingDir: '/tmp',
      alwaysPullImage: true,
      args: '${computer.jnlpmac} ${computer.name}',
      serviceAccount: 'jenkins'
    ),
    containerTemplate(
      name: 'nodejs10',
      image: "${dockerRegistry}/cd/jenkins-slave-nodejs10-angular:${odsImageTag}",
      workingDir: '/tmp',
      alwaysPullImage: true,
      ttyEnabled: true,
      command: 'cat',
    )
  ],
  projectId: projectId,
  componentId: componentId,
  branchToEnvironmentMapping: [
    'production': 'test',
    'newfrontend': 'nfe',
    '*': 'dev'
  ],
  sonarQubeBranch: '*'
) { context ->
  container('nodejs10') {
    dir('client') {
      stageInstallFrontend(context)
      stageBuildFrontend(context)
    }
  }
  stageBuildBackend(context)
  stageScanForSonarqube(context)
  stageStartOpenshiftBuild(context)
  stageDeployToOpenshift(context)
}

def stageBuildBackend(def context) {
  def javaOpts = "-Xmx512m"
  def gradleTestOpts = "-Xmx128m"
  def springBootEnv = context.environment
  if (springBootEnv.contains('-dev')) {
    springBootEnv = 'dev'
  }
  stage('Build Backend') {
    sh 'echo ${APP_DNS}'
    sh 'openssl s_client -showcerts -connect ${APP_DNS}:443 < /dev/null | openssl x509 -outform DER > docker/derp.der'
    withEnv(["TAGVERSION=${context.tagversion}", "NEXUS_USERNAME=${context.nexusUsername}", "NEXUS_PASSWORD=${context.nexusPassword}", "NEXUS_HOST=${context.nexusHost}", "JAVA_OPTS=${javaOpts}","GRADLE_TEST_OPTS=${gradleTestOpts}","ENVIRONMENT=${springBootEnv}"]) {
      def status = sh(script: "./gradlew clean build --stacktrace --no-daemon", returnStatus: true)
      junit 'build/test-results/test/*.xml'
      if (status != 0) {
        error "Build failed!"
      }
    }
  }
}

def stageInstallFrontend(def context) {
  stage('Install Frontend') {
    withEnv(["NEXUS_USERNAME=${context.nexusUsername}", "NEXUS_PASSWORD=${context.nexusPassword}", "NEXUS_HOST=${context.nexusHost}"]) {
      def status = sh(
        label: "Install Angular Frontend client dependencies",
        script: "yarn",
        returnStatus: true
      )
      if (status != 0) {
        error "Install failed!"
      }
    }
  }
}

def stageBuildFrontend(def context) {
  stage('Build Frontend') {
    withEnv(["NEXUS_USERNAME=${context.nexusUsername}", "NEXUS_PASSWORD=${context.nexusPassword}", "NEXUS_HOST=${context.nexusHost}"]) {
      def status = sh(
        label: "Building Angular Frontend client",
        script: "yarn build",
        returnStatus: true
      )
      // junit 'build/test-results/test/*.xml'
      if (status != 0) {
        error "Build failed!"
      }
      sh "cp -rv dist/client/* ../src/main/resources/static/"
    }
  }
}
