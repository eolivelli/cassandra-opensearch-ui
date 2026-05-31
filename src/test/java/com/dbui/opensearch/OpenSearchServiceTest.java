package com.dbui.opensearch;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbui.config.OpenSearchProperties;
import com.dbui.model.OsDocumentsResult;
import com.dbui.model.OsIndicesResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration tests for {@link OpenSearchService} against a real OpenSearch
 * container (security plugin disabled).
 */
@Testcontainers
class OpenSearchServiceTest {

    private static final String IMAGE =
            System.getProperty("opensearch.image", "opensearchproject/opensearch:3.6.0");

    @Container
    static final GenericContainer<?> OPENSEARCH =
            new GenericContainer<>(DockerImageName.parse(IMAGE))
                    .withExposedPorts(9200)
                    .withEnv("discovery.type", "single-node")
                    .withEnv("DISABLE_SECURITY_PLUGIN", "true")
                    .withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m")
                    .waitingFor(Wait.forHttp("/_cluster/health")
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(4)));

    static final ObjectMapper MAPPER = new ObjectMapper();
    static OpenSearchService service;

    @BeforeAll
    static void setUp() {
        OpenSearchProperties props = new OpenSearchProperties();
        props.setUrl("http://" + OPENSEARCH.getHost() + ":" + OPENSEARCH.getMappedPort(9200));

        OpenSearchClient client = new OpenSearchClient(props, MAPPER);
        service = new OpenSearchService(client, MAPPER);

        // Seed two documents and refresh so they are immediately searchable.
        client.post("/products/_doc?refresh=true",
                "{\"name\":\"Keyboard\",\"price\":49.9,\"in_stock\":true}");
        client.post("/products/_doc?refresh=true",
                "{\"name\":\"Mouse\",\"price\":19.5,\"in_stock\":false}");
    }

    @Test
    void listsIndicesIncludingSeededIndex() {
        OsIndicesResult result = service.listIndices();

        assertThat(result.request().method()).isEqualTo("GET");
        assertThat(result.request().path()).contains("/_cat/indices");

        List<String> names = result.indices().stream()
                .map(i -> (String) i.get("index"))
                .toList();
        assertThat(names).contains("products");
    }

    @Test
    void returnsDocumentsWithColumnsAndRequest() {
        OsDocumentsResult result = service.indexDocuments("products", 50);

        assertThat(result.request().method()).isEqualTo("POST");
        assertThat(result.request().path()).isEqualTo("/products/_search");
        assertThat(result.request().body()).contains("match_all");
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.columns()).contains("_id", "name", "price", "in_stock");

        List<String> names = result.documents().stream()
                .map(d -> (String) d.get("name"))
                .toList();
        assertThat(names).containsExactlyInAnyOrder("Keyboard", "Mouse");
    }

    @Test
    void honoursSizeLimit() {
        OsDocumentsResult result = service.indexDocuments("products", 1);

        assertThat(result.documents()).hasSize(1);
        assertThat(result.total()).isEqualTo(2);
        Map<String, Object> doc = result.documents().get(0);
        assertThat(doc).containsKey("_id");
    }
}
