# Cassandra + OpenSearch demos

A small monorepo with two applications that run against the **same** local
**Apache Cassandra 5** and **OpenSearch 3.x**, showing two very different ways to use them:

| Module      | What it is                                                                                  | Port  |
|-------------|---------------------------------------------------------------------------------------------|-------|
| **`dbui`**  | **DB UI** — a read-only web console to browse Cassandra & OpenSearch, plus ad-hoc query pages | 8080  |
| **`store`** | **H2O Store** — a demo e-commerce app (aquariums, gear & live tropical fish) that uses Cassandra as its system of record and OpenSearch for the site search | 8081  |
| **`store-cli`** | a command-line tool that ingests products (JSON Lines) into Cassandra **and** OpenSearch | —     |

The interesting bit is the **H2O Store**: it demonstrates Cassandra and OpenSearch working
**together** in one application. Cassandra is the system of record — products, customers,
shopping carts and orders — while OpenSearch powers the one feature Cassandra is not good
at: free-text product search with relevance ranking and fuzziness.

See each module's own README for details:

- [`dbui/README.md`](dbui/README.md) — the DB UI console
- [`store/README.md`](store/README.md) — the H2O Store (architecture, data model, REST API)
- [`store-cli/README.md`](store-cli/README.md) — the ingest CLI

## Prerequisites

- JDK **25**
- Maven **3.9+**
- Docker (with the `docker compose` plugin)

> The **`store`** module builds a React/Vite front-end. The Maven build downloads a local
> Node/npm toolchain automatically (via `frontend-maven-plugin`), so no global Node install
> is required — but the first build needs network access to fetch Node and the npm packages.

## Quick start

```bash
# 1. Start the shared databases (Cassandra on :9042, OpenSearch on :9200)
docker compose up -d

# 2. Build everything (parent + all three modules)
mvn install
#   Backend only, skipping the React build:  mvn install -Dfrontend.skip=true
```

### Run the DB UI console

```bash
java -jar dbui/target/db-ui.jar          # http://localhost:8080
```

### Run the H2O Store

```bash
# Load the sample catalog into Cassandra + OpenSearch
java -jar store-cli/target/store-cli.jar --file store-cli/samples/products.jsonl --recreate

# Start the store
java -jar store/target/h2o-store.jar     # http://localhost:8081
```

Then open http://localhost:8081 and sign in with the demo account **`user`** / **`password`**.

During development you can also run either app with `mvn -pl <module> spring-boot:run`, and
the store's front-end with `cd store/src/main/frontend && npm run dev` (it proxies `/api` to
`:8081`).

To stop and remove the databases:

```bash
docker compose down            # add -v to also delete the data volumes
```

## Repository layout

```
.
├── pom.xml                     # Maven parent: shared versions, dependencyManagement, Spotless
├── docker-compose.yml          # Cassandra 5 + OpenSearch 3.x for local use (shared by both apps)
├── spotless/                   # shared Spotless config (license header + Eclipse formatter)
├── .mvn/jvm.config             # JVM flags Spotless needs on JDK 25
├── scripts/                    # helper scripts for the DB UI (seed data, start/stop)
├── dbui/                       # DB UI console module (package com.dbui)
├── store/                      # H2O Store module (backend com.dbui.store + React/Vite front-end)
└── store-cli/                  # product ingest CLI (package com.dbui.store.cli)
```

Library versions, the Spring Boot parent, and the shared build-plugin configuration
(Spotless, the front-end toolchain) live in the **parent `pom.xml`**; each module keeps only
what is specific to it.

## Code style

Formatting is enforced with the [Spotless](https://github.com/diffplug/spotless) Maven
plugin (configured once in the parent and applied to every module), which also stamps the
Apache 2.0 license header on source files.

```bash
mvn spotless:check    # verify (also runs automatically during the build)
mvn spotless:apply    # auto-format and add/refresh license headers
```

Java is formatted with the **Eclipse JDT formatter** (`spotless/eclipse-formatter.prefs`),
used instead of google-/palantir-java-format because, on JDK 25, those rely on `javac`
internals that changed and fail to run. `.mvn/jvm.config` adds the `--add-exports` /
`--add-opens` flags some Spotless steps need.

## Tests

```bash
mvn test
```

Both apps use **Testcontainers** to launch real Cassandra 5 and OpenSearch 3.x containers
(independent of `docker compose`), so Docker must be running. The OpenSearch image is
~1 GB, so the first run downloads it.

## License

Apache License 2.0 — see [`LICENSE`](LICENSE).
