package com.feedzai.commons.sql.abstraction.util;

public interface KubernetesDBDeployClient {

    void createService();
    void createDeployment();
    int getServicePort();
    String getServiceIP();
    String getFullJDBC();
    void tearDown();

    String getVendor();

    long getSleepTime();
}
