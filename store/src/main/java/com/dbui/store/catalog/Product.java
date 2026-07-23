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
package com.dbui.store.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A catalog product. This is the shape stored in Cassandra (the system of record) and, for the
 * searchable fields, indexed into OpenSearch. Prices are in US dollars.
 *
 * @param id
 *            stable product identifier
 * @param name
 *            display name
 * @param description
 *            longer marketing description
 * @param category
 *            catalog category (e.g. "Live Fish", "Aquariums")
 * @param tags
 *            free-form tags used for filtering and search
 * @param price
 *            price in US dollars
 * @param image
 *            path to the bundled product image (served as a static asset)
 * @param species
 *            scientific/common species name for live fish; null for other products
 * @param stock
 *            units available
 */
public record Product(UUID id, String name, String description, String category, List<String> tags,
        BigDecimal price, String image, String species, int stock) {
}
