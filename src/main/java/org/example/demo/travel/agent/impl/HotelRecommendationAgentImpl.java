package org.example.demo.travel.agent.impl;

import org.example.demo.travel.agent.HotelRecommendationAgent;
import org.example.demo.travel.client.AmapClient;
import org.example.demo.travel.client.UnsplashClient;
import org.example.demo.travel.gateway.MapGateway;
import org.example.demo.model.TravelPlanRequest;
import org.example.demo.model.TravelPlanResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HotelRecommendationAgentImpl implements HotelRecommendationAgent {

    private static final Logger log = LoggerFactory.getLogger(HotelRecommendationAgentImpl.class);

    private final AmapClient amapClient;
    private final UnsplashClient unsplashClient;

    public HotelRecommendationAgentImpl(AmapClient amapClient, UnsplashClient unsplashClient) {
        this.amapClient = amapClient;
        this.unsplashClient = unsplashClient;
    }

    @Override
    public List<TravelPlanResponse.HotelRecommendation> recommend(TravelPlanRequest request) {
        return amapClient.searchHotels(request.destination(), request.hotelStyle(), request.budgetLevel(), 3).stream()
                .map(hotel -> new TravelPlanResponse.HotelRecommendation(
                        hotel.name(),
                        hotel.district(),
                        hotel.address(),
                        hotel.style(),
                        hotel.priceBand(),
                        hotel.highlight(),
                        resolveImage(request, hotel),
                        hotel.latitude(),
                        hotel.longitude()
                ))
                .toList();
    }

    private String resolveImage(TravelPlanRequest request, MapGateway.HotelProfile hotel) {
        String amapUrl = hotel.imageUrl();
        if (amapUrl != null && !amapUrl.isBlank()) {
            return amapUrl;
        }
        try {
            return unsplashClient.resolveHotelImage(request.destination(), hotel.name(), hotel.style());
        } catch (RuntimeException e) {
            log.warn("酒店图片获取失败，使用空值: {} - {}", hotel.name(), e.getMessage());
            return "";
        }
    }
}
