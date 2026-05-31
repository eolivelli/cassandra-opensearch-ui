#!/usr/bin/env bash
# Loads a little sample data into the local Cassandra and OpenSearch so the UI
# has something to show. Safe to run multiple times.
#
# Usage:  ./scripts/seed-sample-data.sh
set -euo pipefail

OS_URL="${OS_URL:-http://localhost:9200}"

echo "==> Seeding Cassandra (keyspace 'shop')"
docker exec dbui-cassandra cqlsh -e "\
CREATE KEYSPACE IF NOT EXISTS shop WITH replication = {'class':'SimpleStrategy','replication_factor':1}; \
CREATE TABLE IF NOT EXISTS shop.products (id int PRIMARY KEY, name text, price double, in_stock boolean); \
INSERT INTO shop.products (id,name,price,in_stock) VALUES (1,'Keyboard',49.9,true); \
INSERT INTO shop.products (id,name,price,in_stock) VALUES (2,'Mouse',19.5,false); \
INSERT INTO shop.products (id,name,price,in_stock) VALUES (3,'Monitor',199.0,true);"

echo "==> Seeding OpenSearch (index 'products')"
curl -sf -X POST "${OS_URL}/products/_doc?refresh=true" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Keyboard","price":49.9,"in_stock":true}' >/dev/null
curl -sf -X POST "${OS_URL}/products/_doc?refresh=true" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Mouse","price":19.5,"in_stock":false}' >/dev/null
curl -sf -X POST "${OS_URL}/products/_doc?refresh=true" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Monitor","price":199.0,"in_stock":true}' >/dev/null

echo "==> Done. Start the app and open http://localhost:8080"
