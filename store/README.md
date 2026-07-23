# H2O Store

A demo e-commerce application for **H2O Store** — a shop selling aquariums, aquascaping
equipment, furniture and **live tropical fish**. It exists to show **Cassandra and
OpenSearch working together** in one application.

- **Cassandra is the system of record.** Products, customers, shopping carts and orders all
  live in Cassandra, with tables modelled per query pattern.
- **OpenSearch powers the search feature only.** The site-wide product search hits the
  `h2o_products` index for relevance ranking, multi-field matching and fuzziness — the one
  thing Cassandra is not designed for. Every other page reads from Cassandra.

> Prerequisites and the shared `docker compose` databases are documented in the
> [repository README](../README.md).

## Features

- Home page with a featured selection and a live-fish showcase.
- Browse by category and view product details (name, tags, image, price in USD, species).
- **Search** — free-text search across name, species, tags and description, with a category
  filter (powered by OpenSearch; the page shows the match count and query time).
- **Mock authentication** — a single demo account (`user` / `password`), session-based.
  *This is a demo stand-in, not a real security mechanism: no password hashing, lockout or
  CSRF protection.*
- **Shopping cart** — add/update/remove items; the cart is stored in Cassandra per user.
- **Mock checkout / payment** — a payment form that always succeeds. No real charge is made
  and card details are not stored (only the last 4 digits are kept on the order).
- **Order history** — past orders, newest first.

## Tech stack

- **Backend:** Java 25, Spring Boot 3.5, Maven; DataStax Java Driver 4 for Cassandra;
  OpenSearch via the **raw REST API** (JDK `HttpClient` + Jackson), mirroring the DB UI module.
- **Frontend:** **React + Vite** (React Router). The Maven build compiles it (downloading a
  local Node/npm toolchain via `frontend-maven-plugin`) straight into the Spring Boot static
  resources, so the runnable jar serves the SPA. Pass `-Dfrontend.skip=true` to skip the
  front-end build when iterating on the backend.
- **Tests:** JUnit 5 + Testcontainers (real Cassandra 5 + OpenSearch 3.x).

## Run it

From the repository root:

```bash
docker compose up -d                       # shared databases
mvn -pl store -am package                  # or `mvn install` to build everything

# Load the sample catalog (writes Cassandra + OpenSearch)
java -jar store-cli/target/store-cli.jar --file store-cli/samples/products.jsonl --recreate

java -jar store/target/h2o-store.jar       # http://localhost:8081
```

Sign in with **`user`** / **`password`**.

For front-end development: `cd store/src/main/frontend && npm install && npm run dev`
(Vite dev server on :5173, proxying `/api` to the backend on :8081).

## Data model

### Cassandra (keyspace `h2o_store`)

Created automatically on startup (`SchemaInitializer`, all `IF NOT EXISTS`):

| Table / type            | Key                         | Purpose                                             |
|-------------------------|-----------------------------|-----------------------------------------------------|
| `products`              | `id`                        | point look-ups by product id                        |
| `products_by_category`  | `(category, name, id)`      | browse a category as a single-partition read        |
| `customers`             | `username`                  | customer profile                                    |
| `cart_items`            | `(username, product_id)`    | a customer's cart (one partition per customer)       |
| `order_item` (UDT)      | —                           | a captured line item                                |
| `orders`                | `(username, order_id)` desc | order history, newest first; items as `list<frozen<order_item>>` |

The two `products*` tables are kept consistent by the ingest CLI (query-first modelling: one
table per access pattern).

### OpenSearch (index `h2o_products`)

An explicit mapping (also created on startup) with `name`/`species` as `text` + `keyword`,
`category`/`tags` as `keyword`, and `price`/`stock` as numbers. Documents are indexed by the
CLI, keyed by the product id.

## REST API

Public (no auth):

| Method & path                         | Returns                                  |
|---------------------------------------|------------------------------------------|
| `GET /api/config`                     | store name + demo username               |
| `GET /api/categories`                 | distinct category names                  |
| `GET /api/products/featured`          | a featured product per category          |
| `GET /api/products?category=`         | products in a category (Cassandra)       |
| `GET /api/products/{id}`              | a single product (Cassandra)             |
| `GET /api/search?q=&category=`        | **product search (OpenSearch)**          |
| `POST /api/auth/login`                | sign in (sets the session)               |
| `POST /api/auth/logout`               | sign out                                 |
| `GET /api/auth/me`                    | current customer, or `{authenticated:false}` |

Authenticated (require a session):

| Method & path                         | Returns                                  |
|---------------------------------------|------------------------------------------|
| `GET /api/cart`                       | the current cart                         |
| `POST /api/cart/items`                | add an item `{productId, qty}`           |
| `PUT /api/cart/items/{id}`            | set a line quantity `{qty}`              |
| `DELETE /api/cart/items/{id}`         | remove a line                            |
| `DELETE /api/cart`                    | empty the cart                           |
| `POST /api/checkout`                  | place a (mock) order                     |
| `GET /api/orders`                     | order history                            |
| `GET /api/orders/{orderId}`           | a single order                           |

## Configuration

Defaults live in `store/src/main/resources/application.yml`:

| Property                          | Default                  | Description                          |
|-----------------------------------|--------------------------|--------------------------------------|
| `server.port`                     | `8081`                   | HTTP port                            |
| `store.demo-username`             | `user`                   | mock login username                  |
| `store.demo-password`             | `password`               | mock login password                  |
| `store.cassandra.*`               | local defaults           | Cassandra host/port/datacenter/keyspace |
| `store.opensearch.url`            | `http://127.0.0.1:9200`  | OpenSearch base URL                  |
| `store.opensearch.index`          | `h2o_products`           | search index name                    |

## Project layout

```
store/
├── src/main/java/com/dbui/store/
│   ├── catalog/     # Product + ProductService (Cassandra reads) + CatalogController
│   ├── search/      # SearchService (OpenSearch) + SearchController   ← the only OS user
│   ├── cart/        # Cart/CartItem + CartService + CartController (Cassandra)
│   ├── order/       # Order/OrderItem + OrderService + checkout (Cassandra UDT)
│   ├── customer/    # Customer profile
│   ├── auth/        # mock session authentication
│   ├── cassandra/   # session provider + SchemaInitializer (Cassandra DDL + OS index)
│   ├── opensearch/  # raw-REST OpenSearch client
│   ├── config/      # @ConfigurationProperties
│   └── web/         # SPA forwarding, JSON error handling, /api/config
├── src/main/frontend/   # React + Vite single-page app (built into the jar)
└── src/test/java/com/dbui/store/   # Testcontainers full-stack integration test
```
