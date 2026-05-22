package org.example.demo.travel.agent;

import org.example.demo.model.TravelPlanRequest;
import org.example.demo.model.TravelPlanResponse;

import java.time.LocalDate;
import java.util.List;

public interface PlanCoordinatorAgent {

    PlanDraft coordinate(String sessionId,
                         TravelPlanRequest request,
                         LocalDate startDate,
                         int days,
                         List<TravelPlanResponse.SightRecommendation> attractions,
                         List<TravelPlanResponse.HotelRecommendation> hotels,
                         List<TravelPlanResponse.WeatherSnapshot> weather,
                         String revisionMessage);

    default PlanDraft coordinate(TravelPlanRequest request,
                                 LocalDate startDate,
                                 int days,
                                 List<TravelPlanResponse.SightRecommendation> attractions,
                                 List<TravelPlanResponse.HotelRecommendation> hotels,
                                 List<TravelPlanResponse.WeatherSnapshot> weather) {
        return coordinate("preview-" + request.destination(), request, startDate, days, attractions, hotels, weather, null);
    }

    record PlanDraft(
            String overview,
            String stayArea,
            String transportAdvice,
            List<TravelPlanResponse.DailyPlan> itinerary,
            List<TravelPlanResponse.MapPoint> mapPoints,
            List<TravelPlanResponse.RouteDay> routeDays,
            List<String> travelTips
    ) {
    }
}
