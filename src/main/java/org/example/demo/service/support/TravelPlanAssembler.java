package org.example.demo.service.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.demo.model.TravelPlanRequest;
import org.example.demo.model.TravelPlanResponse;
import org.springframework.stereotype.Component;

@Component
public class TravelPlanAssembler {

    private final ObjectMapper objectMapper;

    public TravelPlanAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String writeRequest(TravelPlanRequest request) {
        return writeValue(request);
    }

    public String writePlan(TravelPlanResponse response) {
        return writeValue(response);
    }

    public TravelPlanRequest readRequest(String json) {
        try {
            return objectMapper.readValue(json, TravelPlanRequest.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize travel request", exception);
        }
    }

    public TravelPlanResponse readPlan(String json) {
        try {
            return objectMapper.readValue(json, TravelPlanResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize travel plan", exception);
        }
    }

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize travel data", exception);
        }
    }
}
