package org.example.demo.service.support;

import org.example.demo.model.TravelPlanRequest;
import org.example.demo.model.TravelPlanResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class TravelBudgetCalculator {

    public TravelPlanResponse.BudgetSummary calculate(TravelPlanRequest request,
                                                      TravelPlanResponse.TripSummary summary,
                                                      List<TravelPlanResponse.SightRecommendation> attractions,
                                                      List<TravelPlanResponse.HotelRecommendation> hotels,
                                                      List<TravelPlanResponse.RouteDay> routeDays) {
        int travelerCount = Math.max(request.adults() + request.children(), 1);
        int ticketAmount = estimateTickets(request, attractions);
        int hotelAmount = estimateHotel(request, summary.nights(), hotels);
        int foodAmount = estimateFood(request, summary.days());
        int transportAmount = estimateTransport(request, summary.days(), routeDays);

        List<TravelPlanResponse.BudgetItem> items = List.of(
                buildTicketItem(request, attractions, ticketAmount),
                buildHotelItem(request, summary.nights(), hotelAmount),
                buildFoodItem(request, summary.days(), foodAmount),
                buildTransportItem(request, summary.days(), routeDays, transportAmount)
        );

        int totalAmount = items.stream().mapToInt(TravelPlanResponse.BudgetItem::amount).sum();
        List<String> notes = List.of(
                "交通预算基于高德真实路线距离与时长换算，不代表实时打车账单。",
                "酒店预算根据预算档位、住宿风格和晚数估算，酒店 POI 来自高德但不含实时报价。",
                "景点门票因高德 POI 不含票价字段，按景点分类和名称规则估算。",
                "餐饮预算按人数、天数与预算档位估算，仅供行前参考。"
        );

        return new TravelPlanResponse.BudgetSummary(
                "CNY",
                "MIXED_REAL_INPUT_AND_ESTIMATION",
                travelerCount,
                summary.days(),
                summary.nights(),
                items,
                totalAmount,
                notes
        );
    }

    private TravelPlanResponse.BudgetItem buildTicketItem(TravelPlanRequest request,
                                                          List<TravelPlanResponse.SightRecommendation> attractions,
                                                          int amount) {
        Map<String, Integer> categoryCounts = new LinkedHashMap<>();
        for (TravelPlanResponse.SightRecommendation attraction : attractions) {
            categoryCounts.merge(normalizeCategory(attraction.category()), 1, Integer::sum);
        }

        String categorySummary = categoryCounts.isEmpty()
                ? "未获取到有效景点分类"
                : categoryCounts.entrySet().stream()
                .map(entry -> entry.getKey() + entry.getValue() + "个")
                .reduce((left, right) -> left + "，" + right)
                .orElse("未获取到有效景点分类");

        return new TravelPlanResponse.BudgetItem(
                "ticket",
                "门票",
                amount,
                "RULE_ESTIMATED",
                "LOW",
                "根据景点分类、名称关键词和预算档位估算门票费用。",
                List.of(
                        new TravelPlanResponse.BudgetDetail("景点数量", attractions.size() + "个", "REAL_AMAP"),
                        new TravelPlanResponse.BudgetDetail("分类构成", categorySummary, "REAL_AMAP"),
                        new TravelPlanResponse.BudgetDetail("预算档位倍率", formatBudgetMultiplier(request.budgetLevel()), "RULE")
                )
        );
    }

    private TravelPlanResponse.BudgetItem buildHotelItem(TravelPlanRequest request,
                                                         int nights,
                                                         int amount) {
        return new TravelPlanResponse.BudgetItem(
                "hotel",
                "酒店",
                amount,
                "RULE_ESTIMATED",
                "LOW",
                "根据预算档位、住宿风格和住宿晚数估算酒店费用。",
                List.of(
                        new TravelPlanResponse.BudgetDetail("住宿晚数", nights + "晚", "DERIVED"),
                        new TravelPlanResponse.BudgetDetail("住宿风格", request.hotelStyle(), "REQUEST"),
                        new TravelPlanResponse.BudgetDetail("预算档位", request.budgetLevel(), "REQUEST")
                )
        );
    }

    private TravelPlanResponse.BudgetItem buildFoodItem(TravelPlanRequest request,
                                                        int days,
                                                        int amount) {
        double effectiveTravelers = request.adults() + request.children() * 0.6d;
        return new TravelPlanResponse.BudgetItem(
                "food",
                "餐饮",
                amount,
                "RULE_ESTIMATED",
                "LOW",
                "根据出行人数、天数与预算档位估算餐饮费用。",
                List.of(
                        new TravelPlanResponse.BudgetDetail("出行人数", request.adults() + "成人，" + request.children() + "儿童", "REQUEST"),
                        new TravelPlanResponse.BudgetDetail("折算人数", formatDecimal(effectiveTravelers) + "人", "DERIVED"),
                        new TravelPlanResponse.BudgetDetail("行程天数", days + "天", "DERIVED")
                )
        );
    }

    private TravelPlanResponse.BudgetItem buildTransportItem(TravelPlanRequest request,
                                                             int days,
                                                             List<TravelPlanResponse.RouteDay> routeDays,
                                                             int amount) {
        double totalMeters = routeDays.stream()
                .flatMap(routeDay -> routeDay.segments().stream())
                .mapToDouble(TravelPlanResponse.RouteSegment::distanceMeters)
                .sum();
        double totalSeconds = routeDays.stream()
                .flatMap(routeDay -> routeDay.segments().stream())
                .mapToDouble(TravelPlanResponse.RouteSegment::durationSeconds)
                .sum();
        double totalKm = totalMeters / 1000d;

        return new TravelPlanResponse.BudgetItem(
                "transport",
                "交通",
                amount,
                "MIXED",
                "MEDIUM",
                "基于高德真实路线距离与时长，结合预算档位换算市内机动交通费用。",
                List.of(
                        new TravelPlanResponse.BudgetDetail("路线天数", routeDays.size() + "天", "REAL_AMAP"),
                        new TravelPlanResponse.BudgetDetail("路线总里程", formatDecimal(totalKm) + "km", "REAL_AMAP"),
                        new TravelPlanResponse.BudgetDetail("路线总时长", formatDecimal(totalSeconds / 3600d) + "小时", "REAL_AMAP"),
                        new TravelPlanResponse.BudgetDetail("估算单公里成本", "¥" + formatDecimal(transportRate(request.budgetLevel())) + "/km", "RULE"),
                        new TravelPlanResponse.BudgetDetail("每日最低预算", "¥" + transportDailyFloor(request.budgetLevel()) + "/天", "RULE"),
                        new TravelPlanResponse.BudgetDetail("行程天数", days + "天", "DERIVED")
                )
        );
    }

    private int estimateTickets(TravelPlanRequest request, List<TravelPlanResponse.SightRecommendation> attractions) {
        if (attractions == null || attractions.isEmpty()) {
            return 0;
        }
        double multiplier = budgetMultiplier(request.budgetLevel());
        double total = 0d;
        for (TravelPlanResponse.SightRecommendation attraction : attractions) {
            int base = baseTicketPrice(normalizeCategory(attraction.category()));
            int adjustment = ticketAdjustmentByName(attraction.name());
            int amount = Math.max(base + adjustment, 0);
            total += amount * multiplier;
        }
        return (int) Math.round(total);
    }

    private int estimateHotel(TravelPlanRequest request,
                              int nights,
                              List<TravelPlanResponse.HotelRecommendation> hotels) {
        if (nights <= 0) {
            return 0;
        }
        int baseRate = switch (normalizeBudgetLevel(request.budgetLevel())) {
            case "轻松省心" -> 280;
            case "品质升级" -> 880;
            default -> 480;
        };
        int styleAdjustment = switch (safeString(request.hotelStyle())) {
            case "市区便利" -> 50;
            case "度假放松" -> 120;
            case "精品设计" -> 100;
            case "亲子友好" -> 80;
            default -> 0;
        };
        int hotelCountAdjustment = hotels != null && hotels.size() >= 3 ? 20 : 0;
        return (baseRate + styleAdjustment + hotelCountAdjustment) * nights;
    }

    private int estimateFood(TravelPlanRequest request, int days) {
        int dailyFoodPerPerson = switch (normalizeBudgetLevel(request.budgetLevel())) {
            case "轻松省心" -> 120;
            case "品质升级" -> 320;
            default -> 200;
        };
        double effectiveTravelers = request.adults() + request.children() * 0.6d;
        return (int) Math.round(dailyFoodPerPerson * days * Math.max(effectiveTravelers, 1d));
    }

    private int estimateTransport(TravelPlanRequest request,
                                  int days,
                                  List<TravelPlanResponse.RouteDay> routeDays) {
        double totalMeters = routeDays == null ? 0d : routeDays.stream()
                .flatMap(routeDay -> routeDay.segments().stream())
                .mapToDouble(TravelPlanResponse.RouteSegment::distanceMeters)
                .sum();
        double totalKm = totalMeters / 1000d;
        double distanceBased = totalKm * transportRate(request.budgetLevel());
        int floor = transportDailyFloor(request.budgetLevel()) * Math.max(days, 1);
        return (int) Math.round(Math.max(distanceBased, floor));
    }

    private double transportRate(String budgetLevel) {
        return switch (normalizeBudgetLevel(budgetLevel)) {
            case "轻松省心" -> 1.3d;
            case "品质升级" -> 3.0d;
            default -> 2.0d;
        };
    }

    private int transportDailyFloor(String budgetLevel) {
        return switch (normalizeBudgetLevel(budgetLevel)) {
            case "轻松省心" -> 50;
            case "品质升级" -> 160;
            default -> 90;
        };
    }

    private double budgetMultiplier(String budgetLevel) {
        return switch (normalizeBudgetLevel(budgetLevel)) {
            case "轻松省心" -> 0.85d;
            case "品质升级" -> 1.2d;
            default -> 1.0d;
        };
    }

    private String formatBudgetMultiplier(String budgetLevel) {
        return "x" + formatDecimal(budgetMultiplier(budgetLevel));
    }

    private String normalizeBudgetLevel(String budgetLevel) {
        String normalized = safeString(budgetLevel);
        if (normalized.contains("轻松")) {
            return "轻松省心";
        }
        if (normalized.contains("品质") || normalized.contains("豪华") || normalized.contains("奢华")) {
            return "品质升级";
        }
        return "均衡舒适";
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "景点";
        }
        String normalized = category.toLowerCase(Locale.ROOT);
        if (normalized.contains("food") || normalized.contains("美食")) {
            return "美食";
        }
        if (normalized.contains("night") || normalized.contains("夜")) {
            return "夜游";
        }
        if (normalized.contains("history") || normalized.contains("历史")) {
            return "历史";
        }
        if (normalized.contains("nature") || normalized.contains("自然")) {
            return "自然";
        }
        return "景点";
    }

    private int baseTicketPrice(String category) {
        return switch (category) {
            case "自然" -> 60;
            case "历史" -> 80;
            case "夜游" -> 40;
            case "美食" -> 0;
            default -> 70;
        };
    }

    private int ticketAdjustmentByName(String name) {
        String value = safeString(name);
        int adjustment = 0;
        if (containsAny(value, "博物馆", "古镇", "寺", "遗址")) {
            adjustment += 20;
        }
        if (containsAny(value, "公园", "湿地")) {
            adjustment -= 10;
        }
        if (containsAny(value, "乐园", "演艺", "索道")) {
            adjustment += 80;
        }
        return adjustment;
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }

    private String formatDecimal(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.05d) {
            return String.valueOf((int) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
