#!/usr/bin/env bash
# Seeds a richer demo so the schema / detail views have something interesting:
#
#   Cassandra: keyspace 'store' with two UDTs (address, dimensions), a table
#              'products' using them, and several SAI (StorageAttachedIndex) indexes.
#   OpenSearch: an index 'products' (same name as the Cassandra table) with a
#               non-trivial explicit mapping (multi-fields, nested objects,
#               scaled_float, half_float, geo_point, date) and matching documents.
#
# The Cassandra rows and OpenSearch documents share the same ids and fields.
#
# Usage:  ./scripts/seed-demo.sh
set -euo pipefail

OS_URL="${OS_URL:-http://localhost:9200}"

ID1=11111111-1111-1111-1111-111111111111
ID2=22222222-2222-2222-2222-222222222222
ID3=33333333-3333-3333-3333-333333333333

echo "==> Cassandra: keyspace 'store' with UDTs, table and SAI indexes"
docker exec dbui-cassandra cqlsh -e "\
CREATE KEYSPACE IF NOT EXISTS store WITH replication = {'class':'SimpleStrategy','replication_factor':1}; \
CREATE TYPE IF NOT EXISTS store.address (street text, city text, zip text, country text); \
CREATE TYPE IF NOT EXISTS store.dimensions (width_cm double, height_cm double, depth_cm double); \
CREATE TABLE IF NOT EXISTS store.products ( \
  id uuid PRIMARY KEY, \
  name text, \
  description text, \
  price decimal, \
  currency text, \
  in_stock boolean, \
  rating float, \
  tags set<text>, \
  categories list<text>, \
  ship_from frozen<address>, \
  dims frozen<dimensions>, \
  created_at timestamp); \
CREATE CUSTOM INDEX IF NOT EXISTS products_name_sai ON store.products(name) USING 'StorageAttachedIndex'; \
CREATE CUSTOM INDEX IF NOT EXISTS products_price_sai ON store.products(price) USING 'StorageAttachedIndex'; \
CREATE CUSTOM INDEX IF NOT EXISTS products_in_stock_sai ON store.products(in_stock) USING 'StorageAttachedIndex'; \
CREATE CUSTOM INDEX IF NOT EXISTS products_rating_sai ON store.products(rating) USING 'StorageAttachedIndex'; \
INSERT INTO store.products (id,name,description,price,currency,in_stock,rating,tags,categories,ship_from,dims,created_at) VALUES ( \
  $ID1, 'Mechanical Keyboard', 'Hot-swappable 75% mechanical keyboard with PBT keycaps.', 129.99, 'USD', true, 4.7, \
  {'mechanical','rgb','wireless'}, ['electronics','peripherals'], \
  {street:'12 Market St', city:'Lisbon', zip:'1100-001', country:'PT'}, \
  {width_cm:32.5, height_cm:3.5, depth_cm:13.5}, '2026-01-15T10:30:00Z'); \
INSERT INTO store.products (id,name,description,price,currency,in_stock,rating,tags,categories,ship_from,dims,created_at) VALUES ( \
  $ID2, 'Wireless Mouse', 'Ergonomic wireless mouse, 8000 DPI sensor.', 49.50, 'USD', false, 4.2, \
  {'wireless','ergonomic'}, ['electronics','peripherals'], \
  {street:'5 King Rd', city:'Berlin', zip:'10115', country:'DE'}, \
  {width_cm:6.5, height_cm:4.0, depth_cm:11.0}, '2026-02-02T08:00:00Z'); \
INSERT INTO store.products (id,name,description,price,currency,in_stock,rating,tags,categories,ship_from,dims,created_at) VALUES ( \
  $ID3, '4K Monitor', '27-inch 4K IPS monitor with USB-C.', 399.00, 'USD', true, 4.8, \
  {'4k','ips','usb-c'}, ['electronics','displays'], \
  {street:'88 Bay Ave', city:'Toronto', zip:'M5J 2N8', country:'CA'}, \
  {width_cm:61.0, height_cm:45.0, depth_cm:20.0}, '2026-03-10T14:45:00Z');"

echo "==> OpenSearch: (re)create index 'products' with a non-trivial mapping"
curl -sf -X DELETE "${OS_URL}/products" >/dev/null 2>&1 || true
curl -sf -X PUT "${OS_URL}/products" -H 'Content-Type: application/json' -d '{
  "settings": { "number_of_shards": 1, "number_of_replicas": 0 },
  "mappings": {
    "properties": {
      "name":        { "type": "text", "fields": { "keyword": { "type": "keyword" } } },
      "description": { "type": "text" },
      "price":       { "type": "scaled_float", "scaling_factor": 100 },
      "currency":    { "type": "keyword" },
      "in_stock":    { "type": "boolean" },
      "rating":      { "type": "half_float" },
      "tags":        { "type": "keyword" },
      "categories":  { "type": "keyword" },
      "ship_from": {
        "type": "object",
        "properties": {
          "street":  { "type": "text" },
          "city":    { "type": "keyword" },
          "zip":     { "type": "keyword" },
          "country": { "type": "keyword" }
        }
      },
      "dims": {
        "type": "object",
        "properties": {
          "width_cm":  { "type": "float" },
          "height_cm": { "type": "float" },
          "depth_cm":  { "type": "float" }
        }
      },
      "created_at": { "type": "date" },
      "location":   { "type": "geo_point" }
    }
  }
}' >/dev/null

echo "==> OpenSearch: index matching documents (same ids as Cassandra)"
curl -sf -X PUT "${OS_URL}/products/_doc/${ID1}?refresh=true" -H 'Content-Type: application/json' -d '{
  "name":"Mechanical Keyboard","description":"Hot-swappable 75% mechanical keyboard with PBT keycaps.",
  "price":129.99,"currency":"USD","in_stock":true,"rating":4.7,
  "tags":["mechanical","rgb","wireless"],"categories":["electronics","peripherals"],
  "ship_from":{"street":"12 Market St","city":"Lisbon","zip":"1100-001","country":"PT"},
  "dims":{"width_cm":32.5,"height_cm":3.5,"depth_cm":13.5},
  "created_at":"2026-01-15T10:30:00Z","location":{"lat":38.7223,"lon":-9.1393}
}' >/dev/null
curl -sf -X PUT "${OS_URL}/products/_doc/${ID2}?refresh=true" -H 'Content-Type: application/json' -d '{
  "name":"Wireless Mouse","description":"Ergonomic wireless mouse, 8000 DPI sensor.",
  "price":49.50,"currency":"USD","in_stock":false,"rating":4.2,
  "tags":["wireless","ergonomic"],"categories":["electronics","peripherals"],
  "ship_from":{"street":"5 King Rd","city":"Berlin","zip":"10115","country":"DE"},
  "dims":{"width_cm":6.5,"height_cm":4.0,"depth_cm":11.0},
  "created_at":"2026-02-02T08:00:00Z","location":{"lat":52.5200,"lon":13.4050}
}' >/dev/null
curl -sf -X PUT "${OS_URL}/products/_doc/${ID3}?refresh=true" -H 'Content-Type: application/json' -d '{
  "name":"4K Monitor","description":"27-inch 4K IPS monitor with USB-C.",
  "price":399.00,"currency":"USD","in_stock":true,"rating":4.8,
  "tags":["4k","ips","usb-c"],"categories":["electronics","displays"],
  "ship_from":{"street":"88 Bay Ave","city":"Toronto","zip":"M5J 2N8","country":"CA"},
  "dims":{"width_cm":61.0,"height_cm":45.0,"depth_cm":20.0},
  "created_at":"2026-03-10T14:45:00Z","location":{"lat":43.6532,"lon":-79.3832}
}' >/dev/null

echo "==> Done. Cassandra: store.products  |  OpenSearch: products"
