def PROJECT_NAME = "Slot-Vikings"
def UNITY_VERSION = "2022.3.48f1"
def UNITY_INSTALLATION = "C:\\Program Files\\Unity\\Hub\\Editor\\${UNITY_VERSION}\\Editor\\Unity.exe"
def REPO_URL = "https://github.com/DingDingHouse/Slot-Vikings.git"

pipeline {
    agent {
        label 'windows' // Specify the agent by label
    }

    options {
        timeout(time: 60, unit: 'MINUTES') // Set a timeout for the entire build
    }
    
    environment {
        PROJECT_PATH = "C:\\${PROJECT_NAME}" 
        Token = credentials('GITHUB_Ekansh') 
        S3_BUCKET = "vikingsbucket" // Define your bucket name here
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    dir("${PROJECT_PATH}") { 
                         git url: REPO_URL, branch: 'develop', credentialsId: 'GITHUB_Ekansh'
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
                            git init
                            git config user.email "ekansh.underpinservices@gmail.com"
                            git config user.name "Ekansh"
                            git config --global core.autocrlf true
                            git add -f Builds
                            git commit -m "Add build"
                            git remote set-url origin https://github.com/DingDingHouse/Slot-Vikings.git
                            git push https://${Token}@github.com/DingDingHouse/Slot-Vikings.git develop --force
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
                        REM Copy all files, including .html files, to S3
                        aws s3 cp "Builds/WebGL/" s3://%S3_BUCKET%/ --recursive --acl public-read
                        
                        REM Move index.html to the root for S3 hosting
                        aws s3 cp "Builds/WebGL/index.html" s3://%S3_BUCKET%/index.html --acl public-read
                        
                        REM Optional: Set S3 bucket for static web hosting
                        aws s3 website s3://%S3_BUCKET%/ --index-document index.html --error-document index.html
                        '''
                    }
                }
            }
        }
    }
}
