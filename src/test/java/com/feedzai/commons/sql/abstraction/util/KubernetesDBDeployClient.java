package com.feedzai.commons.sql.abstraction.util;

public interface KubernetesDBDeployClient {

    void createService();
    void createDeployment();
    int getServicePort();
    String getServiceIP();
    String getFullJDBC();
    void tearDown();
    int getInternalPort();
    String getInternalIP();
    String getFullInternalJDBC();
    String getVendor();

    long getSleepTime();
}
