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
import { Link, useNavigate } from 'react-router-dom';
import { api, money } from '../api.js';
import { useStore } from '../store.jsx';

export default function Checkout() {
  const { cart, user, setCart } = useStore();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    shipTo: user?.address || '',
    cardName: user?.fullName || '',
    cardNumber: '4242 4242 4242 4242',
    expiry: '12/29',
    cvv: '123',
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const update = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  const onSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const order = await api.checkout(form);
      setCart({ items: [], subtotal: 0, count: 0 });
      navigate(`/order/${order.orderId}`, { replace: true });
    } catch (err) {
      setError(err.message);
      setSubmitting(false);
    }
  };

  if (!cart.items.length) {
    return (
      <div className="page">
        <h1 className="page-title">Checkout</h1>
        <div className="empty">
          <p>Your cart is empty — nothing to check out.</p>
          <Link className="btn btn-primary" to="/">
            Back to store
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      <h1 className="page-title">Checkout</h1>
      <div className="checkout-layout">
        <form className="checkout-form" onSubmit={onSubmit}>
          <section className="panel">
            <h2>Shipping</h2>
            <label className="field">
              <span>Shipping address</span>
              <textarea
                required
                rows={3}
                value={form.shipTo}
                onChange={update('shipTo')}
                placeholder="123 Coral Way, Reef City, CA 90210"
              />
            </label>
          </section>

          <section className="panel">
            <h2>
              Payment <span className="mock-tag">mock — no real charge</span>
            </h2>
            <label className="field">
              <span>Name on card</span>
              <input required value={form.cardName} onChange={update('cardName')} />
            </label>
            <label className="field">
              <span>Card number</span>
              <input
                required
                value={form.cardNumber}
                onChange={update('cardNumber')}
                inputMode="numeric"
                autoComplete="off"
              />
            </label>
            <div className="field-row">
              <label className="field">
                <span>Expiry</span>
                <input required value={form.expiry} onChange={update('expiry')} placeholder="MM/YY" />
              </label>
              <label className="field">
                <span>CVV</span>
                <input
                  required
                  value={form.cvv}
                  onChange={update('cvv')}
                  inputMode="numeric"
                  autoComplete="off"
                />
              </label>
            </div>
            <p className="hint">
              This is a demo. Any details are accepted and no payment is processed — only the last 4
              digits are stored on the order.
            </p>
          </section>

          {error && <div className="notice notice-error">{error}</div>}

          <button className="btn btn-primary btn-lg btn-block" type="submit" disabled={submitting}>
            {submitting ? 'Processing payment…' : `Pay ${money(cart.subtotal)}`}
          </button>
        </form>

        <aside className="summary">
          <h2>Your order</h2>
          <ul className="summary-items">
            {cart.items.map((item) => (
              <li key={item.productId}>
                <span className="summary-qty">{item.qty}×</span>
                <span className="summary-name">{item.name}</span>
                <span>{money(item.price * item.qty)}</span>
              </li>
            ))}
          </ul>
          <div className="summary-row summary-total">
            <span>Total</span>
            <span>{money(cart.subtotal)}</span>
          </div>
        </aside>
      </div>
    </div>
  );
}
