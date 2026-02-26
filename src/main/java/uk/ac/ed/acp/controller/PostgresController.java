package uk.ac.ed.acp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acp.service.PostgresService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/acp")
public class PostgresController {

    private final PostgresService postgresService;

    public PostgresController(PostgresService postgresService) {
        this.postgresService = postgresService;
    }

    @GetMapping("/all/postgres/{table}")
    public ResponseEntity<List<JsonNode>> getAllPostgres(@PathVariable String table) {
        try {
            return ResponseEntity.ok(postgresService.getAllRows(table));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
