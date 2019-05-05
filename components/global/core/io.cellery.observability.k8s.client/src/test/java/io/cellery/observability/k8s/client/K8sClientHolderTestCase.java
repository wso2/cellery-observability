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

import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.log4j.Logger;
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * K8s Client test case.
 */
public class K8sClientHolderTestCase {

    private static final Logger logger = Logger.getLogger(K8sClientHolderTestCase.class.getName());
    @Test
    public void testGetK8sClient() {
        Whitebox.setInternalState(K8sClientHolder.class, "k8sClient", (KubernetesClient) null);
        Assert.assertNotNull(K8sClientHolder.getK8sClient());
    }

    @Test
    public void testGetK8sClientTwice() {
        Whitebox.setInternalState(K8sClientHolder.class, "k8sClient", (KubernetesClient) null);
        KubernetesClient k8sClient = K8sClientHolder.getK8sClient();
        Assert.assertNotNull(k8sClient);
        Assert.assertSame(K8sClientHolder.getK8sClient(), k8sClient);
    }
}
