package org.example.demo.travel.agent;

import org.example.demo.model.TravelPlanRequest;
import org.example.demo.model.TravelPlanResponse;

import java.time.LocalDate;
import java.util.List;

public interface WeatherQueryAgent {

    List<TravelPlanResponse.WeatherSnapshot> forecast(TravelPlanRequest request, LocalDate startDate, LocalDate endDate);
}
