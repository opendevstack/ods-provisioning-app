def odsImageTag
def final projectId = 'prov'
def final componentId = 'prov-app'
def final credentialsId = "${projectId}-cd-cd-user-with-password"
def dockerRegistry
def odsGitRef
node {
  dockerRegistry = env.DOCKER_REGISTRY
  odsImageTag = env.ODS_IMAGE_TAG ?: '2.x'
  odsGitRef = env.ODS_GIT_REF ?: '2.x'
}

library("ods-jenkins-shared-library@${odsGitRef}")

// See readme of shared library for usage and customization.
odsPipeline(
  podContainers: [
    containerTemplate(
      name: 'jnlp',
      image: "${dockerRegistry}/ods/jenkins-slave-maven:${odsImageTag}",
      workingDir: '/tmp',
      alwaysPullImage: true,
      args: '${computer.jnlpmac} ${computer.name}',
    ),
    containerTemplate(
      name: 'nodejs10',
      image: "${dockerRegistry}/ods/jenkins-slave-nodejs10-angular:${odsImageTag}",
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
        script: "yarn build:prod",
        returnStatus: true
      )
      if (status != 0) {
        error "Build failed!"
      }
    }
  }
}
