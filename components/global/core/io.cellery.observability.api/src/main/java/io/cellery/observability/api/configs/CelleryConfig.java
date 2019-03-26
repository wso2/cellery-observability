package io.cellery.observability.api.configs;

import org.wso2.carbon.config.ConfigurationException;
import org.wso2.carbon.config.annotation.Configuration;
import org.wso2.carbon.config.annotation.Element;
import org.wso2.carbon.config.provider.ConfigProvider;

/**
 * This bean class used to read cellery config.
 */
@Configuration(namespace = "cellery", description = "Cellery Configuration Parameters")
public class CelleryConfig {

    private static ConfigProvider configProvider;
    private static CelleryConfig celleryConfig;

    @Element(description = "dashboardURL")
    private String dashboardURL = "";

    @Element(description = "tokenEndpoint")
    private String tokenEndpoint = "";

    @Element(description = "dcrEnpoint")
    private String dcrEnpoint = "";

    @Element(description = "username")
    private String username = "";

    @Element(description = "password")
    private String password = "";

    public String getIntrospectEndpoint() {
        return introspectEndpoint;
    }

    public void setIntrospectEndpoint(String introspectEndpoint) {
        this.introspectEndpoint = introspectEndpoint;
    }

    @Element(description = "introspect-endpoint")
    private String introspectEndpoint = "";

    public CelleryConfig() {

    }

    public String getDashboardURL() {
        return dashboardURL;
    }

    public void setDashboardURL(String dashboardURL) {
        this.dashboardURL = dashboardURL;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getDcrEnpoint() {
        return dcrEnpoint;
    }

    public void setDcrEnpoint(String dcrEnpoint) {
        this.dcrEnpoint = dcrEnpoint;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private static ConfigProvider getConfigProvider() {
        return configProvider;
    }

    public static void setConfigProvider(ConfigProvider configProvider) {
        CelleryConfig.configProvider = configProvider;
    }

    public static synchronized CelleryConfig getInstance() throws ConfigurationException {
        if (celleryConfig == null) {
            celleryConfig = CelleryConfig.getConfigProvider().getConfigurationObject(CelleryConfig.class);
        }
        return celleryConfig;
    }

}
