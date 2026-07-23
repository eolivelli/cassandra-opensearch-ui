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

/** An error carrying the HTTP status so callers can react to 401s, 404s, etc. */
export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

async function request(path, options = {}) {
  const res = await fetch(path, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const message = (data && data.message) || res.statusText || 'Request failed';
    throw new ApiError(message, res.status);
  }
  return data;
}

const qs = (params) =>
  Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join('&');

/** Typed wrappers over the store's REST API. All calls carry the session cookie. */
export const api = {
  config: () => request('/api/config'),
  categories: () => request('/api/categories'),
  featured: () => request('/api/products/featured'),
  byCategory: (category) => request(`/api/products?${qs({ category })}`),
  product: (id) => request(`/api/products/${id}`),
  search: (q, category) => request(`/api/search?${qs({ q, category })}`),

  me: () => request('/api/auth/me'),
  login: (username, password) =>
    request('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),
  logout: () => request('/api/auth/logout', { method: 'POST' }),

  cart: () => request('/api/cart'),
  addToCart: (productId, qty) =>
    request('/api/cart/items', { method: 'POST', body: JSON.stringify({ productId, qty }) }),
  setQty: (productId, qty) =>
    request(`/api/cart/items/${productId}`, { method: 'PUT', body: JSON.stringify({ qty }) }),
  removeItem: (productId) => request(`/api/cart/items/${productId}`, { method: 'DELETE' }),
  clearCart: () => request('/api/cart', { method: 'DELETE' }),

  checkout: (payload) =>
    request('/api/checkout', { method: 'POST', body: JSON.stringify(payload) }),
  orders: () => request('/api/orders'),
  order: (id) => request(`/api/orders/${id}`),
};

/** Formats a dollar amount for display. */
export function money(value) {
  const n = typeof value === 'number' ? value : Number(value ?? 0);
  return n.toLocaleString('en-US', { style: 'currency', currency: 'USD' });
}
