package org.example.demo.model;

import java.time.Instant;

public record StoredTravelPlanResponse(
        Long itineraryId,
        String sessionCode,
        int revisionNo,
        boolean favorite,
        String departureCity,
        String destination,
        String startDate,
        String endDate,
        Instant createdAt,
        Instant updatedAt,
        TravelPlanRequest request,
        TravelPlanResponse plan
) {
}
