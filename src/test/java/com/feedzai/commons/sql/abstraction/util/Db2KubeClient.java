package com.feedzai.commons.sql.abstraction.util;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class Db2KubeClient {
    private Config config;
    private KubernetesClient client;
    private Deployment deployment;
    private Service service;
    private Pod pod;

    public Db2KubeClient() {
        config = new ConfigBuilder().build();
        client = new DefaultKubernetesClient(config);
        ServiceAccount fabric8 = new ServiceAccountBuilder().withNewMetadata()
                .withName("fabric8").endMetadata().build();
        client.serviceAccounts().inNamespace("default")
                .createOrReplace(fabric8);
        deployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName("db2-dep")
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("app", "db2")
                .endMetadata().withNewSpec()
                .addNewContainer()
                .withName("db2")
                .withImage("ibmcom/db2express-c:10.5.0.5-3.10.0")
                .addNewPort()
                .withContainerPort(50000)
                .endPort()
                .addNewEnv()
                .withName("LICENSE")
                .withValue("accept")
                .endEnv()
                .addNewEnv()
                .withName("DB2INST1_PASSWORD")
                .withValue("db2inst1-pwd")
                .endEnv()
                .addToCommand("/bin/bash", "-c", "echo", "-e", "\"$DB2INST1_PASSWORD\\n$DB2INST1_PASSWORD\" | passwd db2inst1;", "/usr/bin/su - db2inst1 -c \'db2start; db2 create database testdb; tail -f /dev/null\'")
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
        System.err.println("COMMAND: " + deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getCommand());
        service = new ServiceBuilder()
                .withNewMetadata()
                .withName("db2")
                .endMetadata().withNewSpec().withType("NodePort").addNewPort()
                .withPort(50000).withNewTargetPort(50000).endPort()
                .addToSelector("app", "db2").endSpec().build();

    }

    public String createDB2DeploymentAndService() {
        deployment = client.extensions().deployments().inNamespace("default")
                .create(deployment);
        String loc = null;
        while (loc == null)
            for (Pod p : client.pods().list().getItems())
                if (p.getMetadata().getName().startsWith("db2")) {
                    loc = p.getStatus().getHostIP();
                    pod = p;
                }

        service = client.services().inNamespace("default").create(service);

        int port = service.getSpec().getPorts().get(0).getNodePort();
        System.err.print("SERVER: "+loc+":"+port);
        try {
            Thread.sleep(120*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return loc + ":"+ port;

    }

    public void tareDown(){
        client.services().inNamespace("default").withField("metadata.name", "db2").delete();
        client.extensions().deployments().inNamespace("default").withField("metadata.name", "db2-dep").delete();
        // client.pods().withName(pod.getMetadata().getName()).delete();
    }
}
