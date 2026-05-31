package com.dbui.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection settings for the OpenSearch cluster. Defaults target a local
 * OpenSearch running in Docker (see docker-compose.yml).
 */
@ConfigurationProperties(prefix = "dbui.opensearch")
public class OpenSearchProperties {

    /** Base URL of the OpenSearch HTTP endpoint. */
    private String url = "http://127.0.0.1:9200";

    /** Optional basic-auth username (left empty when the security plugin is disabled). */
    private String username = "";

    /** Optional basic-auth password. */
    private String password = "";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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
}
