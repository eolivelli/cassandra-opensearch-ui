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

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A line in the shopping cart. Product name, price and image are denormalized into the cart row (a
 * common Cassandra pattern) so rendering the cart needs no extra look-ups.
 *
 * @param productId
 *            the product being purchased
 * @param name
 *            product name at the time it was added
 * @param price
 *            unit price in US dollars
 * @param image
 *            bundled image path
 * @param qty
 *            quantity ordered
 */
public record CartItem(UUID productId, String name, BigDecimal price, String image, int qty) {

    /** The line total: unit price times quantity. */
    public BigDecimal lineTotal() {
        return price.multiply(BigDecimal.valueOf(qty));
    }
}
