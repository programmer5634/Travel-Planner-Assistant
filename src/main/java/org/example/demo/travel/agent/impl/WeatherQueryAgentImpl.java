package org.example.demo.travel.agent.impl;

import org.example.demo.travel.agent.WeatherQueryAgent;
import org.example.demo.travel.client.AmapClient;
import org.example.demo.model.TravelPlanRequest;
import org.example.demo.model.TravelPlanResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class WeatherQueryAgentImpl implements WeatherQueryAgent {

    private final AmapClient amapClient;

    public WeatherQueryAgentImpl(AmapClient amapClient) {
        this.amapClient = amapClient;
    }

    @Override
    public List<TravelPlanResponse.WeatherSnapshot> forecast(TravelPlanRequest request, LocalDate startDate, LocalDate endDate) {
        return amapClient.forecast(request.destination(), startDate, endDate).stream()
                .map(item -> new TravelPlanResponse.WeatherSnapshot(
                        item.date(),
                        item.condition(),
                        item.temperature(),
                        item.advice()
                ))
                .toList();
    }
}
