package org.example.demo.travel.agent;

import org.example.demo.model.TravelPlanRequest;
import org.example.demo.model.TravelPlanResponse;

import java.util.List;

public interface HotelRecommendationAgent {

    List<TravelPlanResponse.HotelRecommendation> recommend(TravelPlanRequest request);
}
