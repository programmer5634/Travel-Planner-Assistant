package org.example.demo.travel.tool;

import dev.langchain4j.agent.tool.Tool;
import org.example.demo.travel.client.AmapClient;
import org.example.demo.travel.client.UnsplashClient;
import org.example.demo.travel.gateway.MapGateway;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class TravelPlanningTools {

    private final AmapClient amapClient;
    private final UnsplashClient unsplashClient;

    public TravelPlanningTools(AmapClient amapClient, UnsplashClient unsplashClient) {
        this.amapClient = amapClient;
        this.unsplashClient = unsplashClient;
    }

    @Tool("Search destination profile and best stay area for a city")
    public MapGateway.DestinationProfile resolveDestination(String destination) {
        return amapClient.resolveDestination(destination);
    }

    @Tool("Search attraction recommendations for a destination and interests")
    public List<MapGateway.AttractionProfile> searchAttractions(String destination, List<String> interests, int limit) {
        return amapClient.searchAttractions(destination, interests, limit);
    }

    @Tool("Search hotel recommendations for a destination, style, and budget level")
    public List<MapGateway.HotelProfile> searchHotels(String destination, String hotelStyle, String budgetLevel, int limit) {
        return amapClient.searchHotels(destination, hotelStyle, budgetLevel, limit);
    }

    @Tool("Get forecast between start and end date for a destination")
    public List<MapGateway.WeatherProfile> forecast(String destination, String startDate, String endDate) {
        return amapClient.forecast(destination, LocalDate.parse(startDate), LocalDate.parse(endDate));
    }

    @Tool("Resolve a representative hotel image url for a specific hotel at a destination")
    public String resolveHotelImage(String destination, String hotelName, String hotelStyle) {
        return unsplashClient.resolveHotelImage(destination, hotelName, hotelStyle);
    }
}
