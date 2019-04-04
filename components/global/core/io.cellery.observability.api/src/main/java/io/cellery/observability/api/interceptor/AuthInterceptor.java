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

package io.cellery.observability.api.interceptor;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.cellery.observability.api.Constants;
import io.cellery.observability.api.bean.CelleryConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.wso2.carbon.config.ConfigurationException;
import org.wso2.msf4j.Request;
import org.wso2.msf4j.Response;
import org.wso2.msf4j.interceptor.RequestInterceptor;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;

/**
 * Used for securing backend APIs.
 */

public class AuthInterceptor implements RequestInterceptor {

    private static final String ACTIVE_STATUS = "active";
    private static final Logger log = Logger.getLogger(AuthInterceptor.class);

    @Override
    public boolean interceptRequest(Request request, Response response) {

        log.info(request.getHeader("Cookie"));
        String accessToken;
        if (!request.getHttpMethod().equalsIgnoreCase(HttpMethod.OPTIONS) &&
                request.getHeader(HttpHeaders.AUTHORIZATION) != null) {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);

            accessToken = header.split(" ")[1];
            log.info(accessToken);

            if (!validateToken(accessToken)) {
                response.setStatus(401);
                return false;
            } else {
                return true;
            }

        }

        return true;
    }

    private static boolean validateToken(String token) {

        try {
            SSLContext sslcontext = SSLContexts.custom()
                    .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                    .build();

            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext,
                    SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            CloseableHttpClient httpclient = HttpClients.custom()
                    .setSSLSocketFactory(sslsf)
                    .build();
            Unirest.setHttpClient(httpclient);
            HttpResponse<String> stringResponse
                    = Unirest.post(CelleryConfig.getInstance().getDcrEnpoint() + Constants.INTROSPECT_ENDPOINT)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .basicAuth(CelleryConfig.getInstance().getUsername()
                            , CelleryConfig.getInstance().getPassword()).body("token=" + token).asString();

            JSONObject jsonResponse = new JSONObject(stringResponse.getBody());
            if (!((Boolean) jsonResponse.get(ACTIVE_STATUS))) {
                return false;
            }

        } catch (UnirestException | KeyStoreException | NoSuchAlgorithmException |
                KeyManagementException | ConfigurationException e) {
            return false;
        }

        return true;
    }

}

