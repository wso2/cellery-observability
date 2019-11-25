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

/**
 * This contains the constants for the K8S Client Extensions.
 */
public class Constants {

    public static final String CELLERY_GROUP = "mesh.cellery.io";
    public static final String CELL_CRD_NAME = "cells." + CELLERY_GROUP;
    public static final String CELL_CRD_VERSION = "v1alpha2";
    public static final String CELL_KIND = "Cell";
    public static final String COMPOSITE_CRD_NAME = "composites." + CELLERY_GROUP;
    public static final String COMPOSITE_CRD_VERSION = "v1alpha2";
    public static final String COMPOSITE_KIND = "Composite";

    public static final String CELLERY_OBSERVABILITY_GROUP = "observability." + CELLERY_GROUP;
    public static final String CELLERY_OBSERVABILITY_INSTANCE_LABEL = CELLERY_OBSERVABILITY_GROUP + "/instance";
    public static final String CELLERY_OBSERVABILITY_INSTANCE_KIND_LABEL =
            CELLERY_OBSERVABILITY_GROUP + "/instance-kind";
    public static final String CELLERY_OBSERVABILITY_COMPONENT_NAME_LABEL = CELLERY_OBSERVABILITY_GROUP + "/component";
    public static final String CELLERY_OBSERVABILITY_GATEWAY_NAME_LABEL = CELLERY_OBSERVABILITY_GROUP + "/gateway";
    public static final String STATUS_FIELD = "status.phase";
    public static final String STATUS_FIELD_RUNNING_VALUE = "Running";
    public static final String K8S_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /**
     * Cellery ingress types related classes.
     */
    public static class IngressType {
        public static final String HTTP = "HTTP";
        public static final String WEB = "WEB";
        public static final String TCP = "TCP";
        public static final String GRPC = "GRPC";
    }

    /**
     * Siddhi extension attribute names.
     */
    public static class Attribute {
        public static final String NAMESPACE = "namespace";
        public static final String INSTANCE = "instance";
        public static final String INSTANCE_KIND = "instanceKind";
        public static final String ACTION = "action";
        public static final String COMPONENT = "component";
        public static final String CREATION_TIMESTAMP = "creationTimestamp";
        public static final String INGRESS_TYPES = "ingressTypes";
        public static final String POD_NAME = "podName";
        public static final String DELETION_TIMESTAMP = "deletionTimestamp";
        public static final String NODE_NAME = "nodeName";
        public static final String STATUS = "status";
    }

    private Constants() {   // Prevent initialization
    }
}
