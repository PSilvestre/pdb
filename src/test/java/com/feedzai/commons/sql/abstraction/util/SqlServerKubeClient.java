package com.feedzai.commons.sql.abstraction.util;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class SqlServerKubeClient {
    private Config config;
    private KubernetesClient client;
    private Deployment deployment;
    private Service service;
    private Pod pod;

    public SqlServerKubeClient() {
        config = new ConfigBuilder().build();
        client = new DefaultKubernetesClient(config);
        ServiceAccount fabric8 = new ServiceAccountBuilder().withNewMetadata()
                .withName("fabric8").endMetadata().build();
        client.serviceAccounts().inNamespace("default")
                .createOrReplace(fabric8);
        deployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName("sqlserver-dep")
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("app", "sqlserver")
                .endMetadata().withNewSpec()
                .addNewContainer()
                .withName("sqlserver")
                .withImage("microsoft/mssql-server-linux:2017-CU6")
                .addNewPort()
                .withContainerPort(1433)
                .endPort()
                .addNewEnv()
                .withName("ACCEPT_EULA")
                .withValue("Y")
                .endEnv()
                .addNewEnv()
                .withName("SA_PASSWORD")
                .withValue("AAaa11!!")
                .endEnv()
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();

       service = new ServiceBuilder()
                .withNewMetadata()
                .withName("sqlserver")
                .endMetadata().withNewSpec().withType("NodePort").addNewPort()
                .withPort(1433).withNewTargetPort(1433).endPort()
                .addToSelector("app", "sqlserver").endSpec().build();

    }

    public String createSqlServerDeploymentAndService() {
        deployment = client.extensions().deployments().inNamespace("default")
                .create(deployment);
        String loc = null;
        while (loc == null)
            for (Pod p : client.pods().list().getItems())
                if (p.getMetadata().getName().startsWith("sqlserver")) {
                    loc = p.getStatus().getHostIP();
                    pod = p;
                }

        service = client.services().inNamespace("default").create(service);

        int port = service.getSpec().getPorts().get(0).getNodePort();
        System.err.print("SERVER: "+loc+":"+port);
        try {
            Thread.sleep(42*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return loc + ":"+ port;

    }

    public void tareDown(){
        client.services().inNamespace("default").withField("metadata.name", "sqlserver").delete();
        client.extensions().deployments().inNamespace("default").withField("metadata.name", "sqlserver-dep").delete();
       // client.pods().withName(pod.getMetadata().getName()).delete();
    }

}
