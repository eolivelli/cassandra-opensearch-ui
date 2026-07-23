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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards client-side (React Router) routes to {@code index.html} so the single-page app can boot
 * and take over routing. Two kinds of path are deliberately excluded so they are handled elsewhere:
 * paths whose first segment is {@code api} (handled by the REST controllers, and 404'd by Spring
 * when unmapped rather than masked by the SPA), and paths containing a dot (static assets such as
 * {@code main.js} or {@code /products/x.jpg}, served by the resource handler). The {@code (?!api$)}
 * lookahead on the first segment enforces the former.
 */
@Controller
public class SpaController {

    @GetMapping(value = {"/", "/{path:(?!api$)[^\\.]*}", "/{path:(?!api$)[^\\.]*}/{sub:[^\\.]*}",
            "/{path:(?!api$)[^\\.]*}/{sub:[^\\.]*}/{sub2:[^\\.]*}"})
    public String forward() {
        return "forward:/index.html";
    }
}
