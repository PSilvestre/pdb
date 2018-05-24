package com.feedzai.commons.sql.abstraction.util;

import com.feedzai.commons.sql.abstraction.util.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

public class TestMainMySql {

    private static MySqlKubeClient cMySQL;
    private static SqlServerKubeClient cSQLServer;
    private static Db2KubeClient cdb2;
    private static OracleKubeClient cOrcl;
    private static PostGresKubeClient cPost;

    public static void main(String[] args){
        start();

    }

    public static void start(){
        cMySQL =  new MySqlKubeClient();
        String mysqlIp = cMySQL.createMySqlDeploymentAndService();
        String mysqlJDBC = "mysql=jdbc:mysql://"+mysqlIp+"/mysql?useSSL=false";
        cSQLServer = new SqlServerKubeClient();
        String sqlServerIp = cSQLServer.createSqlServerDeploymentAndService();
        String sqlServerJDBC = "sqlserver=jdbc:sqlserver://"+sqlServerIp;
        cdb2 = new Db2KubeClient();
        String db2Ip = cdb2.createDB2DeploymentAndService();
        String db2JDBC = "db2=jdbc:db2://"+db2Ip+"/testdb";
        cOrcl = new OracleKubeClient();
        String orclIp = cOrcl.createOracleDeploymentAndService();
        String orclJDBC = "oracle=jdbc:oracle:thin:@(DESCRIPTION=(ENABLE=broken)(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST="+orclIp.substring(0,orclIp.indexOf(":"))+")(PORT="+orclIp.substring(orclIp.indexOf(":")+1)+")))(CONNECT_DATA=(SID=orcl)))";
        cPost = new PostGresKubeClient();
        String postIp = cPost.createPostgresqlDeploymentAndService();
        String postGresJDBC = "postgres=jdbc:postgresql://"+postIp+"/postgres";
        try (FileOutputStream fos = new FileOutputStream("locations")) {
            fos.write((mysqlJDBC+"\n"+sqlServerJDBC+"\n"+db2JDBC+"\n"+orclJDBC+"\n"+postGresJDBC+"\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void tearDown(){
        cMySQL.tareDown();
        cSQLServer.tareDown();
        cdb2.tareDown();
        cOrcl.tareDown();
        cPost.tareDown();
    }
}
