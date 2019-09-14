/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package io.cellery.observability.k8s.client;

import io.cellery.observability.k8s.client.crds.CellImpl;
import io.cellery.observability.k8s.client.crds.CompositeImpl;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import org.apache.log4j.Logger;

/**
 * This class will hold the instance of the k8sClient that is used in the {@link GetComponentPodsStreamProcessor}
 * stream processor extension.
 */
class K8sClientHolder {

    private static final Logger logger = Logger.getLogger(K8sClientHolder.class.getName());
    private static KubernetesClient k8sClient;

    private K8sClientHolder() {     // Prevent initialization
    }

    static synchronized KubernetesClient getK8sClient() {
        if (k8sClient == null) {
            k8sClient = new DefaultKubernetesClient();
            if (logger.isDebugEnabled()) {
                logger.debug("Created API server client");
            }

            // Register the Cellery custom resource kinds to Kubernetes deserializer to perform deserialization
            // of Cellery objects.
            KubernetesDeserializer.registerCustomKind(Constants.CELLERY_CRD_GROUP + "/"
                    + Constants.CELL_CRD_VERSION, Constants.CELL_KIND, CellImpl.class);
            KubernetesDeserializer.registerCustomKind(Constants.CELLERY_CRD_GROUP + "/"
                    + Constants.COMPOSITE_CRD_VERSION, Constants.COMPOSITE_KIND, CompositeImpl.class);
        }
        return k8sClient;
    }

}
