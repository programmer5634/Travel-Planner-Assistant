package org.example.demo.controller;

import jakarta.validation.Valid;
import org.example.demo.model.ItinerarySummaryResponse;
import org.example.demo.model.PageResult;
import org.example.demo.model.StoredTravelPlanResponse;
import org.example.demo.model.TravelPlanRequest;
import org.example.demo.model.TravelPlanResponse;
import org.example.demo.model.TravelPlanRevisionRequest;
import org.example.demo.model.TravelPlanningOptionsResponse;
import org.example.demo.service.TravelPlannerService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/travel")
public class TravelPlannerController {

    private final TravelPlannerService travelPlannerService;

    public TravelPlannerController(TravelPlannerService travelPlannerService) {
        this.travelPlannerService = travelPlannerService;
    }

    @GetMapping("/options")
    public TravelPlanningOptionsResponse options() {
        return travelPlannerService.options();
    }

    @PostMapping("/plan")
    public TravelPlanResponse createPlan(@Valid @RequestBody TravelPlanRequest request) {
        return travelPlannerService.createPlan(request);
    }

    @PostMapping("/itineraries")
    public StoredTravelPlanResponse createItinerary(Authentication authentication,
                                                    @Valid @RequestBody TravelPlanRequest request) {
        return travelPlannerService.createItinerary(currentUserId(authentication), request);
    }

    @GetMapping("/itineraries")
    public PageResult<ItinerarySummaryResponse> listItineraries(Authentication authentication,
                                                                @RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false) String destination,
                                                                @RequestParam(required = false) Boolean favoriteOnly,
                                                                @RequestParam(required = false) String startDate,
                                                                @RequestParam(required = false) String endDate,
                                                                @RequestParam(defaultValue = "1") long pageNo,
                                                                @RequestParam(defaultValue = "10") long pageSize) {
        return travelPlannerService.listItineraries(currentUserId(authentication), keyword, destination, favoriteOnly, startDate, endDate, pageNo, pageSize);
    }

    @GetMapping("/itineraries/{itineraryId}")
    public StoredTravelPlanResponse getItinerary(Authentication authentication,
                                                 @PathVariable Long itineraryId) {
        return travelPlannerService.getItinerary(currentUserId(authentication), itineraryId);
    }

    @PostMapping("/itineraries/{itineraryId}/revise")
    public StoredTravelPlanResponse reviseItinerary(Authentication authentication,
                                                    @PathVariable Long itineraryId,
                                                    @Valid @RequestBody TravelPlanRevisionRequest request) {
        return travelPlannerService.reviseItinerary(currentUserId(authentication), itineraryId, request);
    }

    @PutMapping("/itineraries/{itineraryId}/favorite")
    public StoredTravelPlanResponse updateFavorite(Authentication authentication,
                                                   @PathVariable Long itineraryId,
                                                   @RequestParam boolean favorite) {
        return travelPlannerService.updateFavorite(currentUserId(authentication), itineraryId, favorite);
    }

    @DeleteMapping("/itineraries/{itineraryId}")
    public void deleteItinerary(Authentication authentication,
                                @PathVariable Long itineraryId) {
        travelPlannerService.deleteItinerary(currentUserId(authentication), itineraryId);
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new IllegalStateException("Unauthorized");
        }
        return userId;
    }
}
