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
import { Link, useNavigate } from 'react-router-dom';
import { useStore } from '../store.jsx';
import { api } from '../api.js';
import ProductCard from '../components/ProductCard.jsx';
import Spinner from '../components/Spinner.jsx';

const CATEGORY_EMOJI = {
  'Live Fish': '🐠',
  Aquariums: '🐟',
  Furniture: '🪵',
  Equipment: '⚙️',
  'Decor & Plants': '🌿',
};

const FISH_CATEGORY = 'Live Fish';

export default function Home() {
  const { categories } = useStore();
  const [featured, setFeatured] = useState(null);
  const [fish, setFish] = useState(null);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    api
      .featured()
      .then(setFeatured)
      .catch((e) => setError(e.message));
    api
      .byCategory(FISH_CATEGORY)
      .then(setFish)
      .catch(() => setFish([]));
  }, []);

  return (
    <div className="page">
      <section className="hero">
        <div className="hero-content">
          <p className="hero-kicker">Welcome to the reef</p>
          <h1 className="hero-title">
            Build your dream <span className="grad">aquarium</span>
          </h1>
          <p className="hero-lede">
            Tanks, stands and pro-grade gear — plus healthy, hand-picked live tropical fish shipped
            to your door. Everything you need, from first fill to full reef.
          </p>
          <div className="hero-cta">
            <Link className="btn btn-primary" to="/category/Live%20Fish">
              Shop live fish 🐠
            </Link>
            <button className="btn btn-ghost" onClick={() => navigate('/search?q=')}>
              Explore the catalog
            </button>
          </div>
        </div>
      </section>

      <section className="cat-tiles">
        {categories.map((c) => (
          <Link key={c} to={`/category/${encodeURIComponent(c)}`} className="cat-tile">
            <span className="cat-emoji" aria-hidden="true">
              {CATEGORY_EMOJI[c] || '🌊'}
            </span>
            <span className="cat-label">{c}</span>
          </Link>
        ))}
      </section>

      <section className="section">
        <div className="section-head">
          <h2>Featured picks</h2>
          <Link to="/search?q=" className="section-link">
            View all →
          </Link>
        </div>
        {error && <div className="notice notice-error">{error}</div>}
        {!featured && !error && <Spinner label="Loading featured products…" />}
        {featured && (
          <div className="grid">
            {featured.map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        )}
      </section>

      {fish && fish.length > 0 && (
        <section className="section">
          <div className="section-head">
            <h2>Meet our tropical fish 🐠</h2>
            <Link to={`/category/${encodeURIComponent(FISH_CATEGORY)}`} className="section-link">
              All live fish →
            </Link>
          </div>
          <div className="grid">
            {fish.map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
