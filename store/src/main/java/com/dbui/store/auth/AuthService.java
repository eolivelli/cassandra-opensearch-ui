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

import com.dbui.store.config.StoreProperties;
import com.dbui.store.customer.CustomerService;
import jakarta.servlet.http.HttpSession;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Mock authentication for the demo. A single hard-coded account is validated against
 * {@link StoreProperties} and, on success, the username is stored in the HTTP session. This is
 * intentionally not a real security mechanism - there is no password hashing, lockout or CSRF
 * protection - it only demonstrates a logged-in shopping flow.
 */
@Service
public class AuthService {

    /** Session attribute under which the authenticated username is stored. */
    static final String SESSION_USER = "store.username";

    private final StoreProperties properties;
    private final CustomerService customers;

    public AuthService(StoreProperties properties, CustomerService customers) {
        this.properties = properties;
        this.customers = customers;
    }

    /**
     * Validates the credentials against the single demo account and, on success, records the user
     * in the session and ensures their customer profile exists.
     *
     * @throws NotAuthenticatedException
     *             if the credentials do not match
     */
    public String login(HttpSession session, String username, String password) {
        boolean ok = Objects.equals(username, properties.getDemoUsername())
                && Objects.equals(password, properties.getDemoPassword());
        if (!ok) {
            throw new NotAuthenticatedException("Invalid username or password");
        }
        customers.ensureExists(username, properties.getDemoFullName());
        session.setAttribute(SESSION_USER, username);
        return username;
    }

    /** Clears the session. */
    public void logout(HttpSession session) {
        session.invalidate();
    }

    /** Returns the logged-in username, or null if there is no active session. */
    public String currentUsername(HttpSession session) {
        Object value = session == null ? null : session.getAttribute(SESSION_USER);
        return value == null ? null : value.toString();
    }

    /**
     * Returns the logged-in username or throws, for use by protected controllers.
     *
     * @throws NotAuthenticatedException
     *             if there is no active session
     */
    public String requireUsername(HttpSession session) {
        String username = currentUsername(session);
        if (username == null) {
            throw new NotAuthenticatedException("You must sign in to do that");
        }
        return username;
    }
}
