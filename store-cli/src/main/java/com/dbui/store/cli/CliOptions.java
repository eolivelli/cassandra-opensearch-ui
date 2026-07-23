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
package com.dbui.store.cli;

import java.io.PrintStream;
import java.nio.file.Path;

/**
 * Parsed command-line options for the ingest CLI. Defaults match the local Cassandra and OpenSearch
 * started by the repository's docker-compose.yml.
 */
record CliOptions(boolean help, Path file, String cassandraHost, int cassandraPort,
        String datacenter, String keyspace, String openSearchUrl, String index, boolean recreate) {

    static CliOptions parse(String[] args) {
        boolean help = false;
        Path file = null;
        String cassandraHost = "127.0.0.1";
        int cassandraPort = 9042;
        String datacenter = "datacenter1";
        String keyspace = "h2o_store";
        String openSearchUrl = "http://127.0.0.1:9200";
        String index = "h2o_products";
        boolean recreate = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-h", "--help" -> help = true;
                case "--recreate" -> recreate = true;
                case "-f", "--file" -> file = Path.of(requireValue(args, ++i, arg));
                case "--cassandra-host" -> cassandraHost = requireValue(args, ++i, arg);
                case "--cassandra-port" ->
                    cassandraPort = Integer.parseInt(requireValue(args, ++i, arg));
                case "--datacenter" -> datacenter = requireValue(args, ++i, arg);
                case "--keyspace" -> keyspace = requireValue(args, ++i, arg);
                case "--opensearch-url" -> openSearchUrl = requireValue(args, ++i, arg);
                case "--index" -> index = requireValue(args, ++i, arg);
                default -> {
                    if (arg.startsWith("-")) {
                        throw new IllegalArgumentException("Unknown option: " + arg);
                    }
                    // A bare argument is treated as the input file.
                    file = Path.of(arg);
                }
            }
        }

        if (!help && file == null) {
            throw new IllegalArgumentException("No input file given (use --file <products.jsonl>)");
        }
        return new CliOptions(help, file, cassandraHost, cassandraPort, datacenter, keyspace,
                openSearchUrl, index, recreate);
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Option " + option + " requires a value");
        }
        return args[index];
    }

    static void printUsage(PrintStream out) {
        out.println(
                """
                        H2O Store — product ingest CLI

                        Reads a JSON Lines file (one product per line) and loads every product into
                        Cassandra (system of record) and OpenSearch (search index).

                        Usage:
                          java -jar store-cli.jar --file <products.jsonl> [options]

                        Options:
                          -f, --file <path>          input JSON Lines file (required)
                              --cassandra-host <h>    Cassandra contact host   (default 127.0.0.1)
                              --cassandra-port <p>    Cassandra native port     (default 9042)
                              --datacenter <dc>       local datacenter          (default datacenter1)
                              --keyspace <ks>         Cassandra keyspace        (default h2o_store)
                              --opensearch-url <url>  OpenSearch base URL       (default http://127.0.0.1:9200)
                              --index <name>          OpenSearch index          (default h2o_products)
                              --recreate              truncate tables and drop/recreate the index
                                                      first (use when re-ingesting products whose
                                                      name or category changed)
                          -h, --help                  show this help

                        Each JSON line looks like:
                          {"name":"Neon Tetra","description":"...","category":"Live Fish",
                           "tags":["schooling","freshwater"],"price":4.99,
                           "image":"/products/neon-tetra.jpg","species":"Paracheirodon innesi","stock":120}
                        """);
    }
}
