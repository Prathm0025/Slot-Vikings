def PROJECT_NAME = "Slot-Vikings"
def UNITY_VERSION = "2022.3.48f1"
def UNITY_INSTALLATION = "C:\\Program Files\\Unity\\Hub\\Editor\\${UNITY_VERSION}\\Editor\\Unity.exe"
def REPO_URL = "git@github.com:Prathm0025/Slot-Vikings.git"

pipeline {
    agent any

    options {
        timeout(time: 60, unit: 'MINUTES')
    }

    environment {
        PROJECT_PATH = "D:\\Slot-Vikings"
        S3_BUCKET = "vikingsbucket"
        REPO_URL = "git@github.com:Prathm0025/Slot-Vikings.git"
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    
                    bat 'whoami'
                    bat 'cd /d D:\\'

                    if (fileExists(PROJECT_PATH + '\\.git')) {
                        dir(PROJECT_PATH) {
                            bat '''
                            git fetch --all
                            git reset --hard origin/develop
                            git checkout develop
                            '''
                        }
                    } else {
                        bat '''
                        git config --global http.postBuffer 3221225472
                        git clone ${REPO_URL} D:\\Slot-Vikings
                        cd D:\\Slot-Vikings
                        git checkout develop
                        '''
                    }
                }
            }
        }

        stage('Build WebGL') {
            steps {
                script {
                    withEnv(["UNITY_PATH=${UNITY_INSTALLATION}"]) {
                        bat '''
                        "%UNITY_PATH%" -quit -batchmode -projectPath "%PROJECT_PATH%" -executeMethod BuildScript.BuildWebGL -logFile -
                        '''
                    }
                }
            }
        }

        stage('Push Build to GitHub') {
            steps {
                script {
                    dir("${PROJECT_PATH}") {
                        bat '''
                        hostname
                        git clean -fd
                        git stash --include-untracked
                        git checkout main 
                        git rm -r -f Builds 
                        git add .
                        git commit -m "delete old Builds"
                        git push origin main

                        git checkout main
                        git checkout develop -- Builds
                        git add -f Builds
                        git commit -m "adding new Builds"
                        git push origin main
                        '''
                    }
                }
            }
        }

        stage('Deploy to S3') {
            steps {
                script {
                    dir("${PROJECT_PATH}") {
                        bat '''
                        
                        aws s3 cp "Builds/WebGL/" s3://%S3_BUCKET%/ --recursive --acl public-read

                        
                        aws s3 cp "Builds/WebGL/index.html" s3://%S3_BUCKET%/index.html --acl public-read

                        
                        aws s3 website s3://%S3_BUCKET%/ --index-document index.html --error-document index.html
                        '''
                    }
                }
            }
        }
    }
}
