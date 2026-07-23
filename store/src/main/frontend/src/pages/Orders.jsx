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
import { Link } from 'react-router-dom';
import { api, money } from '../api.js';
import Spinner from '../components/Spinner.jsx';

function formatDate(iso) {
  try {
    return new Date(iso).toLocaleString('en-US', {
      dateStyle: 'medium',
      timeStyle: 'short',
    });
  } catch {
    return iso;
  }
}

export default function Orders() {
  const [orders, setOrders] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api
      .orders()
      .then(setOrders)
      .catch((e) => setError(e.message));
  }, []);

  return (
    <div className="page">
      <h1 className="page-title">Your orders</h1>
      {error && <div className="notice notice-error">{error}</div>}
      {!orders && !error && <Spinner label="Loading orders…" />}
      {orders && orders.length === 0 && (
        <div className="empty">
          <div className="empty-emoji" aria-hidden="true">
            📦
          </div>
          <p>No orders yet.</p>
          <Link className="btn btn-primary" to="/">
            Start shopping
          </Link>
        </div>
      )}
      {orders && orders.length > 0 && (
        <div className="orders">
          {orders.map((o) => (
            <Link to={`/order/${o.orderId}`} className="order-row" key={o.orderId}>
              <div>
                <div className="order-id">Order #{String(o.orderId).slice(0, 8)}</div>
                <div className="order-date">{formatDate(o.createdAt)}</div>
              </div>
              <div className="order-items-count">
                {o.items.reduce((n, it) => n + it.qty, 0)} item(s)
              </div>
              <div className={`order-status status-${(o.status || '').toLowerCase()}`}>
                {o.status}
              </div>
              <div className="order-total">{money(o.total)}</div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
