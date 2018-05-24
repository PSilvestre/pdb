package com.feedzai.commons.sql.abstraction.util;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class OracleKubeClient {
    private Config config;
    private KubernetesClient client;
    private Deployment deployment;
    private Service service;
    private Pod pod;

    public OracleKubeClient() {
        config = new ConfigBuilder().build();
        client = new DefaultKubernetesClient(config);
        ServiceAccount fabric8 = new ServiceAccountBuilder().withNewMetadata()
                .withName("fabric8").endMetadata().build();
        client.serviceAccounts().inNamespace("default")
                .createOrReplace(fabric8);
        deployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName("oracle-dep")
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("app", "oracle")
                .endMetadata().withNewSpec()
                .addNewContainer()
                .withName("oracle")
                .withImage("docker.io/filemon/oracle_11g:latest")
                .addNewPort()
                .withContainerPort(1521)
                .endPort()
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
        System.err.println("COMMAND: " + deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getCommand());
        service = new ServiceBuilder()
                .withNewMetadata()
                .withName("oracle")
                .endMetadata().withNewSpec().withType("NodePort").addNewPort()
                .withPort(1521).withNewTargetPort(1521).endPort()
                .addToSelector("app", "oracle").endSpec().build();

    }

    public String createOracleDeploymentAndService() {
        deployment = client.extensions().deployments().inNamespace("default")
                .create(deployment);
        String loc = null;
        while (loc == null)
            for (Pod p : client.pods().list().getItems())
                if (p.getMetadata().getName().startsWith("mysql")) {
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
        client.services().inNamespace("default").withField("metadata.name", "oracle").delete();
        client.extensions().deployments().inNamespace("default").withField("metadata.name", "oracle-dep").delete();
        // client.pods().withName(pod.getMetadata().getName()).delete();
    }
}
