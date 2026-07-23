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
package com.dbui.history;

import com.dbui.model.HistoryEntry;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for reading and clearing the persisted query history. */
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final QueryHistoryService history;

    public HistoryController(QueryHistoryService history) {
        this.history = history;
    }

    @GetMapping
    public List<HistoryEntry> list(@RequestParam(defaultValue = "cassandra") String type) {
        return history.list(type);
    }

    @DeleteMapping
    public void clear(@RequestParam(defaultValue = "cassandra") String type) {
        history.clear(type);
    }
}
