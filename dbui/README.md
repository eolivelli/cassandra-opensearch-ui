# DB UI

A small web console to inspect the contents of two databases running locally, plus
ad-hoc **query pages** for running your own statements against them:

- **Apache Cassandra 5**
- **OpenSearch 3.6.x** (works with 3.5 too)

The browsing screens are read-only; the query pages can run **any** statement you type
(including DDL/DML on Cassandra and write requests on OpenSearch), so use them with the
same care you would a shell.

There is **no authentication** — it is meant for local development against databases
running in Docker on your machine.

Every screen shows the exact **CQL** statement or **OpenSearch REST API** request that
produced the data, so you can copy it and reproduce it yourself.

> Prerequisites, the shared `docker compose` databases, code style and the overall build
> are documented in the [repository README](../README.md). This file covers the DB UI app.

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
- **CQL Query page:** run any CQL statement (SELECT, DDL or DML) and see the whole
  result at once (no paging). SELECTs render as a table; DDL/DML report success.
- **OS Query page:** run any OpenSearch REST call — pick the HTTP method, enter the path
  (relative to the base URL) and an optional JSON body. The raw status and response JSON
  are shown, including for error responses (e.g. a 404).
- **Query history:** every command you run on a query page is saved to a local file
  (the command only — never the results) and listed in the sidebar, newest first. Click
  one to load it back into the editor and re-run it; use the 🗑 button to clear it.

## Tech stack

- **Backend:** Java 25, Spring Boot 3.5, Maven.
  - Cassandra access via the DataStax Java Driver 4.
  - OpenSearch access via the **raw REST API** (JDK `HttpClient` + Jackson) — this keeps
    the exact HTTP requests visible, which is what the UI displays.
- **Frontend:** a single-page app built with plain HTML/CSS/JS (no build step), served as
  static resources by Spring Boot.
- **Tests:** JUnit 5 + Testcontainers, spinning up real Cassandra 5 and OpenSearch 3.6
  containers.

## Run it

From the repository root:

```bash
# 1. Start the databases (Cassandra on :9042, OpenSearch on :9200)
docker compose up -d

# 2. (Optional) load sample data so there is something to see.
./scripts/seed-sample-data.sh   # a simple shop.products table + products index
./scripts/seed-demo.sh          # richer demo: UDTs + SAI indexes, and an OpenSearch
                                # index sharing the name "products" with a non-trivial mapping

# 3. Build and run
mvn -pl dbui -am package        # or `mvn install` at the root to build everything
java -jar dbui/target/db-ui.jar

# 4. Open the UI
open http://localhost:8080
```

During development you can also run `mvn -pl dbui spring-boot:run` instead of building the jar.

## Configuration

Defaults live in `dbui/src/main/resources/application.yml` and can be overridden with
environment variables or `--` flags:

| Property                          | Default                       | Description                          |
|-----------------------------------|-------------------------------|--------------------------------------|
| `dbui.cassandra.host`             | `127.0.0.1`                   | Cassandra contact point host         |
| `dbui.cassandra.port`             | `9042`                        | Cassandra native port                |
| `dbui.cassandra.local-datacenter` | `datacenter1`                 | Local datacenter name                |
| `dbui.opensearch.url`             | `http://127.0.0.1:9200`       | OpenSearch base URL                  |
| `dbui.opensearch.username`        | *(empty)*                     | Basic-auth user (if security is on)  |
| `dbui.opensearch.password`        | *(empty)*                     | Basic-auth password                  |
| `dbui.history.file`               | `~/.db-ui/query-history.json` | Where the query history is stored    |
| `dbui.history.max-entries`        | `200`                         | Max history entries kept per source  |

```bash
java -jar dbui/target/db-ui.jar --dbui.opensearch.url=http://localhost:9201
```

The OpenSearch image tag for `docker compose` can be changed with the `OPENSEARCH_TAG`
environment variable (defaults to `3.6.0`).

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
| `POST /api/cassandra/query` `{ "cql": … }`                           | runs any CQL, returns all rows     |
| `POST /api/opensearch/query` `{ method, path, body }`                | runs any OpenSearch REST call      |
| `GET /api/history?type=cassandra\|opensearch`                        | saved query history for a source   |
| `DELETE /api/history?type=cassandra\|opensearch`                     | clears the history for a source    |

The browsing endpoints are read-only and cap row/document counts at 1000 per request.
The `POST …/query` endpoints run whatever you send and, for Cassandra SELECTs, return
the full result with no paging.

## Tests

```bash
mvn -pl dbui test
```

A shared base class (`AbstractIntegrationTest`) starts a real **Cassandra 5**
(`org.testcontainers:cassandra`) and a real **OpenSearch 3.6**
(`org.opensearch:opensearch-testcontainers`) once and reuses them across all tests.

- `CassandraServiceTest` / `OpenSearchServiceTest` — service-layer tests against the containers.
- `DbUiIntegrationTest` — a full-stack `@SpringBootTest` that boots the whole application,
  wires it to the Testcontainers databases via `@DynamicPropertySource`, and drives it
  through its HTTP REST API.

## Project layout

```
dbui/
└── src/main/java/com/dbui/
    ├── cassandra/                # Cassandra session, service, controller (browse + query)
    ├── opensearch/               # OpenSearch REST client, service, controller (browse + query)
    ├── history/                  # query-history service + controller (local file persistence)
    ├── model/                    # response records (CqlResult, Os*Result, HistoryEntry, …)
    ├── config/                   # connection & history properties
    └── web/                      # JSON error handling
    src/main/resources/static/    # the single-page UI (index.html, app.js, style.css)
    src/test/java/com/dbui/       # Testcontainers integration tests
```
