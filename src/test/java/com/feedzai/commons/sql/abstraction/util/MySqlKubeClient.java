package com.feedzai.commons.sql.abstraction.util;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class MySqlKubeClient {
    private Config config;
    private KubernetesClient client;
    private Deployment deployment;
    private Service service;
    private Pod pod;

    public MySqlKubeClient() {
        config = new ConfigBuilder().build();
        client = new DefaultKubernetesClient(config);
        ServiceAccount fabric8 = new ServiceAccountBuilder().withNewMetadata()
                .withName("fabric8").endMetadata().build();
        client.serviceAccounts().inNamespace("default")
                .createOrReplace(fabric8);
        deployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName("mysql-dep")
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("app", "mysql")
                .endMetadata().withNewSpec()
                .addNewContainer()
                .withName("mysql")
                .withImage("mysql:5.7.22")
                .addNewPort()
                .withContainerPort(3306)
                .endPort()
                .addNewEnv()
                .withName("MYSQL_ROOT_PASSWORD")
                .withValue("my-secret-pw")
                .endEnv()
                .addToCommand("entrypoint.sh", "--max-allowed-packet=16000000", "--innodb-log-file-size=160000000")
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
        System.err.println("COMMAND: " + deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getCommand());
        service = new ServiceBuilder()
                .withNewMetadata()
                .withName("mysql")
                .endMetadata().withNewSpec().withType("NodePort").addNewPort()
                .withPort(3306).withNewTargetPort(3306).endPort()
                .addToSelector("app", "mysql").endSpec().build();

    }

    public String createMySqlDeploymentAndService() {
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
        client.services().inNamespace("default").withField("metadata.name", "mysql").delete();
        client.extensions().deployments().inNamespace("default").withField("metadata.name", "mysql-dep").delete();
       // client.pods().withName(pod.getMetadata().getName()).delete();
    }

}
