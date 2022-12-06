pipeline {
    agent {
        node LAUNCH_NODE
    }

    tools {
        jdk 'openjdk-15.0.2'
    }

    environment {
        LOG_FILE_NAME="mewore-web-${env.BUILD_NUMBER}.log"
        LOG_FILE="${env.WORKSPACE}/mewore-web-${env.BUILD_NUMBER}.log"
        DOWNLOADED_JAR_NAME = "${MEWORE_WEB_BUILD_JOBNAME}-${MEWORE_WEB_BUILD_NUMBER}-${JAR_NAME}"
        LAUNCH_COMMAND = [
            'export MEWORE_WEB_RABBIT_DIARY_LOCATION="' + "${env.HOME}" + '/${MEWORE_WEB_RABBIT_DIARY_PATH}"',
            "nohup bash -c \"java -jar '${DOWNLOADED_JAR_NAME}' --spring.profiles.active=common,prod\" > '${LOG_FILE}' &"
        ].join(' && ')
        PROTOCOL = "http"
        PORT = "8001"
    }

    stages {
        stage('Prepare') {
            when {
                expression {
                    return !fileExists("${DOWNLOADED_JAR_NAME}");
                }
            }
            steps {
                script {
                    sh 'pwd'
                    copyArtifacts(projectName: "${MEWORE_WEB_BUILD_JOBNAME}", selector: specific("${MEWORE_WEB_BUILD_NUMBER}"), filter: "build/libs/${JAR_NAME}")
                    sh 'cp "build/libs/${JAR_NAME}" "${DOWNLOADED_JAR_NAME}"'
                    sh 'rm -rf "build"'
                }
            }
        }
        stage('Stop') {
            steps {
                script {
                    processOutput = sh returnStdout: true, script: "ps -C java -u '${env.USER}' -o pid=,command= | grep 'spring.profiles.active=common,prod' | awk '{print \$1;}'"
                    processOutput.split('\n').each { pid ->
                        if (pid.length() > 0) {
                            echo "Killing: ${pid}"
                            killStatus = sh returnStatus: true, script: "kill ${pid}"
                            if (killStatus != 0) {
                                echo "Process ${pid} must have already been stopped."
                            }
                        }
                    }
                    sleep 5
                    curlStatus = sh returnStatus: true, script: "curl --insecure ${PROTOCOL}://localhost:${PORT}"
                    if (curlStatus == 0) {
                        error "The app is still running or something else has taken up port :${PORT}! Kill it manually."
                    }
                    sh "mkdir -p 'old-logs' && mv mewore-web-*.log ./old-logs || echo 'No logs to move.'"
                }
            }
        }
        stage('Launch') {
            steps {
                // https://devops.stackexchange.com/questions/1473/running-a-background-process-in-pipeline-job
                withEnv(['JENKINS_NODE_COOKIE=dontkill']) {
                    script {
                        sh LAUNCH_COMMAND
                    }
                }
            }
        }
        stage('Verify') {
            steps {
                script {
                    sh './gradlew --parallel --no-daemon e2e:run'
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: "${LOG_FILE_NAME}", fingerprint: true
        }
    }
}
