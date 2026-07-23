# H2O Store — product ingest CLI

A small standalone command-line tool that loads products into the **H2O Store** catalog. It
reads a **JSON Lines** file (one product per line) and writes every product into **both**
back-ends:

- **Cassandra** — the system of record (the `products` and `products_by_category` tables).
- **OpenSearch** — the `h2o_products` search index used by the store's search feature.

The Cassandra schema and the OpenSearch index (with its mapping) are created if missing, so
the tool can be run standalone — even before the store application has ever started.

> Prerequisites and the shared `docker compose` databases are documented in the
> [repository README](../README.md).

## Build

```bash
mvn -pl store-cli -am package     # or `mvn install` at the root
```

This produces a self-contained runnable jar at `store-cli/target/store-cli.jar`.

## Usage

```bash
java -jar store-cli/target/store-cli.jar --file <products.jsonl> [options]
```

| Option                   | Default                   | Description                                        |
|--------------------------|---------------------------|----------------------------------------------------|
| `-f, --file <path>`      | *(required)*              | input JSON Lines file                              |
| `--cassandra-host <h>`   | `127.0.0.1`               | Cassandra contact host                             |
| `--cassandra-port <p>`   | `9042`                    | Cassandra native port                              |
| `--datacenter <dc>`      | `datacenter1`             | local datacenter                                   |
| `--keyspace <ks>`        | `h2o_store`               | Cassandra keyspace                                 |
| `--opensearch-url <url>` | `http://127.0.0.1:9200`   | OpenSearch base URL                                |
| `--index <name>`         | `h2o_products`            | OpenSearch index                                   |
| `--recreate`             | off                       | truncate the tables and drop/recreate the index before loading |
| `-h, --help`             | —                         | show help                                          |

Use `--recreate` when you want a clean load — in particular when re-ingesting products whose
`name` or `category` changed, since `products_by_category` is keyed by `(category, name, id)`.

### Example

```bash
# Load the bundled sample catalog (21 products) from scratch
java -jar store-cli/target/store-cli.jar --file store-cli/samples/products.jsonl --recreate
```

## Input format

One JSON object per line. Blank lines and lines starting with `#` are ignored.

```json
{"name":"Neon Tetra","description":"A dazzling nano schooler…","category":"Live Fish","tags":["schooling","freshwater","beginner"],"price":4.99,"image":"/products/neon-tetra.jpg","species":"Paracheirodon innesi","stock":120}
```

| Field         | Required | Notes                                                            |
|---------------|----------|------------------------------------------------------------------|
| `name`        | yes      | product name                                                     |
| `category`    | yes      | catalog category (e.g. `Live Fish`, `Aquariums`)                 |
| `description` | no       | marketing description                                            |
| `tags`        | no       | array of strings, used for filtering and search                  |
| `price`       | no       | price in US dollars (defaults to `0`)                            |
| `image`       | no       | path to a bundled product image (served by the store as a static asset) |
| `species`     | no       | species name for live fish; omit for other products             |
| `stock`       | no       | units available (defaults to `25`)                              |
| `id`          | no       | product UUID; a random one is generated when absent              |

A ready-to-use sample is provided at
[`samples/products.jsonl`](samples/products.jsonl) — the same catalog the demo ships with.
The product images referenced by the sample are bundled in the store front-end under
`store/src/main/frontend/public/products/`.
