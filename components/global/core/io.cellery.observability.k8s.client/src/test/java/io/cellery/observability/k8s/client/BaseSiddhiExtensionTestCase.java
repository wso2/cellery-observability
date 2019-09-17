/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.cellery.observability.k8s.client;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceColumnDefinitionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionNamesBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.apache.log4j.Logger;
import org.powermock.reflect.Whitebox;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * Base Test Case for K8s Clients.
 */
public class BaseSiddhiExtensionTestCase extends BaseTestCase {

    private static final Logger logger = Logger.getLogger(BaseSiddhiExtensionTestCase.class.getName());

    protected static final int WAIT_TIME = 50;
    protected static final int TIMEOUT = 5000;

    protected KubernetesClient k8sClient;
    protected KubernetesServer k8sServer;

    BaseSiddhiExtensionTestCase() throws Exception {
        super();
    }

    @BeforeMethod
    public void initBaseSiddhiExtensionTestCase() {
        k8sServer = new KubernetesServer(true, false);
        k8sServer.before();
        if (logger.isDebugEnabled()) {
            logger.debug("Started K8s Mock Server");
        }

        k8sClient = k8sServer.getClient();
        if (logger.isDebugEnabled()) {
            logger.debug("Initialized the K8s Client for the K8s Mock Server");
        }
        Whitebox.setInternalState(K8sClientHolder.class, "k8sClient", k8sClient);

        k8sServer.expect()
                .withPath("/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions/" + Constants.CELL_CRD_NAME)
                .andReturn(200, new CustomResourceDefinitionBuilder()
                        .withNewMetadata()
                        .withNamespace(Constants.NAMESPACE)
                        .withName(Constants.CELL_CRD_NAME)
                        .endMetadata()
                        .withNewSpec()
                        .withGroup(Constants.CELLERY_CRD_GROUP)
                        .withVersion(Constants.CELL_CRD_VERSION)
                        .withScope("Namespaces")
                        .withNames(new CustomResourceDefinitionNamesBuilder()
                                .withKind(Constants.CELL_KIND)
                                .withPlural("cells")
                                .withSingular("cell")
                                .build())
                        .addToAdditionalPrinterColumns(new CustomResourceColumnDefinitionBuilder()
                                .withName("Status")
                                .withType("string")
                                .withJSONPath(".status.status")
                                .build())
                        .addToAdditionalPrinterColumns(new CustomResourceColumnDefinitionBuilder()
                                .withName("Gateway")
                                .withType("string")
                                .withDescription("Host name of the gateway")
                                .withJSONPath(".status.gatewayHostname")
                                .build())
                        .addToAdditionalPrinterColumns(new CustomResourceColumnDefinitionBuilder()
                                .withName("Services")
                                .withType("integer")
                                .withDescription("Number of services in this cell")
                                .withJSONPath(".status.serviceCount")
                                .build())
                        .addToAdditionalPrinterColumns(new CustomResourceColumnDefinitionBuilder()
                                .withName("Age")
                                .withType("date")
                                .withJSONPath(".metadata.creationTimestamp")
                                .build())
                        .endSpec()
                        .build()
                )
                .always();
        k8sServer.expect()
                .withPath("/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions/" +
                        Constants.COMPOSITE_CRD_NAME)
                .andReturn(200, new CustomResourceDefinitionBuilder()
                        .withNewMetadata()
                        .withNamespace(Constants.NAMESPACE)
                        .withName(Constants.COMPOSITE_CRD_NAME)
                        .endMetadata()
                        .withNewSpec()
                        .withGroup(Constants.CELLERY_CRD_GROUP)
                        .withVersion(Constants.COMPOSITE_CRD_VERSION)
                        .withScope("Namespaces")
                        .withNames(new CustomResourceDefinitionNamesBuilder()
                                .withKind(Constants.COMPOSITE_KIND)
                                .withPlural("composites")
                                .withSingular("composite")
                                .build())
                        .addToAdditionalPrinterColumns(new CustomResourceColumnDefinitionBuilder()
                                .withName("Status")
                                .withType("string")
                                .withJSONPath(".status.status")
                                .build())
                        .addToAdditionalPrinterColumns(new CustomResourceColumnDefinitionBuilder()
                                .withName("Services")
                                .withType("integer")
                                .withDescription("Number of services in this cell")
                                .withJSONPath(".status.serviceCount")
                                .build())
                        .addToAdditionalPrinterColumns(new CustomResourceColumnDefinitionBuilder()
                                .withName("Age")
                                .withType("date")
                                .withJSONPath(".metadata.creationTimestamp")
                                .build())
                        .endSpec()
                        .build()
                )
                .always();
    }

    @AfterMethod
    public void cleanupBaseSiddhiExtensionTestCase() {
        k8sClient.close();
        if (logger.isDebugEnabled()) {
            logger.debug("Closed the K8s Client");
        }
        k8sServer.after();
        if (logger.isDebugEnabled()) {
            logger.debug("Closed the K8s Mock Server");
        }
    }

}
