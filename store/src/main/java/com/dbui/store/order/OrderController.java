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
package com.dbui.store.order;

import com.dbui.store.auth.AuthService;
import com.dbui.store.web.NotFoundException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Checkout and order-history endpoints. All require an authenticated session. */
@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orders;
    private final AuthService auth;

    public OrderController(OrderService orders, AuthService auth) {
        this.orders = orders;
        this.auth = auth;
    }

    @PostMapping("/checkout")
    public Order checkout(@Valid @RequestBody CheckoutRequest request, HttpSession session) {
        return orders.checkout(auth.requireUsername(session), request);
    }

    @GetMapping("/orders")
    public List<Order> list(HttpSession session) {
        return orders.list(auth.requireUsername(session));
    }

    @GetMapping("/orders/{orderId}")
    public Order get(@PathVariable UUID orderId, HttpSession session) {
        String username = auth.requireUsername(session);
        return orders.get(username, orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
    }
}
