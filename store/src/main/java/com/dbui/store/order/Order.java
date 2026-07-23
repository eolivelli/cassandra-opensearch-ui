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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A placed order. Orders live in Cassandra keyed by {@code (username, order_id)} with the newest
 * first, so listing a customer's orders is a single-partition read.
 *
 * @param orderId
 *            time-based UUID identifying the order (also encodes creation time)
 * @param username
 *            the customer who placed it
 * @param total
 *            order total in US dollars
 * @param status
 *            order status (e.g. "PAID")
 * @param createdAt
 *            when the order was placed
 * @param shipTo
 *            shipping address
 * @param cardLast4
 *            last four digits of the (mock) payment card
 * @param items
 *            the purchased line items
 */
public record Order(UUID orderId, String username, BigDecimal total, String status,
        Instant createdAt, String shipTo, String cardLast4, List<OrderItem> items) {
}
