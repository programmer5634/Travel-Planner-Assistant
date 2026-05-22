package org.example.demo.travel.agent.impl;

import org.example.demo.travel.agent.AttractionSearchAgent;
import org.example.demo.travel.client.AmapClient;
import org.example.demo.travel.client.UnsplashClient;
import org.example.demo.model.TravelPlanRequest;
import org.example.demo.model.TravelPlanResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AttractionSearchAgentImpl implements AttractionSearchAgent {

    private static final Logger log = LoggerFactory.getLogger(AttractionSearchAgentImpl.class);

    private final AmapClient amapClient;
    private final UnsplashClient unsplashClient;

    public AttractionSearchAgentImpl(AmapClient amapClient, UnsplashClient unsplashClient) {
        this.amapClient = amapClient;
        this.unsplashClient = unsplashClient;
    }

    @Override
    public List<TravelPlanResponse.SightRecommendation> search(TravelPlanRequest request, int days) {
        int limit = Math.max(days * 2, 4);
        return amapClient.searchAttractions(request.destination(), request.interests(), limit).stream()
                .map(attraction -> new TravelPlanResponse.SightRecommendation(
                        attraction.name(),
                        attraction.category(),
                        attraction.district(),
                        attraction.address(),
                        attraction.highlight(),
                        attraction.recommendedDuration(),
                        attraction.latitude(),
                        attraction.longitude(),
                        resolveImage(request.destination(), attraction.name())
                ))
                .toList();
    }

    private String resolveImage(String destination, String sightName) {
        try {
            return unsplashClient.resolveSightImage(destination, sightName);
        } catch (Exception exception) {
            log.warn("景点图片解析失败，跳过：{} / {} — {}", destination, sightName, exception.getMessage());
            return null;
        }
    }
}
