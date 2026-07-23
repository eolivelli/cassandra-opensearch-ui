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
import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useStore } from '../store.jsx';

export default function Header() {
  const { config, categories, user, cart, logout } = useStore();
  const [q, setQ] = useState('');
  const navigate = useNavigate();

  const onSearch = (e) => {
    e.preventDefault();
    navigate(`/search?q=${encodeURIComponent(q.trim())}`);
  };

  return (
    <header className="header">
      <div className="header-inner">
        <Link to="/" className="brand" aria-label={`${config.storeName} home`}>
          <span className="brand-drop" aria-hidden="true">
            💧
          </span>
          <span className="brand-name">
            {config.storeName}
            <span className="brand-sub">aquatics &amp; livestock</span>
          </span>
        </Link>

        <form className="search" onSubmit={onSearch} role="search">
          <span className="search-icon" aria-hidden="true">
            🔍
          </span>
          <input
            type="search"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Search fish, tanks, gear…  (powered by OpenSearch)"
            aria-label="Search products"
          />
          <button type="submit">Search</button>
        </form>

        <div className="header-actions">
          {user ? (
            <div className="account">
              <Link to="/orders" className="account-name" title="Your orders">
                👋 {user.fullName || user.username}
              </Link>
              <button className="link-btn" onClick={() => logout()}>
                Sign out
              </button>
            </div>
          ) : (
            <Link to="/login" className="link-btn">
              Sign in
            </Link>
          )}
          <Link to="/cart" className="cart-btn" aria-label="Shopping cart">
            🛒
            {cart.count > 0 && <span className="cart-badge">{cart.count}</span>}
          </Link>
        </div>
      </div>

      <nav className="catnav" aria-label="Product categories">
        <NavLink to="/" end className="catnav-link">
          Home
        </NavLink>
        {categories.map((c) => (
          <NavLink key={c} to={`/category/${encodeURIComponent(c)}`} className="catnav-link">
            {c}
          </NavLink>
        ))}
      </nav>
    </header>
  );
}
