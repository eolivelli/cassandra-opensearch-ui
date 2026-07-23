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
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api } from './api.js';

const AppContext = createContext(null);

/**
 * Global store state: catalog config, the authenticated customer and the shopping cart. Everything
 * the header and pages need to share lives here.
 */
export function AppProvider({ children }) {
  const [config, setConfig] = useState({ storeName: 'H2O Store', demoUsername: 'user' });
  const [categories, setCategories] = useState([]);
  const [user, setUser] = useState(null);
  const [cart, setCart] = useState({ items: [], subtotal: 0, count: 0 });
  const [ready, setReady] = useState(false);

  const refreshCart = useCallback(async () => {
    try {
      const c = await api.cart();
      setCart(c);
      return c;
    } catch {
      setCart({ items: [], subtotal: 0, count: 0 });
      return null;
    }
  }, []);

  useEffect(() => {
    (async () => {
      const [cfg, cats, me] = await Promise.allSettled([
        api.config(),
        api.categories(),
        api.me(),
      ]);
      if (cfg.status === 'fulfilled') setConfig(cfg.value);
      if (cats.status === 'fulfilled') setCategories(cats.value);
      if (me.status === 'fulfilled' && me.value && me.value.username) {
        setUser(me.value);
        await refreshCart();
      }
      setReady(true);
    })();
  }, [refreshCart]);

  const login = useCallback(
    async (username, password) => {
      const customer = await api.login(username, password);
      setUser(customer);
      await refreshCart();
      return customer;
    },
    [refreshCart],
  );

  const logout = useCallback(async () => {
    await api.logout();
    setUser(null);
    setCart({ items: [], subtotal: 0, count: 0 });
  }, []);

  const addToCart = useCallback(async (productId, qty = 1) => {
    const c = await api.addToCart(productId, qty);
    setCart(c);
    return c;
  }, []);

  const setQty = useCallback(async (productId, qty) => {
    const c = await api.setQty(productId, qty);
    setCart(c);
    return c;
  }, []);

  const removeItem = useCallback(async (productId) => {
    const c = await api.removeItem(productId);
    setCart(c);
    return c;
  }, []);

  const value = useMemo(
    () => ({
      config,
      categories,
      user,
      cart,
      ready,
      login,
      logout,
      addToCart,
      setQty,
      removeItem,
      refreshCart,
      setCart,
    }),
    [config, categories, user, cart, ready, login, logout, addToCart, setQty, removeItem, refreshCart],
  );

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

/** Hook to read the global store state. */
export function useStore() {
  const ctx = useContext(AppContext);
  if (!ctx) {
    throw new Error('useStore must be used within an AppProvider');
  }
  return ctx;
}
