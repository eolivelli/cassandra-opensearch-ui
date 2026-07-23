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
package com.dbui.store.web;

import com.dbui.store.config.StoreProperties;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes non-sensitive store configuration to the front-end: the store name and the demo username
 * to pre-fill on the login screen. The demo password is intentionally not returned.
 */
@RestController
@RequestMapping("/api")
public class StoreConfigController {

    private final StoreProperties properties;

    public StoreConfigController(StoreProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of("storeName", properties.getName(), "demoUsername",
                properties.getDemoUsername());
    }
}
