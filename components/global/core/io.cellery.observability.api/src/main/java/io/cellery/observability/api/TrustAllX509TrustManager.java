package io.cellery.observability.api;

import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

/**
 * This class will simply trust everything that comes along.
 */
public class TrustAllX509TrustManager implements X509TrustManager {

    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    public void checkClientTrusted(java.security.cert.X509Certificate[] certs,
                                   String authType) {
    }

    public void checkServerTrusted(java.security.cert.X509Certificate[] certs,
                                   String authType) {
    }

}