/*
 * Copyright 2026 Enrico Olivelli
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dbui.store.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection settings for the OpenSearch cluster used by the site search. Defaults target the local
 * OpenSearch started by the repository's docker-compose.yml (shared with the DB UI module).
 */
@ConfigurationProperties(prefix = "store.opensearch")
public class OpenSearchProperties {

    /** Base URL of the OpenSearch HTTP endpoint. */
    private String url = "http://127.0.0.1:9200";

    /** Optional basic-auth username (left empty when the security plugin is disabled). */
    private String username = "";

    /** Optional basic-auth password. */
    private String password = "";

    /** Index that holds the searchable product catalog. */
    private String index = "h2o_products";

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

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }
}
