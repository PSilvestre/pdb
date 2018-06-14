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
        boolean internal = Boolean.parseBoolean(System.getProperty("cluster-internal"));
        //List<Thread> threads = new LinkedList<>();
        for(String v : vendors) {
            switch (v.trim()) {
                case "mysql":
                    //threads.add(new Thread(() -> apply(new MySqlKubeClient(), command)));
                    apply(new MySqlKubeClient(), command, internal);
                    break;
                case "sqlserver":
                    //threads.add(new Thread(() -> apply(new SqlServerKubeClient(), command)));
                    apply(new SqlServerKubeClient(), command, internal);
                    break;
                case "oracle":
                    //threads.add(new Thread(() -> apply(new OracleKubeClient(), command)));
                    apply(new OracleKubeClient(), command, internal);
                    break;
                case "postgresql":
                    //threads.add(new Thread(() -> apply(new PostGresKubeClient(), command)));
                    apply(new PostGresKubeClient(), command, internal);
                    break;
                case "db2":
                    //threads.add(new Thread(() -> apply(new Db2KubeClient(), command)));
                    apply(new Db2KubeClient(), command, internal);
                    break;
            }
        }
    }

    private static void apply(KubernetesDBDeployClient c, String command, boolean internal){
        if (command.equalsIgnoreCase(COMMANDS.DEPLOY.name())) {
            c.createDeployment();
            c.createService();
            try {
                Thread.sleep(c.getSleepTime());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try (FileOutputStream fos = new FileOutputStream(c.getVendor())) {
                if(internal) {
                    System.err.println(c.getFullInternalJDBC());
                    fos.write(c.getFullInternalJDBC().getBytes());
                }
                else
                    fos.write((c.getFullJDBC()).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (command.equalsIgnoreCase(COMMANDS.TEARDOWN.name())) {
            c.tearDown();
        }
    }
}
