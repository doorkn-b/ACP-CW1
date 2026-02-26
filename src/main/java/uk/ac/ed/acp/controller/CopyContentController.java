package uk.ac.ed.acp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acp.config.SidProvider;
import uk.ac.ed.acp.service.DynamoService;
import uk.ac.ed.acp.service.PostgresService;
import uk.ac.ed.acp.service.S3Service;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/acp")
public class CopyContentController {

    private final PostgresService postgresService;
    private final DynamoService dynamoService;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;
    private final SidProvider sidProvider;

    public CopyContentController(PostgresService postgresService,
                                 DynamoService dynamoService,
                                 S3Service s3Service,
                                 ObjectMapper objectMapper,
                                 SidProvider sidProvider) {
        this.postgresService = postgresService;
        this.dynamoService = dynamoService;
        this.s3Service = s3Service;
        this.objectMapper = objectMapper;
        this.sidProvider = sidProvider;
    }

    @PostMapping("/copy-content/dynamo/{table}")
    public ResponseEntity<Void> copyToDynamo(@PathVariable String table) {
        try {
            List<JsonNode> rows = postgresService.getAllRows(table);
            String sid = sidProvider.getSid();
            for (JsonNode row : rows) {
                String uuid = UUID.randomUUID().toString();
                ObjectNode wrapper = objectMapper.createObjectNode();
                wrapper.set("content", row);
                dynamoService.putItem(sid, uuid, objectMapper.writeValueAsString(wrapper));
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/copy-content/S3/{table}")
    public ResponseEntity<Void> copyToS3(@PathVariable String table) {
        try {
            List<JsonNode> rows = postgresService.getAllRows(table);
            String sid = sidProvider.getSid();
            for (JsonNode row : rows) {
                String uuid = UUID.randomUUID().toString();
                String json = objectMapper.writeValueAsString(row);
                s3Service.putObject(sid, uuid, json);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
