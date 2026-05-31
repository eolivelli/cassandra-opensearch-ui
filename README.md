# DB UI

A small, **read-only** web console to inspect the contents of two databases running locally:

- **Apache Cassandra 5**
- **OpenSearch 3.6.x** (works with 3.5 too)

There is **no authentication** — it is meant for local development against databases
running in Docker on your machine.

Every screen shows the exact **CQL** statement or **OpenSearch REST API** request that
produced the data, so you can copy it and reproduce it yourself.

## Features

- List Cassandra keyspaces and their tables, and browse table contents.
- **Cassandra table schema:** columns (with partition-key / clustering / static markers),
  indexes (including **SAI** / StorageAttachedIndex), the **user-defined types** the table
  uses, and the full `CREATE TABLE` statement.
- **Cassandra row detail:** click a row to open it in a drawer showing every column in full,
  with a ready-to-run `SELECT … WHERE <primary key>` to fetch just that row.
- List OpenSearch indices and browse the documents inside them.
- **OpenSearch index mapping:** view the field mappings and settings (`GET /{index}`).
- **OpenSearch document detail:** click a row to see the full `_source` JSON
  (`GET /{index}/_doc/{id}`).
- Each view displays the underlying CQL / REST request (with a copy button).
- Adjustable row/document limit.

## Tech stack

- **Backend:** Java 25, Spring Boot 3.5, Maven.
  - Cassandra access via the DataStax Java Driver 4.
  - OpenSearch access via the **raw REST API** (JDK `HttpClient` + Jackson) — this keeps
    the exact HTTP requests visible, which is what the UI displays.
- **Frontend:** a single-page app built with plain HTML/CSS/JS (no build step), served as
  static resources by Spring Boot.
- **Tests:** JUnit 5 + Testcontainers, spinning up real Cassandra 5 and OpenSearch 3.6
  containers.

## Prerequisites

- JDK **25**
- Maven 3.9+
- Docker (with the `docker compose` plugin)

## Quick start

```bash
# 1. Start the databases (Cassandra on :9042, OpenSearch on :9200)
docker compose up -d

# 2. (Optional) load sample data so there is something to see.
./scripts/seed-sample-data.sh   # a simple shop.products table + products index
./scripts/seed-demo.sh          # richer demo: UDTs + SAI indexes, and an OpenSearch
                                # index sharing the name "products" with a non-trivial mapping

# 3. Build and run the app
mvn package
java -jar target/db-ui.jar

# 4. Open the UI
open http://localhost:8080      # or just visit it in your browser
```

During development you can also run `mvn spring-boot:run` instead of building the jar.

To stop and remove the databases:

```bash
docker compose down            # add -v to also delete the data volumes
```

## Configuration

Defaults live in `src/main/resources/application.yml` and can be overridden with
environment variables or `--` flags. The connection settings are:

| Property                          | Default                  | Description                          |
|-----------------------------------|--------------------------|--------------------------------------|
| `dbui.cassandra.host`             | `127.0.0.1`              | Cassandra contact point host         |
| `dbui.cassandra.port`             | `9042`                   | Cassandra native port                |
| `dbui.cassandra.local-datacenter` | `datacenter1`            | Local datacenter name                |
| `dbui.opensearch.url`             | `http://127.0.0.1:9200`  | OpenSearch base URL                  |
| `dbui.opensearch.username`        | *(empty)*                | Basic-auth user (if security is on)  |
| `dbui.opensearch.password`        | *(empty)*                | Basic-auth password                  |

Example:

```bash
java -jar target/db-ui.jar --dbui.opensearch.url=http://localhost:9201
```

The OpenSearch image tag for `docker compose` can be changed with the `OPENSEARCH_TAG`
environment variable (defaults to `3.6.0`):

```bash
OPENSEARCH_TAG=3.5.0 docker compose up -d
```

## REST API

The UI is a thin client over these endpoints (all read-only, returning JSON that
includes the query/request used):

| Method & path                                                        | Returns                            |
|----------------------------------------------------------------------|------------------------------------|
| `GET /api/cassandra/keyspaces`                                       | all keyspaces                      |
| `GET /api/cassandra/keyspaces/{keyspace}/tables`                     | tables in a keyspace               |
| `GET /api/cassandra/keyspaces/{keyspace}/tables/{table}/rows?limit=` | rows of a table (default 100)      |
| `GET /api/cassandra/keyspaces/{keyspace}/tables/{table}/schema`      | columns, indexes (incl. SAI), UDTs |
| `GET /api/opensearch/indices`                                        | all indices (`_cat/indices`)       |
| `GET /api/opensearch/indices/{index}/documents?size=`                | documents in an index (default 50) |
| `GET /api/opensearch/indices/{index}/documents/{id}`                 | a single document (`_doc/{id}`)    |
| `GET /api/opensearch/indices/{index}/schema`                         | index mappings & settings          |

Row/document counts are capped at 1000 per request.

## Tests

```bash
mvn test
```

The tests use Testcontainers, so Docker must be running. They start their own
Cassandra 5 and OpenSearch 3.6 containers (independent of `docker compose`), seed data,
and assert the service layer returns the right rows/documents and the right CQL/REST
request. The OpenSearch image is ~1 GB, so the first run downloads it.

## Code style

Formatting is enforced with the [Spotless](https://github.com/diffplug/spotless) Maven
plugin, which also applies the Apache 2.0 license header to source files.

```bash
mvn spotless:check    # verify formatting (also runs automatically during `mvn verify`)
mvn spotless:apply    # auto-format and add/refresh license headers
```

Java is formatted with the **Eclipse JDT formatter** (config in
`spotless/eclipse-formatter.prefs`). It is used instead of google-/palantir-java-format
because, on JDK 25, those rely on `javac` internals that changed and fail to run.
`.mvn/jvm.config` adds the `--add-exports`/`--add-opens` flags some Spotless steps need.

## Project layout

```
db-ui/
├── docker-compose.yml            # Cassandra 5 + OpenSearch 3.6 for local use
├── spotless/                     # Spotless config (license header, Eclipse formatter)
├── .mvn/jvm.config               # JVM flags needed by Spotless on JDK 25
├── scripts/seed-sample-data.sh   # optional simple sample data
├── scripts/seed-demo.sh          # optional rich demo (UDTs, SAI, non-trivial OS mapping)
├── src/main/java/com/dbui/
│   ├── cassandra/                # Cassandra session, service, controller
│   ├── opensearch/               # OpenSearch REST client, service, controller
│   ├── model/                    # response records (CqlResult, Os*Result)
│   ├── config/                   # connection properties
│   └── web/                      # JSON error handling
└── src/main/resources/static/    # the single-page UI (index.html, app.js, style.css)
```
