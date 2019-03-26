package io.cellery.observability.api.registeration;

import io.cellery.observability.api.AggregatedRequestsAPI;
import io.cellery.observability.api.configs.CelleryConfig;
import io.cellery.observability.api.Constants;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.wso2.carbon.config.ConfigurationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * This class is used to create a DCR request for Service Provider registeration.
 */
public class IdpRegisteration {

    private static final Logger log = Logger.getLogger(AggregatedRequestsAPI.class);

    public static JSONObject getClientCredentials() throws IOException {
        BufferedReader bufReader = null;
        InputStreamReader inputStreamReader = null;
        JSONObject jsonObject = null;
        ArrayList<String> uris = new ArrayList<>(Arrays.asList("https://localhost:4000"));
        ArrayList<String> grants = new ArrayList<>(Arrays.asList("implicit", "authorization_code"));
        try {
            HttpClient client = getAllSSLClient();
            HttpPost request = constructRequestBody("https://localhost:9443" +
                    Constants.IDP_REGISTERATION_ENDPOINT, uris, grants);

            HttpResponse response = client.execute(request);
            inputStreamReader = new InputStreamReader(
                    response.getEntity().getContent(), StandardCharsets.UTF_8);
            bufReader = new BufferedReader(inputStreamReader);
            StringBuilder builder = new StringBuilder();

            String line;
            while ((line = bufReader.readLine()) != null) {
                builder.append(line);
                builder.append(System.lineSeparator());
            }

            jsonObject = new JSONObject(builder.toString());

            if (jsonObject.has("error")) {
                jsonObject = checkRegisteration("https://localhost:9443" +
                        Constants.IDP_REGISTERATION_ENDPOINT, client);
            } else {
                jsonObject = new JSONObject(builder.toString());
            }
            return jsonObject;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.error("Error while fetching the Client-Id for the dynamically client ", e);
            return jsonObject;
        } finally {
            if (bufReader != null) {
                try {
                    bufReader.close();
                } catch (IOException e) {
                    log.error("Error in closing the BufferedReader", e);
                }
            }
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    log.error("Error in closing the InputStreamReader", e);
                }
            }
        }
    }

    private static HttpPost constructRequestBody(String dcrEp, ArrayList<String> callbackUris,
                                                 ArrayList<String> grants) {
        HttpPost request =
                new HttpPost(dcrEp);
        JSONObject clientJson = new JSONObject();
        clientJson.put(Constants.CALL_BACK_URL, callbackUris);
        clientJson.put(Constants.CLIENT_NAME, Constants.APPLICATION_NAME);
        clientJson.put(Constants.GRANT_TYPE, grants);
        clientJson.put("ext_param_client_id", Constants.STANDARD_ID);
        StringEntity requestEntity = new StringEntity(clientJson.toString(), ContentType.APPLICATION_JSON);
        request.setHeader(Constants.AUTHORIZATION, Constants.BASIC_ADMIN_AUTH);
        request.setHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
        request.setEntity(requestEntity);

        return request;
    }

    private static String basicAuthentication() throws ConfigurationException {
        String authString = CelleryConfig.getInstance().getUsername() + ":"
                + CelleryConfig.getInstance().getPassword();
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes(Charset.forName("UTF-8")));
        String authStringEnc = new String(authEncBytes, Charset.forName("UTF-8"));
        return "Basic " + authStringEnc;
    }

    private static HttpClient getAllSSLClient()
            throws NoSuchAlgorithmException, KeyManagementException {

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

//    public static void main(String[] args) throws IOException {
//        System.out.println(getClientCredentials());
//    }

    private static JSONObject checkRegisteration(String dcrEp, HttpClient client) throws IOException {
        HttpGet getRequest = new HttpGet(dcrEp + "?client_name=" + Constants.APPLICATION_NAME);
        getRequest.setHeader(Constants.AUTHORIZATION, Constants.BASIC_ADMIN_AUTH);

        HttpResponse resp = client.execute(getRequest);
        HttpEntity entity = resp.getEntity();
        String result = EntityUtils.toString(entity, Charset.forName("utf-8"));
        return new JSONObject(result);
    }
}
