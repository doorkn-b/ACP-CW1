package uk.ac.ed.acp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acp.service.S3Service;

import java.util.List;

@RestController
@RequestMapping("/api/v1/acp")
public class S3Controller {

    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    public S3Controller(S3Service s3Service, ObjectMapper objectMapper) {
        this.s3Service = s3Service;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/all/s3/{bucket}")
    public ResponseEntity<List<JsonNode>> getAllS3Objects(@PathVariable String bucket) {
        try {
            List<JsonNode> objects = s3Service.getAllObjects(bucket);
            return ResponseEntity.ok(objects);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/single/s3/{bucket}/{key}")
    public ResponseEntity<JsonNode> getSingleS3Object(
            @PathVariable String bucket,
            @PathVariable String key) {
        try {
            String content = s3Service.getObjectContent(bucket, key);
            JsonNode json = objectMapper.readTree(content);
            return ResponseEntity.ok(json);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
