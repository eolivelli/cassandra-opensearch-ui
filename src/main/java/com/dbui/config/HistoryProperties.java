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
package com.dbui.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the local query-history file. The history stores executed commands (not their
 * results) so they can be re-run later.
 */
@ConfigurationProperties(prefix = "dbui.history")
public class HistoryProperties {

    /** Path of the JSON file the history is persisted to. */
    private String file = System.getProperty("user.home") + "/.db-ui/query-history.json";

    /** Maximum number of entries kept per source before the oldest are dropped. */
    private int maxEntries = 200;

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }
}
