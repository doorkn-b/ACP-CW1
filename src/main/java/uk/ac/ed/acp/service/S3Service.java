package uk.ac.ed.acp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.ArrayList;
import java.util.List;

@Service
public class S3Service {

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;

    public S3Service(S3Client s3Client, ObjectMapper objectMapper) {
        this.s3Client = s3Client;
        this.objectMapper = objectMapper;
    }

    public List<JsonNode> getAllObjects(String bucket) throws Exception {
        ListObjectsV2Response listResp = s3Client.listObjectsV2(
                ListObjectsV2Request.builder().bucket(bucket).build()
        );
        List<JsonNode> results = new ArrayList<>();
        for (S3Object obj : listResp.contents()) {
            String content = getObjectContent(bucket, obj.key());
            results.add(objectMapper.readTree(content));
        }
        return results;
    }

    public String getObjectContent(String bucket, String key) {
        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(bucket).key(key).build();
        return s3Client.getObjectAsBytes(req).asUtf8String();
    }

    public void putObject(String bucket, String key, String jsonContent) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket).key(key)
                        .contentType("application/json")
                        .build(),
                RequestBody.fromString(jsonContent)
        );
    }
}
