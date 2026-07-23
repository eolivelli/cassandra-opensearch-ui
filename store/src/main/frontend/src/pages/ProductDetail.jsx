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
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api, money } from '../api.js';
import { useStore } from '../store.jsx';
import ProductImage from '../components/ProductImage.jsx';
import Spinner from '../components/Spinner.jsx';

export default function ProductDetail() {
  const { id } = useParams();
  const { user, addToCart } = useStore();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [error, setError] = useState(null);
  const [qty, setQty] = useState(1);
  const [status, setStatus] = useState('idle');

  useEffect(() => {
    setProduct(null);
    setError(null);
    api
      .product(id)
      .then(setProduct)
      .catch((e) => setError(e.message));
  }, [id]);

  const onAdd = async () => {
    if (!user) {
      navigate('/login', { state: { from: `/product/${id}` } });
      return;
    }
    setStatus('adding');
    try {
      await addToCart(product.id, qty);
      setStatus('added');
      setTimeout(() => setStatus('idle'), 1600);
    } catch {
      setStatus('idle');
    }
  };

  if (error) {
    return (
      <div className="page">
        <div className="notice notice-error">{error}</div>
        <Link className="btn btn-ghost" to="/">
          ← Back to store
        </Link>
      </div>
    );
  }
  if (!product) {
    return (
      <div className="page">
        <Spinner label="Loading product…" />
      </div>
    );
  }

  const inStock = product.stock > 0;
  const isFish = Boolean(product.species);

  return (
    <div className="page">
      <div className="crumbs">
        <Link to={`/category/${encodeURIComponent(product.category)}`}>{product.category}</Link>
        <span className="sep">/</span>
        <strong>{product.name}</strong>
      </div>

      <div className="detail">
        <div className="detail-media">
          <ProductImage src={product.image} alt={product.name} className="detail-img" />
          {isFish && <span className="detail-flag">Live arrival guarantee</span>}
        </div>

        <div className="detail-info">
          <div className="card-cat">{product.category}</div>
          <h1 className="detail-title">{product.name}</h1>
          {isFish && <div className="detail-species">{product.species}</div>}
          <div className="detail-price">{money(product.price)}</div>
          <p className="detail-desc">{product.description}</p>

          <div className="card-tags">
            {(product.tags || []).map((t) => (
              <span key={t} className="tag">
                {t}
              </span>
            ))}
          </div>

          <div className={`stock ${inStock ? 'in' : 'out'}`}>
            {inStock ? `● In stock — ${product.stock} available` : '● Currently out of stock'}
          </div>

          <div className="buy">
            <div className="qty">
              <button onClick={() => setQty((q) => Math.max(1, q - 1))} aria-label="Decrease">
                −
              </button>
              <span>{qty}</span>
              <button onClick={() => setQty((q) => q + 1)} aria-label="Increase">
                +
              </button>
            </div>
            <button
              className={`btn btn-primary btn-lg ${status === 'added' ? 'is-added' : ''}`}
              onClick={onAdd}
              disabled={!inStock || status === 'adding'}
            >
              {status === 'added'
                ? '✓ Added to cart'
                : status === 'adding'
                  ? 'Adding…'
                  : 'Add to cart'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
