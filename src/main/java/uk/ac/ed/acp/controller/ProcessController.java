package uk.ac.ed.acp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acp.config.SidProvider;
import uk.ac.ed.acp.model.Drone;
import uk.ac.ed.acp.model.UrlPathRequest;
import uk.ac.ed.acp.service.DynamoService;
import uk.ac.ed.acp.service.ExternalService;
import uk.ac.ed.acp.service.PostgresService;
import uk.ac.ed.acp.service.S3Service;

import java.util.List;

@RestController
@RequestMapping("/api/v1/acp")
public class ProcessController {

    private final ExternalService externalService;
    private final DynamoService dynamoService;
    private final S3Service s3Service;
    private final PostgresService postgresService;
    private final ObjectMapper objectMapper;
    private final SidProvider sidProvider;

    public ProcessController(ExternalService externalService,
                             DynamoService dynamoService,
                             S3Service s3Service,
                             PostgresService postgresService,
                             ObjectMapper objectMapper,
                             SidProvider sidProvider) {
        this.externalService = externalService;
        this.dynamoService = dynamoService;
        this.s3Service = s3Service;
        this.postgresService = postgresService;
        this.objectMapper = objectMapper;
        this.sidProvider = sidProvider;
    }

    @PostMapping("/process/dump")
    public ResponseEntity<List<Drone>> processDump(@RequestBody UrlPathRequest request) {
        try {
            List<Drone> drones = externalService.fetchAndEnrichDrones(request.getUrlPath());
            return ResponseEntity.ok(drones);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/process/dynamo")
    public ResponseEntity<Void> processDynamo(@RequestBody UrlPathRequest request) {
        try {
            List<Drone> drones = externalService.fetchAndEnrichDrones(request.getUrlPath());
            String sid = sidProvider.getSid();
            for (Drone drone : drones) {
                ObjectNode wrapper = objectMapper.createObjectNode();
                wrapper.set("content", objectMapper.valueToTree(drone));
                dynamoService.putItem(sid, drone.getName(), objectMapper.writeValueAsString(wrapper));
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/process/s3")
    public ResponseEntity<Void> processS3(@RequestBody UrlPathRequest request) {
        try {
            List<Drone> drones = externalService.fetchAndEnrichDrones(request.getUrlPath());
            String sid = sidProvider.getSid();
            for (Drone drone : drones) {
                String json = objectMapper.writeValueAsString(drone);
                s3Service.putObject(sid, drone.getName(), json);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/process/postgres/{table}")
    public ResponseEntity<Void> processPostgres(
            @PathVariable String table,
            @RequestBody UrlPathRequest request) {
        try {
            List<Drone> drones = externalService.fetchAndEnrichDrones(request.getUrlPath());
            for (Drone drone : drones) {
                JsonNode droneJson = objectMapper.valueToTree(drone);
                postgresService.insertRow(table, droneJson);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
