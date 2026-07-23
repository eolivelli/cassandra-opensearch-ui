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

// Decorative rising-bubble background. Purely cosmetic; hidden from assistive tech.
const BUBBLES = Array.from({ length: 14 }, (_, i) => {
  const size = 6 + ((i * 7) % 26);
  return {
    left: `${(i * 137) % 100}%`,
    size: `${size}px`,
    duration: `${9 + ((i * 3) % 12)}s`,
    delay: `${(i * 1.6) % 10}s`,
  };
});

export default function Bubbles() {
  return (
    <div className="bubbles" aria-hidden="true">
      {BUBBLES.map((b, i) => (
        <span
          key={i}
          className="bubble"
          style={{
            left: b.left,
            width: b.size,
            height: b.size,
            animationDuration: b.duration,
            animationDelay: b.delay,
          }}
        />
      ))}
    </div>
  );
}
