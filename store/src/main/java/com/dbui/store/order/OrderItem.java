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
import java.util.UUID;

/**
 * A line item captured on a placed order. Stored in Cassandra as a frozen user-defined type
 * ({@code order_item}) inside the order row, so the order is a self-contained snapshot that does
 * not change if the catalog later changes.
 *
 * @param productId
 *            the purchased product
 * @param name
 *            product name at purchase time
 * @param price
 *            unit price paid, in US dollars
 * @param image
 *            bundled image path
 * @param qty
 *            quantity purchased
 */
public record OrderItem(UUID productId, String name, BigDecimal price, String image, int qty) {
}
