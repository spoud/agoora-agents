def getAgents(){
  return ['agoora-pgsql-agent']
}

pipeline {
  agent {
    docker {
      label 'aws-t3-2xlarge'
      image 'spoud/build:java-graalvm-21.0.0.2-java11'
      args '-v /var/run/docker.sock:/var/run/docker.sock --user 0:0'
      alwaysPull true
    }
  }

  environment {
    DOCKER_IMAGE = "spoud/agoora-agents"
    SPOUD_ARTIFACTORY_PASSWORD = credentials('artifactory_password')
    SPOUD_ARTIFACTORY_USER = credentials('artifactory_user')
    GIT_TAG = sh(script: 'git describe --tags --exclude "sdm-*" --abbrev=8', returnStdout: true).trim()
    AGENTS=getAgents()
  }

  stages {
    stage ('Submodule') {
      steps {
        sshagent(credentials: ['802a059b-6d2e-46a9-b4e3-e73f19fd2307']) {
          sh 'git submodule sync'
          sh 'git submodule update --force --init --recursive'
        }
      }
    }

    stage('Build') {
      steps {
        withMaven(mavenSettingsConfig: 'github_package_maven'){
          sh '$MVN_CMD -B -DskipTests clean package'
        }
      }
    }

    stage('Test') {
      steps {
        withMaven(mavenSettingsConfig: 'github_package_maven'){
          sh '$MVN_CMD -B test'
        }
      }
    }

    stage('Sonar pull request') {
      when { changeRequest() }
      steps{
        withCredentials([string(credentialsId: 'agoora_agents_sonar_token', variable: 'SONAR_TOKEN')]) {
            withSonarQubeEnv('SonarCloud') {
                sh "mvn verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar\
                -Dsonar.pullrequest.provider=GitHub \
                -Dsonar.pullrequest.github.repository=SPOUD/agoora-agents \
                -Dsonar.pullrequest.key=${env.CHANGE_ID} \
                -Dsonar.pullrequest.branch=${env.CHANGE_BRANCH}"
            }
        }
      }
    }

    stage('Sonar branch') {
      when { not{ changeRequest() } }
      steps{
        withCredentials([string(credentialsId: 'agoora_agents_sonar_token', variable: 'SONAR_TOKEN')]) {
            withSonarQubeEnv('SonarCloud') {
                sh "mvn org.jacoco:jacoco-maven-plugin:prepare-agent verify sonar:sonar \
                -Dsonar.branch.name=${env.BRANCH_NAME}"
            }
        }
      }
    }

    stage('Build native'){
        steps {
            withMaven(mavenSettingsConfig: 'github_package_maven'){
                sh 'env'
                sh '$MVN_CMD -v'
                sh '$MVN_CMD -B clean package -DskipTests -Pnative'
            }
        }
    }

    stage('Agent docker build'){
        matrix {
            axes {
                axis{
                    name 'AGENT'
                    values 'agoora-pgsql-agent'
                }
            }
            stages{
                stage('Docker build') {
                    when { changeRequest() }
                    steps {
                        dir("${AGENT}"){
                            sh "docker build -t ${AGENT}:test ."
                            sh "docker rmi ${AGENT}:test"
                        }
                    }
                }

                stage('Docker build and publish') {
                when {
                    anyOf {
                        branch 'master'
                        tag "*"
                    }
                }
                steps {
                    dir(agent){
                        withCredentials([usernamePassword(credentialsId: '95c0e4c5-7a97-4c15-a5bf-4c2f1561c762', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                            sh "docker login -u $USER -p $PASS"
                            sh "docker build -t ${AGENT}:${GIT_TAG} ."
                            sh "docker tag ${AGENT}:${GIT_TAG} ${AGENT}:latest"
                            sh "docker push ${AGENT}:${GIT_TAG}"
                            sh "docker push ${AGENT}:latest"
                            sh "docker rmi ${AGENT}:latest"
                            sh "docker rmi ${AGENT}:${GIT_TAG}"
                        }
                    }
                }
            }
        }
    }

    stage ('sdm-docs') {
      when {
        anyOf {
          branch 'master'
          tag "*"
        }
      }
      steps {
          build job: 'sdm-docs', wait: false, parameters: []
      }
    }

  }
}

