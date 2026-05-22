package org.example.demo.model;

import jakarta.validation.constraints.NotBlank;

public record TravelPlanRevisionRequest(
        @NotBlank String message
) {
}
