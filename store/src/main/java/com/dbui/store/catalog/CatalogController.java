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

import com.dbui.store.web.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public catalog endpoints backed entirely by Cassandra. No authentication is required to browse.
 */
@RestController
@RequestMapping("/api")
public class CatalogController {

    private final ProductService products;

    public CatalogController(ProductService products) {
        this.products = products;
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return products.categories();
    }

    @GetMapping("/products/featured")
    public List<Product> featured() {
        return products.featured(8);
    }

    @GetMapping("/products")
    public List<Product> byCategory(@RequestParam String category) {
        return products.byCategory(category);
    }

    @GetMapping("/products/{id}")
    public Product byId(@PathVariable UUID id) {
        return products.byId(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }
}
