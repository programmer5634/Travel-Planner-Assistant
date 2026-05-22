package org.example.demo.model;

import java.util.List;

public record TravelPlanResponse(
        String title,
        String overview,
        TripSummary summary,
        List<DailyPlan> itinerary,
        List<SightRecommendation> attractions,
        List<HotelRecommendation> hotels,
        List<WeatherSnapshot> weather,
        List<MapPoint> mapPoints,
        List<RouteDay> routeDays,
        BudgetSummary budget,
        List<String> travelTips
) {

    public record TripSummary(
            int days,
            int nights,
            String budgetLevel,
            String pace,
            String stayArea,
            String transportAdvice
    ) {
    }

    public record DailyPlan(
            int day,
            String date,
            String theme,
            List<String> agenda,
            String foodSuggestion,
            String eveningSuggestion
    ) {
    }

    public record SightRecommendation(
            String name,
            String category,
            String district,
            String address,
            String highlight,
            String recommendedDuration,
            double latitude,
            double longitude,
            String imageUrl
    ) {
    }

    public record HotelRecommendation(
            String name,
            String district,
            String address,
            String style,
            String priceBand,
            String highlight,
            String imageUrl,
            double latitude,
            double longitude
    ) {
    }

    public record WeatherSnapshot(
            String date,
            String condition,
            String temperature,
            String advice
    ) {
    }

    public record MapPoint(
            int day,
            int sequence,
            String name,
            String type,
            String district,
            String address,
            String description,
            double latitude,
            double longitude,
            String imageUrl
    ) {
    }

    public record RouteDay(
            int day,
            String mode,
            List<RouteSegment> segments
    ) {
    }

    public record RouteSegment(
            int sequence,
            String fromName,
            String toName,
            double distanceMeters,
            double durationSeconds,
            List<RouteCoordinate> polyline
    ) {
    }

    public record RouteCoordinate(
            double latitude,
            double longitude
    ) {
    }

    public record BudgetSummary(
            String currency,
            String pricingMode,
            int travelerCount,
            int days,
            int nights,
            List<BudgetItem> items,
            int totalAmount,
            List<String> notes
    ) {
    }

    public record BudgetItem(
            String code,
            String label,
            int amount,
            String basis,
            String estimateLevel,
            String formulaDescription,
            List<BudgetDetail> details
    ) {
    }

    public record BudgetDetail(
            String label,
            String value,
            String sourceType
    ) {
    }
}
