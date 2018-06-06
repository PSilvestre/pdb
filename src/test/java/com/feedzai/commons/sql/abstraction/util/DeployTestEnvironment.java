package com.feedzai.commons.sql.abstraction.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class DeployTestEnvironment {
    public enum COMMANDS {DEPLOY, TEARDOWN};

    public static void main(String[] args){
        String command = args[0];
        String[] vendors = System.getProperty("instances").split(",");
        //List<Thread> threads = new LinkedList<>();
        for(String v : vendors) {
            switch (v.trim()) {
                case "mysql":
                    //threads.add(new Thread(() -> apply(new MySqlKubeClient(), command)));
                    apply(new MySqlKubeClient(), command);
                    break;
                case "sqlserver":
                    //threads.add(new Thread(() -> apply(new SqlServerKubeClient(), command)));
                    apply(new SqlServerKubeClient(), command);
                    break;
                case "oracle":
                    //threads.add(new Thread(() -> apply(new OracleKubeClient(), command)));
                    apply(new OracleKubeClient(), command);
                    break;
                case "postgresql":
                    //threads.add(new Thread(() -> apply(new PostGresKubeClient(), command)));
                    apply(new PostGresKubeClient(), command);
                    break;
                case "db2":
                    //threads.add(new Thread(() -> apply(new Db2KubeClient(), command)));
                    apply(new Db2KubeClient(), command);
                    break;
            }
        }
        /*for(Thread t : threads)
            t.start();

        for(Thread t : threads) {//Wait for threads to finish;
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/
    }

    private static void apply(KubernetesDBDeployClient c, String command){
        if (command.equalsIgnoreCase(COMMANDS.DEPLOY.name())) {
            c.createDeployment();
            c.createService();
            try {
                Thread.sleep(c.getSleepTime());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try (FileOutputStream fos = new FileOutputStream(c.getVendor())) {
                    fos.write((c.getFullJDBC()).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (command.equalsIgnoreCase(COMMANDS.TEARDOWN.name())) {
            c.tearDown();
        }
    }
}
