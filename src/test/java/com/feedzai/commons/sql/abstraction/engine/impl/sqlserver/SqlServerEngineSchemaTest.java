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
package com.feedzai.commons.sql.abstraction.engine.impl.sqlserver;

import com.feedzai.commons.sql.abstraction.engine.DatabaseEngine;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngineException;
import com.feedzai.commons.sql.abstraction.engine.impl.abs.AbstractEngineSchemaTest;
import com.feedzai.commons.sql.abstraction.engine.testconfig.DatabaseConfiguration;
import com.feedzai.commons.sql.abstraction.engine.testconfig.DatabaseTestUtil;
import com.feedzai.commons.sql.abstraction.util.MySqlKubeClient;
import com.feedzai.commons.sql.abstraction.util.SqlServerKubeClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Properties;

import static com.feedzai.commons.sql.abstraction.engine.configuration.PdbProperties.*;
import static com.feedzai.commons.sql.abstraction.engine.configuration.PdbProperties.SCHEMA;
import static com.feedzai.commons.sql.abstraction.engine.configuration.PdbProperties.SCHEMA_POLICY;

/**
 * @author Rui Vilao (rui.vilao@feedzai.com)
 * @since 2.0.0
 */
@RunWith(Parameterized.class)
public class SqlServerEngineSchemaTest extends AbstractEngineSchemaTest {

    private static SqlServerKubeClient client;
    private static String kubeJDBC;

    @Parameterized.Parameters
    public static Collection<DatabaseConfiguration> data() throws Exception {
        return DatabaseTestUtil.loadConfigurations("sqlserver");
    }

   /* @BeforeClass
    public static void initKubernetesClient(){
        client = new SqlServerKubeClient();
        String loc = client.createSqlServerDeploymentAndService();
        kubeJDBC = "jdbc:sqlserver://"+loc;
    }
*/
    @Override
    @Before
    public void init() throws Exception {

        properties = new Properties() {
            {
                setProperty(JDBC, config.jdbc);
                setProperty(USERNAME, config.username);
                setProperty(PASSWORD, config.password);
                setProperty(ENGINE, config.engine);
                setProperty(SCHEMA_POLICY, "drop-create");
                setProperty(SCHEMA, getDefaultSchema());
            }
        };
    }

    @Override
    protected String getDefaultSchema() {
        return "dbo";
    }

    @Override
    protected String getSchema() {
        return "myschema";
    }

    @Override
    protected void defineUDFGetOne(DatabaseEngine engine) throws DatabaseEngineException {
        engine.executeUpdate("IF OBJECT_ID (N'dbo.GetOne', N'FN') IS NOT NULL\n" +
            "    DROP FUNCTION dbo.GetOne");
        engine.executeUpdate(
            "CREATE FUNCTION dbo.GetOne()\n" +
                "RETURNS INTEGER\n" +
                "AS\n" +
                "BEGIN\n" +
                "  RETURN(1)\n" +
                "END"
        );
    }

    @Override
    protected void defineUDFTimesTwo(DatabaseEngine engine) throws DatabaseEngineException {
        engine.executeUpdate("IF EXISTS (SELECT * FROM sys.schemas WHERE name = N'myschema')\n" +
            "BEGIN\n" +
            "   IF OBJECT_ID (N'myschema.TimesTwo', N'FN') IS NOT NULL\n" +
            "   BEGIN\n" +
            "       DROP FUNCTION myschema.TimesTwo;\n" +
            "   END\n" +
            "   DROP SCHEMA myschema;\n" +
            "END");
        engine.executeUpdate("CREATE SCHEMA myschema");

        engine.executeUpdate(
            "CREATE FUNCTION myschema.TimesTwo(@number INTEGER)\n" +
                "RETURNS INTEGER\n" +
                "AS\n" +
                "BEGIN\n" +
                "  RETURN(@number * 2)\n" +
                "END\n"
        );
    }
}
