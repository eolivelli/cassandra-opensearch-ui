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
import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="page">
      <div className="empty">
        <div className="empty-emoji" aria-hidden="true">
          🐡
        </div>
        <h1 className="page-title">Lost at sea</h1>
        <p>We couldn&apos;t find that page.</p>
        <Link className="btn btn-primary" to="/">
          Back to the store
        </Link>
      </div>
    </div>
  );
}
