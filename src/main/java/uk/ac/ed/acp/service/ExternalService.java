package uk.ac.ed.acp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.ac.ed.acp.model.Drone;

import java.util.List;

@Service
public class ExternalService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ExternalService(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    public List<Drone> fetchAndEnrichDrones(String urlPath) throws Exception {
        String json = restTemplate.getForObject(urlPath, String.class);
        List<Drone> drones = objectMapper.readValue(json, new TypeReference<List<Drone>>() {});
        for (Drone drone : drones) {
            drone.enrich();
        }
        return drones;
    }
}
