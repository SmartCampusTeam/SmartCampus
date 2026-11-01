
pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
    }

    tools {
        maven 'Maven'
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Nettoyage du workspace...'
                deleteDir()

                echo 'Recuperation du code depuis GitHub...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Compilation et generation du package...'
                sh 'mvn -B clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                echo 'Execution des tests unitaires...'
                sh 'mvn -B test'
            }
        }
    }

    post {
        success {
            echo 'Build termine avec succes !'

            archiveArtifacts artifacts: 'target/*.jar',
                             fingerprint: true
        }

        failure {
            echo 'Le pipeline a echoue. Consultez la console pour identifier l erreur.'
        }

        always {
            echo 'Fin du pipeline Jenkins.'
        }
    }
}
