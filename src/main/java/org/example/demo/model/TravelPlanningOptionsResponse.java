package org.example.demo.model;

import java.util.List;

public record TravelPlanningOptionsResponse(
        List<String> featuredDestinations,
        List<String> interests,
        List<String> budgetLevels,
        List<String> paceOptions,
        List<String> hotelStyles
) {
}
