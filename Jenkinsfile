#!/usr/bin/env groovy

properties([
        buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10')),
        parameters([
                string(defaultValue: '1', description: 'Nr of concurrent students per docker container', name: 'NR_STUDENTS_PER_CONTAINER', trim: false),
                string(defaultValue: '1', description: 'Nr of concurrent docker containers', name: 'DOCKER_NR_CONTAINERS', trim: false),
                string(defaultValue: '300', description: 'Rampup time in seconds', name: 'RAMPUP_TIME', trim: false),
                string(defaultValue: '60', description: 'Max duration in minutes', name: 'MAX_DURATION', trim: false),
                choice(choices: ['tst'], description: 'Environment to run load test against', name: 'ENV')
                ])
])

node('master') {
    timeout(params.MAX_DURATION.toInteger() + 20) {
        currentBuild.displayName = "#${BUILD_NUMBER}: ${params.SERVER} - ${params.NR_STUDENTS_PER_CONTAINER.toInteger() * params.DOCKER_NR_CONTAINERS.toInteger()} users - feeder start ${params.GATLING_FEEDER_START}"
        String loadtestRoot = ''
        String GATLING_BASEURL
        String AWS_REPORT_BUCKET = "gatling-runner"

        stage('Setup') {
            switch ("${params.SERVER}") {
                case "tst":
                    GATLING_BASEURL = "https://test.com"
                    break;
                default:
                    println("INVALID ENVIRONMENT SELECTED!")
                    sh "exit"
            }

            env.AWS_REGION = "eu-west-1"

            dir(loadtestRoot) {
                sh "wget -q https://s3.amazonaws.com/amazon-ecs-cli/ecs-cli-linux-amd64-latest"
                sh "mv ecs-cli-linux-amd64-latest ecs-cli"
                sh "chmod +x ecs-cli"
                env.PATH = env.WORKSPACE + "/" + loadtestRoot + ":" + env.PATH
                env.PATH = tool("Maven-3.5.2") + "/bin/:" + env.PATH

                sh "ecs-cli --version || exit 1"

                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'codecontrol']]) {

                    sh "ecs-cli configure profile"
                }
            }
        }

        stage('Run loadtest on AWS') {
            dir(loadtestRoot) {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'codecontrol']]) {
                    sh "./scripts/runLoadtestOnAWS.sh -r ${AWS_REPORT_BUCKET} -c ${params.DOCKER_NR_CONTAINERS} -s ${params.NR_STUDENTS_PER_CONTAINER} -d ${params.MAX_DURATION} -ramp ${params.RAMPUP_TIME} -b ${GATLING_BASEURL}"
                }
            }
        }

        stage('Create report') {
            dir(loadtestRoot) {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'codecontrol']]) {
                    sh "./scripts/generateHTMLReport.sh -r ${AWS_REPORT_BUCKET}"
                }
                gatlingArchive()
            }
        }
    }
}
