
//works with 3.3.9-jdk-8-alpine with -DforkCount=0
podTemplate(label: 'maven', containers: [
  containerTemplate(name: 'maven', image: 'maven:3.5.3-jdk-8', ttyEnabled: true, command: 'cat', 
        resourceRequestCpu: '5000m',
        resourceLimitCpu: '5000m',
        resourceRequestMemory: '4000Mi',
        resourceLimitMemory: '4000Mi')
  ], volumes: [
  hostPathVolume(hostPath: '/mnt/test-resources/01_INSTALLATIONS/ojdbc8/', mountPath: '/mnt/ojdbc')
  ]) {
    stage('download'){
        node('maven'){
        
        container('maven'){
            git 'https://github.com/PSilvestre/pdb'
            sh 'mvn install:install-file -DgroupId=com.oracle.jdbc -DartifactId=ojdbc8 -Dversion=12.2.0.1 -Dpackaging=jar -Dfile=/mnt/ojdbc/ojdbc8.jar'
            sh 'mvn clean install -Dmaven.test.skip=true'
            sh 'mvn test-compile'
            sh 'cp /mnt/ojdbc/kube/config .'
        }
         stash name: 'stuff'
    }
    }
    stage('test'){
        parallel(
            "test mysql":       {test("mysql")},
            "test postgresSQL": {test("postgresql")},
            //"test sqlserver" :  {test("sqlserver")}
            "test db2" :        {test("db2")},
            "test oracle" :     {test("oracle")}   
        )        
    }
    
}

void test(final String profile) {
    node('maven') {
        container('maven'){
            unstash 'stuff'
            sh 'ls src/main/java/com/feedzai/commons/sql/abstraction/util'
            sh 'mvn install:install-file -DgroupId=com.oracle.jdbc -DartifactId=ojdbc8 -Dversion=12.2.0.1 -Dpackaging=jar -Dfile=/mnt/ojdbc/ojdbc8.jar'
            sh "mvn verify -P${profile}"
            
        }
    }
}