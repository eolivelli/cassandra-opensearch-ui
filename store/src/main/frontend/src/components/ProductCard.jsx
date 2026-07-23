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
import { money } from '../api.js';
import { useStore } from '../store.jsx';
import ProductImage from './ProductImage.jsx';

export default function ProductCard({ product }) {
  const { user, addToCart } = useStore();
  const navigate = useNavigate();
  const [status, setStatus] = useState('idle'); // idle | adding | added

  const onAdd = async () => {
    if (!user) {
      navigate('/login', { state: { from: `/product/${product.id}` } });
      return;
    }
    setStatus('adding');
    try {
      await addToCart(product.id, 1);
      setStatus('added');
      setTimeout(() => setStatus('idle'), 1400);
    } catch {
      setStatus('idle');
    }
  };

  const isFish = Boolean(product.species);

  return (
    <article className="card">
      <Link to={`/product/${product.id}`} className="card-media">
        <ProductImage src={product.image} alt={product.name} className="card-img" />
        {isFish && <span className="card-flag">Live fish</span>}
      </Link>
      <div className="card-body">
        <div className="card-cat">{product.category}</div>
        <h3 className="card-title">
          <Link to={`/product/${product.id}`}>{product.name}</Link>
        </h3>
        {isFish && <div className="card-species">{product.species}</div>}
        <div className="card-tags">
          {(product.tags || []).slice(0, 3).map((t) => (
            <span key={t} className="tag">
              {t}
            </span>
          ))}
        </div>
        <div className="card-foot">
          <span className="price">{money(product.price)}</span>
          <button
            className={`btn btn-add ${status === 'added' ? 'is-added' : ''}`}
            onClick={onAdd}
            disabled={status === 'adding'}
          >
            {status === 'added' ? '✓ Added' : status === 'adding' ? '…' : 'Add to cart'}
          </button>
        </div>
      </div>
    </article>
  );
}
