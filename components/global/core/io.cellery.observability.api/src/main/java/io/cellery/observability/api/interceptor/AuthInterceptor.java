package io.cellery.observability.api.interceptor;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.cellery.observability.api.Constants;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
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

    @Override
    public boolean interceptRequest(Request request, Response response) {
        String token;
        if (!request.getHttpMethod().equalsIgnoreCase(HttpMethod.OPTIONS)) {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            token = header.substring(7, header.length());

            System.out.println(request.getHeader(HttpHeaders.AUTHORIZATION));

            if (!validateToken(token)) {
                response.setStatus(401);
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
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
                    = Unirest.post(Constants.INTERNAL_INTROSPECT_ENDPOINT)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .basicAuth("admin", "admin").body("token=" + token).asString();

            JSONObject jsonResponse = new JSONObject(stringResponse.getBody());
            System.out.println(stringResponse.getBody());
            if (!((Boolean) jsonResponse.get("active"))) {
                return false;
            }

        } catch (UnirestException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            return false;
        }

        return true;
    }
}

