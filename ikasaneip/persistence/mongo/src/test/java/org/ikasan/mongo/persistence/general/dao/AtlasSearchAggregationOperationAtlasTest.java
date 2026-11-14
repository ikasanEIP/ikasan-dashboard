package org.ikasan.mongo.persistence.general.dao;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.testcontainers.mongodb.MongoDBAtlasLocalContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Integration tests for AtlasSearchAggregationOperation using MongoDB Atlas Local.
 * These tests verify Atlas Search functionality with actual search indexes.
 *
 * @author Ikasan Development Team
 */
public class AtlasSearchAggregationOperationAtlasTest {

    private static MongoDBAtlasLocalContainer atlasContainer;
    private static MongoClient mongoClient;
    private static MongoTemplate mongoTemplate;
    private static MongoDatabase database;
    private static final String COLLECTION_NAME = "testDocuments";

    @BeforeClass
    public static void startContainer() {
        atlasContainer = new MongoDBAtlasLocalContainer("mongodb/mongodb-atlas-local:7.0.14");
        atlasContainer.start();

        mongoClient = MongoClients.create(atlasContainer.getConnectionString());
        database = mongoClient.getDatabase("testdb");
        mongoTemplate = new MongoTemplate(mongoClient, "testdb");
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoClient != null) {
            mongoClient.close();
        }
        if (atlasContainer != null) {
            atlasContainer.stop();
        }
    }

    @Before
    public void setup() {
        // Create collection and insert test documents
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        collection.drop();

        // Insert test documents (this also creates the collection)
        collection.insertOne(new Document()
            .append("moduleName", "myModule")
            .append("flowName", "myFlow")
            .append("componentName", "myComponent")
            .append("status", "COMPLETE")
            .append("description", "This is a test document"));

        collection.insertOne(new Document()
            .append("moduleName", "anotherModule")
            .append("flowName", "anotherFlow")
            .append("componentName", "anotherComponent")
            .append("status", "ERROR")
            .append("description", "Another test document"));

        collection.insertOne(new Document()
            .append("moduleName", "myModule")
            .append("flowName", "differentFlow")
            .append("componentName", "differentComponent")
            .append("status", "COMPLETE")
            .append("description", "Different module instance"));

        collection.insertOne(new Document()
            .append("moduleName", "testModule")
            .append("flowName", "testFlow")
            .append("componentName", "testComponent")
            .append("status", "SKIPPED")
            .append("description", "Test status document"));

        // Create Atlas Search index for wildcard searches (collection now exists)
        createSearchIndex();
    }

    @After
    public void teardown() {
        database.getCollection(COLLECTION_NAME).drop();
    }

    private void createSearchIndex() {
        // Create a search index that supports wildcard queries
        Document indexDefinition = new Document()
            .append("mappings", new Document()
                .append("dynamic", true));

        database.runCommand(new Document()
            .append("createSearchIndexes", COLLECTION_NAME)
            .append("indexes", List.of(
                new Document()
                    .append("name", "default")
                    .append("definition", indexDefinition)
            ))
        );

        // Wait for index to be ready
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void test_compoundMustWildcardSearch_single_field() {
        // Given: field-value pair for moduleName
        List<AtlasSearchAggregationOperation.FieldValuePair> fieldValuePairs = List.of(
            new AtlasSearchAggregationOperation.FieldValuePair("moduleName", "myModule")
        );

        // When: execute compound must wildcard search
        AtlasSearchAggregationOperation searchOp =
            AtlasSearchAggregationOperation.compoundMustWildcardSearch("default", fieldValuePairs);

        Aggregation aggregation = Aggregation.newAggregation(searchOp);
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, COLLECTION_NAME, Document.class);

        // Then: should return 2 documents with moduleName="myModule"
        List<Document> resultList = results.getMappedResults();
        Assert.assertEquals(2, resultList.size());
        Assert.assertTrue(resultList.stream().allMatch(doc -> doc.getString("moduleName").equals("myModule")));
    }

    @Test
    public void test_compoundMustWildcardSearch_multiple_fields_and_logic() {
        // Given: field-value pairs for moduleName AND status (both must match)
        List<AtlasSearchAggregationOperation.FieldValuePair> fieldValuePairs = List.of(
            new AtlasSearchAggregationOperation.FieldValuePair("moduleName", "myModule"),
            new AtlasSearchAggregationOperation.FieldValuePair("status", "COMPLETE")
        );

        // When: execute compound must wildcard search
        AtlasSearchAggregationOperation searchOp =
            AtlasSearchAggregationOperation.compoundMustWildcardSearch("default", fieldValuePairs);

        Aggregation aggregation = Aggregation.newAggregation(searchOp);
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, COLLECTION_NAME, Document.class);

        // Then: should return 2 documents matching both conditions
        List<Document> resultList = results.getMappedResults();
        Assert.assertEquals(2, resultList.size());
        Assert.assertTrue(resultList.stream().allMatch(doc ->
            doc.getString("moduleName").equals("myModule") &&
            doc.getString("status").equals("COMPLETE")));
    }

    @Test
    public void test_compoundMustWildcardSearch_three_fields_and_logic() {
        // Given: field-value pairs for moduleName AND flowName AND status (all must match)
        List<AtlasSearchAggregationOperation.FieldValuePair> fieldValuePairs = List.of(
            new AtlasSearchAggregationOperation.FieldValuePair("moduleName", "myModule"),
            new AtlasSearchAggregationOperation.FieldValuePair("flowName", "myFlow"),
            new AtlasSearchAggregationOperation.FieldValuePair("status", "COMPLETE")
        );

        // When: execute compound must wildcard search
        AtlasSearchAggregationOperation searchOp =
            AtlasSearchAggregationOperation.compoundMustWildcardSearch("default", fieldValuePairs);

        AtomicReference<List<Document>> resultList = new AtomicReference<>();

        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Aggregation aggregation = Aggregation.newAggregation(searchOp);
            AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, COLLECTION_NAME, Document.class);

            // Then: should return 1 document matching all three conditions
            resultList.set(results.getMappedResults());
            Assert.assertEquals(1, resultList.get().size());
        });

        Document doc = resultList.get().get(0);
        Assert.assertEquals("myModule", doc.getString("moduleName"));
        Assert.assertEquals("myFlow", doc.getString("flowName"));
        Assert.assertEquals("COMPLETE", doc.getString("status"));
    }

    @Test
    public void test_compoundMustWildcardSearch_partial_match_substring() {
        // Given: field-value pair with partial match (substring)
        List<AtlasSearchAggregationOperation.FieldValuePair> fieldValuePairs = List.of(
            new AtlasSearchAggregationOperation.FieldValuePair("moduleName", "Module")
        );

        // When: execute compound must wildcard search (wildcards are added automatically)
        AtlasSearchAggregationOperation searchOp =
            AtlasSearchAggregationOperation.compoundMustWildcardSearch("default", fieldValuePairs);

        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Aggregation aggregation = Aggregation.newAggregation(searchOp);
            AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, COLLECTION_NAME, Document.class);

            // Then: should return all documents containing "Module" substring
            List<Document> resultList = results.getMappedResults();
            Assert.assertEquals(4, resultList.size()); // all documents contain "Module"
        });
    }

    @Test
    public void test_compoundMustWildcardSearch_no_match() {
        // Given: field-value pair that doesn't match any document
        List<AtlasSearchAggregationOperation.FieldValuePair> fieldValuePairs = List.of(
            new AtlasSearchAggregationOperation.FieldValuePair("moduleName", "nonExistent")
        );

        // When: execute compound must wildcard search
        AtlasSearchAggregationOperation searchOp =
            AtlasSearchAggregationOperation.compoundMustWildcardSearch("default", fieldValuePairs);

        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Aggregation aggregation = Aggregation.newAggregation(searchOp);
            AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, COLLECTION_NAME, Document.class);

            // Then: should return no documents
            List<Document> resultList = results.getMappedResults();
            Assert.assertEquals(0, resultList.size());
        });
    }

    @Test
    @Ignore
    public void test_compoundMustWildcardSearch_case_insensitive() {
        // Given: field-value pair with different case
        List<AtlasSearchAggregationOperation.FieldValuePair> fieldValuePairs = List.of(
            new AtlasSearchAggregationOperation.FieldValuePair("moduleName", "MYMODULE")
        );

        // When: execute compound must wildcard search
        AtlasSearchAggregationOperation searchOp =
            AtlasSearchAggregationOperation.compoundMustWildcardSearch("default", fieldValuePairs);

        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Aggregation aggregation = Aggregation.newAggregation(searchOp);
            AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, COLLECTION_NAME, Document.class);

            // Then: should return documents matching case-insensitively
            List<Document> resultList = results.getMappedResults();
            Assert.assertTrue(resultList.size() >= 2); // Should match "myModule" case-insensitively
        });
    }

    @Test
    public void test_compoundOrWildcardSearch_or_logic() {
        // Given: search query across multiple fields with OR logic
        List<String> searchFields = List.of("moduleName", "flowName");

        // When: execute compound OR wildcard search
        AtlasSearchAggregationOperation searchOp =
            AtlasSearchAggregationOperation.compoundOrWildcardSearch("default", "myFlow", searchFields);


        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Aggregation aggregation = Aggregation.newAggregation(searchOp);
            AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, COLLECTION_NAME, Document.class);

            // Then: should return document where flowName matches "myFlow"
            List<Document> resultList = results.getMappedResults();
            Assert.assertTrue(resultList.size() >= 1);
            Assert.assertTrue(resultList.stream().anyMatch(doc -> "myFlow".equals(doc.getString("flowName"))));
        });
    }

    @Test
    public void test_singleFieldWildcardSearch() {
        // When: execute single field wildcard search
        AtlasSearchAggregationOperation searchOp =
            AtlasSearchAggregationOperation.singleFieldWildcardSearch("default", "myModule", "moduleName");

        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Aggregation aggregation = Aggregation.newAggregation(searchOp);
            AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, COLLECTION_NAME, Document.class);

            // Then: should return documents matching moduleName
            List<Document> resultList = results.getMappedResults();
            Assert.assertEquals(2, resultList.size());
            Assert.assertTrue(resultList.stream().allMatch(doc -> doc.getString("moduleName").equals("myModule")));
        });

    }

    @Test
    public void test_fieldValuePair_record() {
        // Given: FieldValuePair record
        AtlasSearchAggregationOperation.FieldValuePair pair =
            new AtlasSearchAggregationOperation.FieldValuePair("testField", "testValue");

        // Then: record methods work correctly
        Assert.assertEquals("testField", pair.field());
        Assert.assertEquals("testValue", pair.value());
    }

    @Test
    public void test_compoundMustWildcardSearch_generates_correct_document() {
        // Given: field-value pairs
        List<AtlasSearchAggregationOperation.FieldValuePair> fieldValuePairs = List.of(
            new AtlasSearchAggregationOperation.FieldValuePair("field1", "value1"),
            new AtlasSearchAggregationOperation.FieldValuePair("field2", "value2")
        );

        // When: create search operation
        AtlasSearchAggregationOperation searchOp =
            AtlasSearchAggregationOperation.compoundMustWildcardSearch("testIndex", fieldValuePairs);

        // Then: should generate correct search document structure
        Document searchDoc = searchOp.toDocument(null);
        Assert.assertTrue(searchDoc.containsKey("$search"));

        Document search = searchDoc.get("$search", Document.class);
        Assert.assertEquals("testIndex", search.getString("index"));
        Assert.assertTrue(search.containsKey("compound"));

        Document compound = search.get("compound", Document.class);
        Assert.assertTrue(compound.containsKey("must"));

        @SuppressWarnings("unchecked")
        List<Document> mustClauses = (List<Document>) compound.get("must");
        Assert.assertEquals(2, mustClauses.size());

        // Verify wildcard clauses
        for (Document clause : mustClauses) {
            Assert.assertTrue(clause.containsKey("wildcard"));
            Document wildcard = clause.get("wildcard", Document.class);
            Assert.assertTrue(wildcard.containsKey("query"));
            Assert.assertTrue(wildcard.containsKey("path"));
            Assert.assertTrue(wildcard.getBoolean("allowAnalyzedField"));
            // Verify wildcards are added
            Assert.assertTrue(wildcard.getString("query").startsWith("*"));
            Assert.assertTrue(wildcard.getString("query").endsWith("*"));
        }
    }
}
