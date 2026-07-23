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
 * Store-level settings, including the mock authentication credentials. Authentication here is a
 * demo stand-in only: a single hard-coded account guarded by a server-side session. It is not a
 * real security mechanism.
 */
@ConfigurationProperties(prefix = "store")
public class StoreProperties {

    /** Human-readable store name shown in the UI. */
    private String name = "H2O Store";

    /** The single demo account's username. */
    private String demoUsername = "user";

    /** The single demo account's password (mock authentication). */
    private String demoPassword = "password";

    /** Display name for the demo account. */
    private String demoFullName = "Demo Customer";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDemoUsername() {
        return demoUsername;
    }

    public void setDemoUsername(String demoUsername) {
        this.demoUsername = demoUsername;
    }

    public String getDemoPassword() {
        return demoPassword;
    }

    public void setDemoPassword(String demoPassword) {
        this.demoPassword = demoPassword;
    }

    public String getDemoFullName() {
        return demoFullName;
    }

    public void setDemoFullName(String demoFullName) {
        this.demoFullName = demoFullName;
    }
}
