package org.example.demo.service;

import org.example.demo.model.ItinerarySummaryResponse;
import org.example.demo.model.PageResult;
import org.example.demo.model.StoredTravelPlanResponse;
import org.example.demo.model.TravelPlanRequest;
import org.example.demo.model.TravelPlanResponse;
import org.example.demo.model.TravelPlanRevisionRequest;
import org.example.demo.model.TravelPlanningOptionsResponse;

public interface TravelPlannerService {

    TravelPlanningOptionsResponse options();

    TravelPlanResponse createPlan(TravelPlanRequest request);

    StoredTravelPlanResponse createItinerary(Long userId, TravelPlanRequest request);

    PageResult<ItinerarySummaryResponse> listItineraries(Long userId,
                                                         String keyword,
                                                         String destination,
                                                         Boolean favoriteOnly,
                                                         String startDate,
                                                         String endDate,
                                                         long pageNo,
                                                         long pageSize);

    StoredTravelPlanResponse getItinerary(Long userId, Long itineraryId);

    StoredTravelPlanResponse reviseItinerary(Long userId, Long itineraryId, TravelPlanRevisionRequest request);

    StoredTravelPlanResponse updateFavorite(Long userId, Long itineraryId, boolean favorite);

    void deleteItinerary(Long userId, Long itineraryId);
}
