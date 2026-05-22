package org.example.demo.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TravelPlanRequest(
        @NotBlank String departureCity,
        @NotBlank String destination,
        @NotBlank String startDate,
        @NotBlank String endDate,
        @Min(1) int adults,
        @Min(0) int children,
        @NotBlank String budgetLevel,
        @NotBlank String pace,
        @NotBlank String hotelStyle,
        @NotEmpty List<String> interests,
        String notes
) {
}
