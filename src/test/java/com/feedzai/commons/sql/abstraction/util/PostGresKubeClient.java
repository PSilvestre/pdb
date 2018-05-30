package com.feedzai.commons.sql.abstraction.util;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class PostGresKubeClient implements KubernetesDBDeployClient{
    public static final String SERVICE_NAME = "postgresql";
    public static final String DEPLOYMENT_NAME = "postgresql-dep";
    public static final String NAMESPACE = "default";
    public static final String VENDOR = "postgresql";

    private Config config;
    private KubernetesClient client;
    private Deployment deployment;
    private Service service;

    public PostGresKubeClient() {
        config = new ConfigBuilder().build();
        client = new DefaultKubernetesClient(config);
        ServiceAccount fabric8 = new ServiceAccountBuilder().withNewMetadata()
                .withName("fabric8").endMetadata().build();
        client.serviceAccounts().inNamespace("default")
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
                .withName("postgresql")
                .withImage("postgres:9.6.8")
                .addNewPort()
                .withContainerPort(5432)
                .endPort()
                .addNewEnv()
                .withName("POSTGRES_PASSWORD")
                .withValue("pgpassword")
                .endEnv()
             .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
        System.err.println("COMMAND: " + deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getCommand());
        service = new ServiceBuilder()
                .withNewMetadata()
                .withName(SERVICE_NAME)
                .endMetadata().withNewSpec().withType("NodePort").addNewPort()
                .withPort(5432).withNewTargetPort(5432).endPort()
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
        return "postgresql.jdbc=jdbc:postgresql://"+getServiceIP()+":"+getServicePort()+"/postgres";
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
