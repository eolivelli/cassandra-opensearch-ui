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
import { Link, useNavigate } from 'react-router-dom';
import { money } from '../api.js';
import { useStore } from '../store.jsx';
import ProductImage from '../components/ProductImage.jsx';

export default function Cart() {
  const { cart, setQty, removeItem } = useStore();
  const navigate = useNavigate();

  if (!cart.items.length) {
    return (
      <div className="page">
        <h1 className="page-title">Your cart</h1>
        <div className="empty">
          <div className="empty-emoji" aria-hidden="true">
            🪸
          </div>
          <p>Your cart is as empty as a freshly cycled tank.</p>
          <Link className="btn btn-primary" to="/">
            Start shopping
          </Link>
        </div>
      </div>
    );
  }

  const shipping = 0;

  return (
    <div className="page">
      <h1 className="page-title">Your cart</h1>
      <div className="cart-layout">
        <div className="cart-lines">
          {cart.items.map((item) => (
            <div className="cart-line" key={item.productId}>
              <Link to={`/product/${item.productId}`} className="cart-thumb">
                <ProductImage src={item.image} alt={item.name} className="cart-thumb-img" />
              </Link>
              <div className="cart-line-main">
                <Link to={`/product/${item.productId}`} className="cart-line-name">
                  {item.name}
                </Link>
                <div className="cart-line-price">{money(item.price)} each</div>
              </div>
              <div className="qty">
                <button
                  onClick={() => setQty(item.productId, item.qty - 1)}
                  aria-label="Decrease quantity"
                >
                  −
                </button>
                <span>{item.qty}</span>
                <button
                  onClick={() => setQty(item.productId, item.qty + 1)}
                  aria-label="Increase quantity"
                >
                  +
                </button>
              </div>
              <div className="cart-line-total">{money(item.price * item.qty)}</div>
              <button
                className="remove"
                onClick={() => removeItem(item.productId)}
                aria-label={`Remove ${item.name}`}
              >
                ✕
              </button>
            </div>
          ))}
        </div>

        <aside className="summary">
          <h2>Order summary</h2>
          <div className="summary-row">
            <span>Subtotal</span>
            <span>{money(cart.subtotal)}</span>
          </div>
          <div className="summary-row">
            <span>Shipping</span>
            <span>{shipping === 0 ? 'Free' : money(shipping)}</span>
          </div>
          <div className="summary-row summary-total">
            <span>Total</span>
            <span>{money(cart.subtotal + shipping)}</span>
          </div>
          <button className="btn btn-primary btn-block" onClick={() => navigate('/checkout')}>
            Proceed to checkout →
          </button>
          <Link to="/" className="continue">
            ← Continue shopping
          </Link>
        </aside>
      </div>
    </div>
  );
}
