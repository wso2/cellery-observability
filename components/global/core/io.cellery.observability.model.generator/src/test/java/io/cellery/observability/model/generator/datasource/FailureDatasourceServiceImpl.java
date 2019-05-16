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
package io.cellery.observability.model.generator.datasource;

import org.wso2.carbon.datasource.core.api.DataSourceService;
import org.wso2.carbon.datasource.core.beans.DataSourceDefinition;
import org.wso2.carbon.datasource.core.exception.DataSourceException;

/**
 * This is the mock datasource service class to use within the unit test cases.
 **/
public class FailureDatasourceServiceImpl implements DataSourceService {
    private boolean throwException;
    private FailureDatasource datasource;

    public FailureDatasourceServiceImpl(boolean throwException, FailureDatasource datasource) {
        this.throwException = throwException;
        this.datasource = datasource;
    }

    @Override
    public Object getDataSource(String s) throws DataSourceException {
        if (throwException) {
            throw new DataSourceException("Datasource not exists");
        } else {
            return this.datasource;
        }
    }

    @Override
    public Object createDataSource(DataSourceDefinition dataSourceDefinition) {
        return null;
    }
}
