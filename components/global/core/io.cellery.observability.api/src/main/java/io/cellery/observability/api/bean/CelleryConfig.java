package io.cellery.observability.api.bean;

import org.wso2.carbon.config.ConfigurationException;
import org.wso2.carbon.config.annotation.Configuration;
import org.wso2.carbon.config.annotation.Element;
import org.wso2.carbon.config.provider.ConfigProvider;

/**
 * This bean class is used to read cellery config.
 */
@Configuration(namespace = "celleryObservabilityPortal", description = "Cellery Configuration Parameters")
public class CelleryConfig {

    private static ConfigProvider configProvider;
    private static CelleryConfig celleryConfig;

    @Element(description = "dashboardURL")
    private String dashboardURL = "";

    @Element(description = "idp")
    private String idp = "";

    @Element(description = "username")
    private String username = "";

    @Element(description = "password")
    private String password = "";

    public CelleryConfig() {

    }

    public CelleryConfig(String dashboardURL, String idp, String username, String password) {
        this.dashboardURL = dashboardURL;
        this.idp = idp;
        this.username = username;
        this.password = password;
    }

    public String getDashboardURL() {
        return dashboardURL;
    }

    public String getIdp() {
        return idp;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
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
