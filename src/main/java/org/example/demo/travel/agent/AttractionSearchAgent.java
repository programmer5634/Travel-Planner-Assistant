package org.example.demo.travel.agent;

import org.example.demo.model.TravelPlanRequest;
import org.example.demo.model.TravelPlanResponse;

import java.util.List;

public interface AttractionSearchAgent {

    List<TravelPlanResponse.SightRecommendation> search(TravelPlanRequest request, int days);
}
