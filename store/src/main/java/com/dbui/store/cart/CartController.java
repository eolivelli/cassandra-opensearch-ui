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
package com.dbui.store.cart;

import com.dbui.store.auth.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Shopping-cart endpoints. All require an authenticated session, enforced by
 * {@link AuthService#requireUsername}.
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService carts;
    private final AuthService auth;

    public CartController(CartService carts, AuthService auth) {
        this.carts = carts;
        this.auth = auth;
    }

    /** Request body to add an item to the cart. */
    public record AddItemRequest(@NotNull UUID productId, int qty) {
    }

    /** Request body to change a line's quantity. */
    public record QuantityRequest(int qty) {
    }

    @GetMapping
    public Cart get(HttpSession session) {
        return carts.get(auth.requireUsername(session));
    }

    @PostMapping("/items")
    public Cart add(@Valid @RequestBody AddItemRequest request, HttpSession session) {
        int qty = request.qty() <= 0 ? 1 : request.qty();
        return carts.addItem(auth.requireUsername(session), request.productId(), qty);
    }

    @PutMapping("/items/{productId}")
    public Cart setQuantity(@PathVariable UUID productId, @RequestBody QuantityRequest request,
            HttpSession session) {
        return carts.setQuantity(auth.requireUsername(session), productId, request.qty());
    }

    @DeleteMapping("/items/{productId}")
    public Cart remove(@PathVariable UUID productId, HttpSession session) {
        return carts.removeItem(auth.requireUsername(session), productId);
    }

    @DeleteMapping
    public Cart clear(HttpSession session) {
        String username = auth.requireUsername(session);
        carts.clear(username);
        return carts.get(username);
    }
}
