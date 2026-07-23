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
import { useSearchParams } from 'react-router-dom';
import { api } from '../api.js';
import { useStore } from '../store.jsx';
import ProductCard from '../components/ProductCard.jsx';
import Spinner from '../components/Spinner.jsx';

export default function Search() {
  const { categories } = useStore();
  const [params, setParams] = useSearchParams();
  const q = params.get('q') || '';
  const category = params.get('category') || '';
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    setError(null);
    api
      .search(q, category)
      .then((r) => setResult(r))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [q, category]);

  const setCategory = (c) => {
    const next = new URLSearchParams(params);
    if (c) next.set('category', c);
    else next.delete('category');
    setParams(next);
  };

  return (
    <div className="page">
      <div className="search-head">
        <h1 className="page-title">
          {q ? (
            <>
              Results for “<span className="grad">{q}</span>”
            </>
          ) : (
            'Browse the catalog'
          )}
        </h1>
        <span className="os-badge" title="This page is served by OpenSearch">
          ⚡ OpenSearch
        </span>
      </div>

      <div className="filters">
        <button className={`chip-btn ${!category ? 'active' : ''}`} onClick={() => setCategory('')}>
          All
        </button>
        {categories.map((c) => (
          <button
            key={c}
            className={`chip-btn ${category === c ? 'active' : ''}`}
            onClick={() => setCategory(c)}
          >
            {c}
          </button>
        ))}
      </div>

      {result && !loading && (
        <p className="result-meta">
          {result.total} {result.total === 1 ? 'match' : 'matches'} · query took{' '}
          {result.tookMillis} ms
        </p>
      )}

      {error && <div className="notice notice-error">{error}</div>}
      {loading && <Spinner label="Searching…" />}
      {result && !loading && result.hits.length === 0 && (
        <div className="notice">
          No products matched. Try a broader term like “fish”, “tank” or “filter”.
        </div>
      )}
      {result && !loading && result.hits.length > 0 && (
        <div className="grid">
          {result.hits.map((h) => (
            <ProductCard key={h.id} product={h} />
          ))}
        </div>
      )}
    </div>
  );
}
