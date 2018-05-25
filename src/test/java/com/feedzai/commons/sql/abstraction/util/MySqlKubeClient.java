package com.feedzai.commons.sql.abstraction.util;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class MySqlKubeClient implements KubernetesDBDeployClient{
    public static final String SERVICE_NAME = "mysql";
    public static final String DEPLOYMENT_NAME = "mysql-dep";
    public static final String NAMESPACE = "default";
    private static final String VENDOR = "mysql";

    private Config config;
    private KubernetesClient client;
    private Deployment deployment;
    private Service service;

    public MySqlKubeClient() {
        config = new ConfigBuilder().build();
        client = new DefaultKubernetesClient(config);

        ServiceAccount fabric8 = new ServiceAccountBuilder().withNewMetadata()
                .withName("fabric8").endMetadata().build();
        client.serviceAccounts().inNamespace(NAMESPACE)
                .createOrReplace(fabric8);

        deployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName(DEPLOYMENT_NAME)
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("app", SERVICE_NAME)
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
                .addToCommand("/entrypoint.sh","mysqld","--max-allowed-packet=16000000", "--innodb-log-file-size=160000000")
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();

        service = new ServiceBuilder()
                .withNewMetadata()
                .withName(SERVICE_NAME)
                .endMetadata().withNewSpec().withType("NodePort").addNewPort()
                .withPort(3306).withNewTargetPort(3306).endPort()
                .addToSelector("app", SERVICE_NAME).endSpec().build();

    }
    @Override
    public void createService(){
        service = client.services().inNamespace(NAMESPACE).create(service);
    }
    @Override
    public void createDeployment() {
        deployment = client.extensions().deployments().inNamespace(NAMESPACE)
                .create(deployment);
    }

    @Override
    public int getServicePort() {
        return  service.getSpec().getPorts().get(0).getNodePort();
    }

    @Override
    public String getServiceIP() {
        String ip = null;
        while (ip == null)
            for (Pod p : client.pods().list().getItems())
                if (p.getMetadata().getName().startsWith(SERVICE_NAME))
                    ip = p.getStatus().getHostIP();
        return ip;
    }

    @Override
    public String getFullJDBC() {
        return "mysql=jdbc:mysql://"+getServiceIP()+":"+getServicePort()+"/mysql?useSSL=false";
    }


    @Override
    public void tearDown(){
        client.services().inNamespace(NAMESPACE).withField("metadata.name", SERVICE_NAME).delete();
        client.extensions().deployments().inNamespace(NAMESPACE).withField("metadata.name", DEPLOYMENT_NAME).delete();
    }

    @Override
    public String getVendor() {
        return VENDOR;
    }

    @Override
    public long getSleepTime() {
        return 40000;
    }

}
