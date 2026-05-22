package org.example.demo.service.support;

import org.example.demo.model.TravelPlanRequest;
import org.example.demo.model.TravelPlanResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TravelBudgetCalculatorTest {

    private final TravelBudgetCalculator calculator = new TravelBudgetCalculator();

    @Test
    void calculateUsesAmapRouteDistanceForTransportFloorComparison() {
        TravelPlanRequest request = request("均衡舒适", "市区便利", 2, 0);
        TravelPlanResponse.TripSummary summary = new TravelPlanResponse.TripSummary(3, 2, "均衡舒适", "经典平衡", "西湖", "优先地铁");
        List<TravelPlanResponse.RouteDay> routeDays = List.of(
                new TravelPlanResponse.RouteDay(1, "driving", List.of(
                        new TravelPlanResponse.RouteSegment(1, "酒店", "西湖", 100_000, 7_200, List.of())
                ))
        );

        TravelPlanResponse.BudgetSummary budget = calculator.calculate(
                request,
                summary,
                List.of(new TravelPlanResponse.SightRecommendation("西湖", "自然", "西湖区", "西湖景区", "经典湖景", "2小时", 30.25, 120.15, null)),
                List.of(new TravelPlanResponse.HotelRecommendation("湖滨酒店", "西湖区", "湖滨路", "市区便利", "均衡舒适", "步行友好", "", 30.26, 120.16)),
                routeDays
        );

        TravelPlanResponse.BudgetItem transport = budget.items().stream()
                .filter(item -> "transport".equals(item.code()))
                .findFirst()
                .orElseThrow();

        assertEquals(200, transport.amount());
        assertEquals("MIXED", transport.basis());
        assertEquals("100km", transport.details().stream()
                .filter(detail -> "路线总里程".equals(detail.label()))
                .findFirst()
                .orElseThrow()
                .value());
    }

    @Test
    void calculateFallsBackToDailyTransportFloorWhenRouteMissing() {
        TravelPlanRequest request = request("轻松省心", "度假放松", 2, 1);
        TravelPlanResponse.TripSummary summary = new TravelPlanResponse.TripSummary(2, 1, "轻松省心", "轻松慢游", "亚龙湾", "打车为主");

        TravelPlanResponse.BudgetSummary budget = calculator.calculate(
                request,
                summary,
                List.of(),
                List.of(),
                List.of()
        );

        TravelPlanResponse.BudgetItem transport = budget.items().stream()
                .filter(item -> "transport".equals(item.code()))
                .findFirst()
                .orElseThrow();

        assertEquals(100, transport.amount());
    }

    @Test
    void calculateTreatsFoodCategoryAsZeroTicketBase() {
        TravelPlanRequest request = request("均衡舒适", "市区便利", 2, 0);
        TravelPlanResponse.TripSummary summary = new TravelPlanResponse.TripSummary(1, 0, "均衡舒适", "经典平衡", "春熙路", "步行为主");

        TravelPlanResponse.BudgetSummary budget = calculator.calculate(
                request,
                summary,
                List.of(
                        new TravelPlanResponse.SightRecommendation("锦里", "Food", "武侯区", "锦里街区", "小吃集中", "1.5小时", 30.64, 104.05, null),
                        new TravelPlanResponse.SightRecommendation("博物馆", "History", "青羊区", "博物馆路", "历史文物", "2小时", 30.66, 104.06, null)
                ),
                List.of(),
                List.of()
        );

        TravelPlanResponse.BudgetItem ticket = budget.items().stream()
                .filter(item -> "ticket".equals(item.code()))
                .findFirst()
                .orElseThrow();

        assertEquals(100, ticket.amount());
    }

    private TravelPlanRequest request(String budgetLevel, String hotelStyle, int adults, int children) {
        return new TravelPlanRequest(
                "上海",
                "杭州",
                "2026-06-01",
                "2026-06-03",
                adults,
                children,
                budgetLevel,
                "经典平衡",
                hotelStyle,
                List.of("自然风光", "美食打卡"),
                null
        );
    }
}
