package com.feedzai.commons.sql.abstraction.util;

import com.feedzai.commons.sql.abstraction.util.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class TestingDeploymentCreator {

    private static List<KubernetesDBDeployClient> deployments;

    public static void main(String[] args){
        start();
        Scanner sc = new Scanner(System.in);
        sc.nextLine();
        tearDown();
    }


    public static void start(){
        deployments = new LinkedList<KubernetesDBDeployClient>(Arrays.asList(new MySqlKubeClient(), new OracleKubeClient(), new PostGresKubeClient()));

        for(KubernetesDBDeployClient c : deployments) {
           new Thread(() -> {
               c.createDeployment();
               c.createService();
               try {
                   Thread.sleep(c.getSleepTime());
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
               try (FileOutputStream fos = new FileOutputStream(c.getVendor())) {
                   fos.write((c.getFullJDBC() + "\n").getBytes());
               } catch (IOException e) {
                   e.printStackTrace();
               }
           }).start();
        }


    }

    public static void tearDown(){
        for(KubernetesDBDeployClient c : deployments)
            c.tearDown();
    }
}
