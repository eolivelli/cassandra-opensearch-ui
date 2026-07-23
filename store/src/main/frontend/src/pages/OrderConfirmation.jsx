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
import { Link, useParams } from 'react-router-dom';
import { api, money } from '../api.js';
import ProductImage from '../components/ProductImage.jsx';
import Spinner from '../components/Spinner.jsx';

export default function OrderConfirmation() {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api
      .order(id)
      .then(setOrder)
      .catch((e) => setError(e.message));
  }, [id]);

  if (error) {
    return (
      <div className="page">
        <div className="notice notice-error">{error}</div>
        <Link className="btn btn-ghost" to="/orders">
          ← Your orders
        </Link>
      </div>
    );
  }
  if (!order) {
    return (
      <div className="page">
        <Spinner label="Loading order…" />
      </div>
    );
  }

  return (
    <div className="page">
      <div className="confirm-hero">
        <div className="confirm-check" aria-hidden="true">
          ✓
        </div>
        <h1>Order confirmed!</h1>
        <p>
          Thanks for your order. A confirmation for order{' '}
          <strong>#{String(order.orderId).slice(0, 8)}</strong> has been recorded.
        </p>
      </div>

      <div className="confirm-card">
        <div className="confirm-meta">
          <div>
            <span className="label">Status</span>
            <span className={`order-status status-${(order.status || '').toLowerCase()}`}>
              {order.status}
            </span>
          </div>
          <div>
            <span className="label">Ship to</span>
            <span>{order.shipTo}</span>
          </div>
          <div>
            <span className="label">Paid with</span>
            <span>card ending •••• {order.cardLast4}</span>
          </div>
        </div>

        <ul className="confirm-items">
          {order.items.map((item) => (
            <li key={item.productId}>
              <ProductImage src={item.image} alt={item.name} className="confirm-thumb" />
              <span className="confirm-name">{item.name}</span>
              <span className="confirm-qty">×{item.qty}</span>
              <span className="confirm-line">{money(item.price * item.qty)}</span>
            </li>
          ))}
        </ul>

        <div className="summary-row summary-total confirm-total">
          <span>Total paid</span>
          <span>{money(order.total)}</span>
        </div>
      </div>

      <div className="confirm-actions">
        <Link className="btn btn-ghost" to="/orders">
          View all orders
        </Link>
        <Link className="btn btn-primary" to="/">
          Continue shopping
        </Link>
      </div>
    </div>
  );
}
