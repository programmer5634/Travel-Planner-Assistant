package org.example.demo.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.demo.config.TravelPlannerProperties;
import org.example.demo.mapper.ItineraryMapper;
import org.example.demo.mapper.ItineraryRevisionMapper;
import org.example.demo.mapper.TripSessionMapper;
import org.example.demo.model.TravelPlanRequest;
import org.example.demo.model.TravelPlanResponse;
import org.example.demo.model.TravelPlanningOptionsResponse;
import org.example.demo.service.support.TravelBudgetCalculator;
import org.example.demo.service.support.TravelPlanAssembler;
import org.example.demo.travel.agent.AttractionSearchAgent;
import org.example.demo.travel.agent.HotelRecommendationAgent;
import org.example.demo.travel.agent.PlanCoordinatorAgent;
import org.example.demo.travel.agent.WeatherQueryAgent;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TravelPlannerServiceImplTest {

    @Test
    void optionsExposeConfiguredChoices() {
        TravelPlannerServiceImpl service = service(
                mock(AttractionSearchAgent.class),
                mock(WeatherQueryAgent.class),
                mock(HotelRecommendationAgent.class),
                mock(PlanCoordinatorAgent.class)
        );

        TravelPlanningOptionsResponse response = service.options();

        assertEquals(List.of("杭州", "成都"), response.featuredDestinations());
        assertEquals(List.of("均衡舒适", "品质升级"), response.budgetLevels());
    }

    @Test
    void createPlanCoordinatesAgentsIntoStructuredResponse() {
        AttractionSearchAgent attractionSearchAgent = mock(AttractionSearchAgent.class);
        WeatherQueryAgent weatherQueryAgent = mock(WeatherQueryAgent.class);
        HotelRecommendationAgent hotelRecommendationAgent = mock(HotelRecommendationAgent.class);
        PlanCoordinatorAgent planCoordinatorAgent = mock(PlanCoordinatorAgent.class);

        when(attractionSearchAgent.search(any(), anyInt())).thenReturn(List.of(
                new TravelPlanResponse.SightRecommendation("西湖", "自然", "西湖区", "湖滨路", "经典湖景", "2小时", 30.25, 120.15, null)
        ));
        when(weatherQueryAgent.forecast(any(), any(), any())).thenReturn(List.of(
                new TravelPlanResponse.WeatherSnapshot("2026-06-01", "晴到多云", "18-26C", "带一件薄外套")
        ));
        when(hotelRecommendationAgent.recommend(any())).thenReturn(List.of(
                new TravelPlanResponse.HotelRecommendation("西湖湖景酒店", "西湖区", "湖滨路", "市区便利", "均衡舒适", "步行友好", "https://example.com", 30.26, 120.16)
        ));
        when(planCoordinatorAgent.coordinate(any(), any(), any(), anyInt(), any(), any(), any(), any())).thenReturn(new PlanCoordinatorAgent.PlanDraft(
                "整体围绕西湖展开",
                "西湖 / 湖滨",
                "优先地铁，必要时短途打车",
                List.of(new TravelPlanResponse.DailyPlan(
                        1,
                        "2026-06-01",
                        "第1天：自然风光",
                        List.of("游览西湖"),
                        "午餐可试试本地面馆",
                        "傍晚去湖边散步"
                )),
                List.of(new TravelPlanResponse.MapPoint(1, 1, "西湖", "景点", "西湖区", "湖滨路", "经典湖景", 30.25, 120.15, null)),
                List.of(new TravelPlanResponse.RouteDay(
                        1,
                        "driving",
                        List.of(new TravelPlanResponse.RouteSegment(1, "酒店", "西湖", 12_000, 1800, List.of()))
                )),
                List.of("建议住在核心区域附近")
        ));

        TravelPlannerServiceImpl service = service(
                attractionSearchAgent,
                weatherQueryAgent,
                hotelRecommendationAgent,
                planCoordinatorAgent
        );

        TravelPlanResponse response = service.createPlan(new TravelPlanRequest(
                "上海",
                "杭州",
                "2026-06-01",
                "2026-06-03",
                2,
                0,
                "均衡舒适",
                "经典平衡",
                "市区便利",
                List.of("自然风光", "美食打卡"),
                "希望安排一个日落机位"
        ));

        assertEquals("杭州智能行程方案", response.title());
        assertEquals(3, response.summary().days());
        assertEquals(1, response.attractions().size());
        assertEquals("西湖 / 湖滨", response.summary().stayArea());
        assertEquals(1, response.mapPoints().get(0).day());
        assertEquals(1, response.mapPoints().get(0).sequence());
        assertNotNull(response.budget());
        assertEquals(4, response.budget().items().size());
    }

    @Test
    void createPlanRejectsInvalidDateRange() {
        TravelPlannerServiceImpl service = service(
                mock(AttractionSearchAgent.class),
                mock(WeatherQueryAgent.class),
                mock(HotelRecommendationAgent.class),
                mock(PlanCoordinatorAgent.class)
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.createPlan(new TravelPlanRequest(
                "上海",
                "杭州",
                "2026-06-03",
                "2026-06-01",
                2,
                0,
                "均衡舒适",
                "经典平衡",
                "市区便利",
                List.of("自然风光"),
                null
        )));

        assertEquals("endDate must be on or after startDate", exception.getMessage());
    }

    private TravelPlannerServiceImpl service(AttractionSearchAgent attractionSearchAgent,
                                             WeatherQueryAgent weatherQueryAgent,
                                             HotelRecommendationAgent hotelRecommendationAgent,
                                             PlanCoordinatorAgent planCoordinatorAgent) {
        return new TravelPlannerServiceImpl(
                properties(),
                attractionSearchAgent,
                weatherQueryAgent,
                hotelRecommendationAgent,
                planCoordinatorAgent,
                mock(TripSessionMapper.class),
                mock(ItineraryMapper.class),
                mock(ItineraryRevisionMapper.class),
                new TravelPlanAssembler(new ObjectMapper()),
                new TravelBudgetCalculator(),
                new SyncTaskExecutor()
        );
    }

    private TravelPlannerProperties properties() {
        TravelPlannerProperties properties = new TravelPlannerProperties();
        properties.setFeaturedDestinations(List.of("杭州", "成都"));
        properties.setInterests(List.of("自然风光", "美食打卡"));
        properties.setBudgetLevels(List.of("均衡舒适", "品质升级"));
        properties.setPaceOptions(List.of("经典平衡", "轻松慢游"));
        properties.setHotelStyles(List.of("市区便利", "亲子友好"));
        return properties;
    }
}
