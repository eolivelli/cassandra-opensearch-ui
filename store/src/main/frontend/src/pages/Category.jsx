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
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api } from '../api.js';
import ProductCard from '../components/ProductCard.jsx';
import Spinner from '../components/Spinner.jsx';

export default function Category() {
  const { name } = useParams();
  const category = decodeURIComponent(name);
  const [products, setProducts] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    setProducts(null);
    setError(null);
    api
      .byCategory(category)
      .then(setProducts)
      .catch((e) => setError(e.message));
  }, [category]);

  return (
    <div className="page">
      <div className="crumbs">
        <span>Categories</span> <span className="sep">/</span> <strong>{category}</strong>
      </div>
      <h1 className="page-title">{category}</h1>

      {error && <div className="notice notice-error">{error}</div>}
      {!products && !error && <Spinner label={`Loading ${category}…`} />}
      {products && products.length === 0 && (
        <div className="notice">No products in this category yet.</div>
      )}
      {products && products.length > 0 && (
        <div className="grid">
          {products.map((p) => (
            <ProductCard key={p.id} product={p} />
          ))}
        </div>
      )}
    </div>
  );
}
