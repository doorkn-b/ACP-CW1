package uk.ac.ed.acp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Drone {
    private String name;
    private String id;
    private Capability capability;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double costPer100Moves;

    public Drone() {}

    public void enrich() {
        double ci = safeValue(capability.getCostInitial());
        double cf = safeValue(capability.getCostFinal());
        double cpm = safeValue(capability.getCostPerMove());
        this.costPer100Moves = ci + cf + cpm * 100.0;
    }

    private double safeValue(double val) {
        return Double.isNaN(val) ? 0.0 : val;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Capability getCapability() { return capability; }
    public void setCapability(Capability capability) { this.capability = capability; }

    public Double getCostPer100Moves() { return costPer100Moves; }
    public void setCostPer100Moves(Double costPer100Moves) { this.costPer100Moves = costPer100Moves; }
}
