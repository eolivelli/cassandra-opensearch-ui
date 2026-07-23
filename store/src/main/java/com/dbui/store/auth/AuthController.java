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
package com.dbui.store.auth;

import com.dbui.store.customer.Customer;
import com.dbui.store.customer.CustomerService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login, logout and "who am I" endpoints for the mock authentication. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final CustomerService customers;

    public AuthController(AuthService auth, CustomerService customers) {
        this.auth = auth;
        this.customers = customers;
    }

    /** Credentials for the mock login. */
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    @PostMapping("/login")
    public Customer login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        String username = auth.login(session, request.username(), request.password());
        return customers.find(username).orElseThrow();
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        auth.logout(session);
        return Map.of("ok", true);
    }

    /** Returns the current customer, or {@code {"authenticated": false}} when signed out. */
    @GetMapping("/me")
    public Object me(HttpSession session) {
        String username = auth.currentUsername(session);
        if (username == null) {
            return Map.of("authenticated", false);
        }
        return customers.find(username).map(Object.class::cast)
                .orElse(Map.of("authenticated", false));
    }
}
