package org.example.demo.model;

import java.time.Instant;

public record ItinerarySummaryResponse(
        Long itineraryId,
        int revisionNo,
        boolean favorite,
        String departureCity,
        String destination,
        String startDate,
        String endDate,
        Instant createdAt,
        Instant updatedAt,
        String title,
        String overview
) {
}
