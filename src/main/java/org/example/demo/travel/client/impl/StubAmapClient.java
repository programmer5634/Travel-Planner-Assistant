package org.example.demo.travel.client.impl;

import org.example.demo.travel.client.AmapClient;
import org.example.demo.travel.gateway.MapGateway;
import org.example.demo.travel.gateway.impl.MapGatewayImpl;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class StubAmapClient implements AmapClient {

    private final MapGatewayImpl delegate;

    public StubAmapClient(MapGatewayImpl delegate) {
        this.delegate = delegate;
    }

    @Override
    public MapGateway.DestinationProfile resolveDestination(String destination) {
        return delegate.resolveDestination(destination);
    }

    @Override
    public List<MapGateway.AttractionProfile> searchAttractions(String destination, List<String> interests, int limit) {
        return delegate.searchAttractions(destination, interests, limit);
    }

    @Override
    public List<MapGateway.HotelProfile> searchHotels(String destination, String hotelStyle, String budgetLevel, int limit) {
        return delegate.searchHotels(destination, hotelStyle, budgetLevel, limit);
    }

    @Override
    public List<MapGateway.WeatherProfile> forecast(String destination, LocalDate startDate, LocalDate endDate) {
        return delegate.forecast(destination, startDate, endDate);
    }

    @Override
    public TravelRoute planDrivingRoute(List<RouteStop> stops) {
        List<org.example.demo.model.TravelPlanResponse.RouteCoordinate> polyline = stops.stream()
                .map(stop -> new org.example.demo.model.TravelPlanResponse.RouteCoordinate(stop.latitude(), stop.longitude()))
                .toList();
        return new TravelRoute(0, 0, polyline);
    }
}
