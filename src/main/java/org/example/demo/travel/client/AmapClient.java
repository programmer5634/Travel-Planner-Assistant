package org.example.demo.travel.client;

import org.example.demo.travel.gateway.MapGateway;

import java.time.LocalDate;
import java.util.List;

public interface AmapClient {

    MapGateway.DestinationProfile resolveDestination(String destination);

    List<MapGateway.AttractionProfile> searchAttractions(String destination, List<String> interests, int limit);

    List<MapGateway.HotelProfile> searchHotels(String destination, String hotelStyle, String budgetLevel, int limit);

    List<MapGateway.WeatherProfile> forecast(String destination, LocalDate startDate, LocalDate endDate);

    TravelRoute planDrivingRoute(List<RouteStop> stops);

    record RouteStop(
            String name,
            double latitude,
            double longitude
    ) {
    }

    record TravelRoute(
            double distanceMeters,
            double durationSeconds,
            List<org.example.demo.model.TravelPlanResponse.RouteCoordinate> polyline
    ) {
    }
}
