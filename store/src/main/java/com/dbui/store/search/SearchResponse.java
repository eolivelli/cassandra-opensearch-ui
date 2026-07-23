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
package com.dbui.store.search;

import java.math.BigDecimal;
import java.util.List;

/**
 * The result of a site search. Alongside the hits it echoes the query and timing so the demo can
 * make it obvious that this page - and only this page - is powered by OpenSearch.
 *
 * @param query
 *            the free-text query that was run
 * @param total
 *            total number of matching documents
 * @param tookMillis
 *            time OpenSearch reported for the query
 * @param hits
 *            the matching products, most relevant first
 */
public record SearchResponse(String query, long total, long tookMillis, List<Hit> hits) {

    /**
     * A single search hit projected from the OpenSearch {@code _source}.
     *
     * @param id
     *            product id (the document {@code _id})
     * @param name
     *            product name
     * @param description
     *            marketing description
     * @param category
     *            catalog category
     * @param tags
     *            product tags
     * @param price
     *            price in US dollars
     * @param image
     *            bundled image path
     * @param species
     *            species name for live fish, otherwise null
     * @param score
     *            OpenSearch relevance score
     */
    public record Hit(String id, String name, String description, String category,
            List<String> tags, BigDecimal price, String image, String species, double score) {
    }
}
