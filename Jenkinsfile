// Define DevCloud Artifactory for publishing non-docker image artifacts
def artUploadServer = Artifactory.server('devcloud')

// Change Snapshot to your own DevCloud Artifactory repo name
def Snapshot = 'PROPEL'

pipeline {
    options {
        buildDiscarder(logRotator(artifactDaysToKeepStr: '1', artifactNumToKeepStr: '1', daysToKeepStr: '5', numToKeepStr: '10'))
    }
    agent {
        docker {
            image 'maven:3.5'
            label 'dind'
            args '-v /root/.m2:/root/.m2'
        }
    }
    stages {
        stage('Test branch') {
            steps {
                checkout scm
                dir('spring-filters-config') {
                    git branch: 'master', changelog: false, credentialsId: 'github.build.ge.com', poll: false, url: 'https://github.build.ge.com/predix/spring-filters-config.git'
                }
                sh '''#!/bin/bash -ex
                    unset NON_PROXY_HOSTS
                    unset HTTPS_PROXY_PORT
                    unset HTTPS_PROXY_HOST

                    mvn clean verify -s spring-filters-config/mvn_settings_noproxy.xml
                '''
                // No Test reports
            }
        }
        stage('Maven push if master') {
            when {
                anyOf { branch 'master' }
            }
            environment {
                DEPLOY_CREDS = credentials('predix-artifactory-uploader')
            }
            steps {
                sh '''#!/bin/bash -ex
                    source spring-filters-config/set-env-metering-filter.sh
                    unset NON_PROXY_HOSTS
                    unset HTTPS_PROXY_PORT
                    unset HTTPS_PROXY_HOST

                    mvn  -B -s spring-filters-config/mvn_settings_noproxy.xml -DaltDeploymentRepository=artifactory.releases::default::https://devcloud.swcoe.ge.com/artifactory/MAAXA-MVN  -Dartifactory.password=${DEPLOY_CREDS_PSW} clean deploy

                    mvn -B clean deploy -P release -s spring-filters-config/mvn_settings_noproxy.xml -D stagingProfileId=14c243d3be5b9e

                '''
            }
        }
    }
}
