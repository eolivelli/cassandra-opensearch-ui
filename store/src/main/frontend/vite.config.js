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
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const dirname = path.dirname(fileURLToPath(import.meta.url));

// The production build is emitted straight into the Spring Boot static resources so it is
// served by the store application and packaged inside the runnable jar.
export default defineConfig({
  plugins: [react()],
  base: '/',
  build: {
    outDir: path.resolve(dirname, '../../../target/classes/static'),
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    // During `npm run dev`, proxy API calls to the running Spring Boot backend.
    proxy: {
      '/api': 'http://localhost:8081',
    },
  },
});
