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
        PROJECT_PATH = "C:\\${PROJECT_NAME}" 
        S3_BUCKET = "vikingsbucket" // Define your bucket name here
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    dir("${PROJECT_PATH}") { 
                        git url: REPO_URL, branch: 'develop'
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
                            git config user.email "prathamesh@underpinservices.com"
                            git config user.name "Prathm0025"
                            git checkout main
                            rmdir /S /Q Builds
                            git checkout --develop Builds
                            git add Builds
                            git commit -m "Add build" || echo "No changes to commit"
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
