package uk.ac.ed.acp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

@Service
public class DynamoService {

    private final DynamoDbClient dynamoDbClient;
    private final ObjectMapper objectMapper;

    public DynamoService(DynamoDbClient dynamoDbClient, ObjectMapper objectMapper) {
        this.dynamoDbClient = dynamoDbClient;
        this.objectMapper = objectMapper;
    }

    public List<JsonNode> getAllItems(String table) {
        ScanResponse response = dynamoDbClient.scan(
                ScanRequest.builder().tableName(table).build()
        );
        List<JsonNode> results = new ArrayList<>();
        for (Map<String, AttributeValue> item : response.items()) {
            results.add(attributeMapToJsonNode(item));
        }
        return results;
    }

    public JsonNode getItem(String table, String key) {
        DescribeTableResponse desc = dynamoDbClient.describeTable(
                DescribeTableRequest.builder().tableName(table).build()
        );
        String partitionKeyName = desc.table().keySchema().stream()
                .filter(k -> k.keyType() == KeyType.HASH)
                .map(KeySchemaElement::attributeName)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No partition key found"));

        Map<String, AttributeValue> keyMap = Map.of(
                partitionKeyName, AttributeValue.builder().s(key).build()
        );
        GetItemResponse resp = dynamoDbClient.getItem(
                GetItemRequest.builder().tableName(table).key(keyMap).build()
        );
        if (!resp.hasItem() || resp.item().isEmpty()) {
            return null;
        }
        return attributeMapToJsonNode(resp.item());
    }

    public void putItem(String table, String key, String jsonContent) throws Exception {
        JsonNode root = objectMapper.readTree(jsonContent);
        Map<String, AttributeValue> item = jsonNodeToAttributeMap(root);

        // Ensure the partition key is set
        DescribeTableResponse desc = dynamoDbClient.describeTable(
                DescribeTableRequest.builder().tableName(table).build()
        );
        String partitionKeyName = desc.table().keySchema().stream()
                .filter(k -> k.keyType() == KeyType.HASH)
                .map(KeySchemaElement::attributeName)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No partition key found"));

        // Always force-set the partition key to the provided value
        item.put(partitionKeyName, AttributeValue.builder().s(key).build());

        dynamoDbClient.putItem(
                PutItemRequest.builder().tableName(table).item(item).build()
        );
    }

    // --- Conversion: DynamoDB AttributeValue -> Jackson JsonNode ---

    private JsonNode attributeMapToJsonNode(Map<String, AttributeValue> item) {
        ObjectNode node = objectMapper.createObjectNode();
        for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
            node.set(entry.getKey(), convertAttributeValue(entry.getValue()));
        }
        return node;
    }

    private JsonNode convertAttributeValue(AttributeValue av) {
        if (av.s() != null) {
            return objectMapper.getNodeFactory().textNode(av.s());
        }
        if (av.n() != null) {
            String numStr = av.n();
            if (numStr.contains(".")) {
                return objectMapper.getNodeFactory().numberNode(Double.parseDouble(numStr));
            }
            try {
                return objectMapper.getNodeFactory().numberNode(Integer.parseInt(numStr));
            } catch (NumberFormatException e) {
                return objectMapper.getNodeFactory().numberNode(Long.parseLong(numStr));
            }
        }
        if (av.bool() != null) {
            return objectMapper.getNodeFactory().booleanNode(av.bool());
        }
        if (Boolean.TRUE.equals(av.nul())) {
            return objectMapper.getNodeFactory().nullNode();
        }
        if (av.hasM()) {
            return attributeMapToJsonNode(av.m());
        }
        if (av.hasL()) {
            ArrayNode arrNode = objectMapper.createArrayNode();
            for (AttributeValue child : av.l()) {
                arrNode.add(convertAttributeValue(child));
            }
            return arrNode;
        }
        return objectMapper.getNodeFactory().nullNode();
    }

    // --- Conversion: Jackson JsonNode -> DynamoDB AttributeValue ---

    private Map<String, AttributeValue> jsonNodeToAttributeMap(JsonNode root) {
        Map<String, AttributeValue> item = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            item.put(field.getKey(), jsonNodeToAttributeValue(field.getValue()));
        }
        return item;
    }

    private AttributeValue jsonNodeToAttributeValue(JsonNode node) {
        if (node.isTextual()) {
            return AttributeValue.builder().s(node.asText()).build();
        }
        if (node.isNumber()) {
            return AttributeValue.builder().n(node.asText()).build();
        }
        if (node.isBoolean()) {
            return AttributeValue.builder().bool(node.asBoolean()).build();
        }
        if (node.isNull()) {
            return AttributeValue.builder().nul(true).build();
        }
        if (node.isObject()) {
            Map<String, AttributeValue> map = new HashMap<>();
            node.fields().forEachRemaining(e ->
                    map.put(e.getKey(), jsonNodeToAttributeValue(e.getValue()))
            );
            return AttributeValue.builder().m(map).build();
        }
        if (node.isArray()) {
            List<AttributeValue> list = new ArrayList<>();
            node.forEach(e -> list.add(jsonNodeToAttributeValue(e)));
            return AttributeValue.builder().l(list).build();
        }
        return AttributeValue.builder().nul(true).build();
    }
}
