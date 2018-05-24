/*
 * Copyright 2014 Feedzai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.feedzai.commons.sql.abstraction.engine.impl.postgresql;


import com.feedzai.commons.sql.abstraction.engine.DatabaseEngine;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngineException;
import com.feedzai.commons.sql.abstraction.engine.impl.PostgreSqlEngine;
import com.feedzai.commons.sql.abstraction.engine.impl.abs.AbstractEngineSchemaTest;
import com.feedzai.commons.sql.abstraction.engine.testconfig.DatabaseConfiguration;
import com.feedzai.commons.sql.abstraction.engine.testconfig.DatabaseTestUtil;
import com.feedzai.commons.sql.abstraction.util.MySqlKubeClient;
import com.feedzai.commons.sql.abstraction.util.PostGresKubeClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.Properties;

import static com.feedzai.commons.sql.abstraction.engine.configuration.PdbProperties.*;
import static com.feedzai.commons.sql.abstraction.engine.configuration.PdbProperties.SCHEMA;
import static com.feedzai.commons.sql.abstraction.engine.configuration.PdbProperties.SCHEMA_POLICY;
import static com.feedzai.commons.sql.abstraction.engine.impl.abs.AbstractEngineSchemaTest.Ieee754Support.SUPPORTED;

/**
 * @author Rafael Marmelo (rafael.marmelo@feedzai.com)
 * @since 2.0.0
 */
@RunWith(Parameterized.class)
public class PostgreSqlEngineSchemaTest extends AbstractEngineSchemaTest {
    private static PostGresKubeClient client;
    private static String kubeJDBC;

    @Parameterized.Parameters
    public static Collection<DatabaseConfiguration> data() throws Exception {
        return DatabaseTestUtil.loadConfigurations("postgresql");
    }
    @BeforeClass
    public static void initKubernetesClient(){
        client = new PostGresKubeClient();
        String loc = client.createPostgresqlDeploymentAndService();
        kubeJDBC = "jdbc:postgresql://"+loc+"/postgres";
    }

    @Override
    @Before
    public void init() throws Exception {

        properties = new Properties() {
            {
                setProperty(JDBC, kubeJDBC);
                setProperty(USERNAME, config.username);
                setProperty(PASSWORD, config.password);
                setProperty(ENGINE, config.engine);
                setProperty(SCHEMA_POLICY, "drop-create");
                setProperty(SCHEMA, getDefaultSchema());
            }
        };
    }

    @AfterClass
    public static void tareDown(){
        client.tareDown();
    }

    @Override
    protected String getSchema() {
        return "myschema";
    }

    @Override
    protected Ieee754Support getIeee754Support() {
        return SUPPORTED;
    }

    @Override
    protected void defineUDFGetOne(DatabaseEngine engine) throws DatabaseEngineException {
        engine.executeUpdate(
            "CREATE OR REPLACE FUNCTION GetOne()\n" +
                "    RETURNS INTEGER\n" +
                "    AS 'SELECT 1;'\n" +
                "    LANGUAGE SQL;"
        );
    }

    @Override
    protected void defineUDFTimesTwo(DatabaseEngine engine) throws DatabaseEngineException {
        engine.executeUpdate("DROP SCHEMA IF EXISTS myschema CASCADE");
        engine.executeUpdate("CREATE SCHEMA myschema");

        engine.executeUpdate(
            "    CREATE OR REPLACE FUNCTION myschema.TimesTwo(INTEGER)\n" +
                "    RETURNS INTEGER\n" +
                "    AS 'SELECT $1 * 2;'\n" +
                "    LANGUAGE SQL;\n"
        );
    }
}
