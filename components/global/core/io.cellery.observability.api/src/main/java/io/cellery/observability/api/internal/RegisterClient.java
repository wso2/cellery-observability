package io.cellery.observability.api.internal;

import io.cellery.observability.api.AggregatedRequestsAPI;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * This class is used to create a DCR request for Service Provider registeration.
 */
public class RegisterClient {

    private static final Logger log = Logger.getLogger(AggregatedRequestsAPI.class);

    public static JSONObject getClientCredentials() throws IOException {
        BufferedReader bufReader = null;
        JSONObject obj2 = null;
        try {

            HttpClient client = getAllSSLClient();

            HttpPost request =
                    new HttpPost("https://gateway.cellery-system:9443/client-registration/v0.14/register");
            JSONObject obj = new JSONObject();
            obj.put("callbackUrl", "http://cellery-dashboard");
            obj.put("clientName", "Cellery-Observability-Portal");
            obj.put("owner", "admin");
            obj.put("grantType", "authorization_code");
            obj.put("saasApp", " true");
            request.setHeader("Authorization", "Basic YWRtaW46YWRtaW4=");
            request.setHeader("content-type", "application/json");
            request.setEntity(new StringEntity(obj.toString()));

            HttpResponse response = client.execute(request);

            bufReader = new BufferedReader(new InputStreamReader(
                    response.getEntity().getContent(), StandardCharsets.UTF_8));

            StringBuilder builder = new StringBuilder();

            String line;

            while ((line = bufReader.readLine()) != null) {
                builder.append(line);
                builder.append(System.lineSeparator());
            }

            obj2 = new JSONObject(builder.toString());
            return obj2;

        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            log.error("Message", e);
        } finally {
            if (bufReader != null) {
                try {
                    bufReader.close();
                } catch (IOException e) {
                    log.error("Message", e);
                }
            }
        }

        return obj2;
    }

    public static HttpClient getAllSSLClient()
            throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                java.security.cert.X509Certificate[] obj = new java.security.cert.X509Certificate[1];
                return obj;
            }

            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }
        }};
        SSLContext context = SSLContext.getInstance("SSL");
        context.init(null, trustAllCerts, null);

        HttpClientBuilder builder = HttpClientBuilder.create();
        SSLConnectionSocketFactory sslConnectionFactory =
                new SSLConnectionSocketFactory(context, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        builder.setSSLSocketFactory(sslConnectionFactory);

        PlainConnectionSocketFactory plainConnectionSocketFactory = new PlainConnectionSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", sslConnectionFactory)
                .register("http", plainConnectionSocketFactory)
                .build();

        HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);

        builder.setConnectionManager(ccm);

        return builder.build();

    }

}

