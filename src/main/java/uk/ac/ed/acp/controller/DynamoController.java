package uk.ac.ed.acp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acp.service.DynamoService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/acp")
public class DynamoController {

    private final DynamoService dynamoService;

    public DynamoController(DynamoService dynamoService) {
        this.dynamoService = dynamoService;
    }

    @GetMapping("/all/dynamo/{table}")
    public ResponseEntity<List<JsonNode>> getAllDynamo(@PathVariable String table) {
        try {
            return ResponseEntity.ok(dynamoService.getAllItems(table));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/single/dynamo/{table}/{key}")
    public ResponseEntity<JsonNode> getSingleDynamo(
            @PathVariable String table,
            @PathVariable String key) {
        try {
            JsonNode item = dynamoService.getItem(table, key);
            if (item == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
