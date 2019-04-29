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

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilities test case.
 */
public class UtilsTestCase {

    @Test
    public void testGetComponentNameWithComponentLabel() {
        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.COMPONENT_NAME_LABEL, "pet-fe--test-c");

        ObjectMeta podMetadata = Mockito.mock(ObjectMeta.class);
        Mockito.when(podMetadata.getLabels()).thenReturn(labels);
        Pod pod = Mockito.mock(Pod.class);
        Mockito.when(pod.getMetadata()).thenReturn(podMetadata);

        Assert.assertEquals(Utils.getComponentName(pod), "test-c");
    }

    @Test
    public void testGetComponentNameWithGatewayLabel() {
        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.GATEWAY_NAME_LABEL, "pet-fe--test-d");

        ObjectMeta podMetadata = Mockito.mock(ObjectMeta.class);
        Mockito.when(podMetadata.getLabels()).thenReturn(labels);
        Pod pod = Mockito.mock(Pod.class);
        Mockito.when(pod.getMetadata()).thenReturn(podMetadata);

        Assert.assertEquals(Utils.getComponentName(pod), "test-d");
    }
}
