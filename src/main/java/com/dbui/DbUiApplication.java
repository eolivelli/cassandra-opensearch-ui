package com.dbui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * DB UI - a read-only web console to inspect a local Cassandra 5 cluster and a
 * local OpenSearch 3.x cluster.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DbUiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbUiApplication.class, args);
    }
}
