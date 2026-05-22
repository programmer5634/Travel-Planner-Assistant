package org.example.demo.travel.gateway;

import java.time.LocalDate;
import java.util.List;

public interface MapGateway {

    DestinationProfile resolveDestination(String destination);

    List<AttractionProfile> searchAttractions(String destination, List<String> interests, int limit);

    List<HotelProfile> searchHotels(String destination, String hotelStyle, String budgetLevel, int limit);

    List<WeatherProfile> forecast(String destination, LocalDate startDate, LocalDate endDate);

    record DestinationProfile(
            String city,
            String summary,
            String bestArea,
            String transportAdvice
    ) {
    }

    record AttractionProfile(
            String name,
            String category,
            String district,
            String address,
            String highlight,
            String recommendedDuration,
            double latitude,
            double longitude,
            List<String> tags
    ) {
    }

    record HotelProfile(
            String name,
            String district,
            String address,
            String style,
            String priceBand,
            String highlight,
            double latitude,
            double longitude,
            String imageUrl
    ) {
    }

    record WeatherProfile(
            String date,
            String condition,
            String temperature,
            String advice
    ) {
    }
}
