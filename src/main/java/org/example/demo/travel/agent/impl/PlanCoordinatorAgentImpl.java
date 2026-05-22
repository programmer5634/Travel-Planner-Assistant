package org.example.demo.travel.agent.impl;

import org.example.demo.model.TravelPlanRequest;
import org.example.demo.model.TravelPlanResponse;
import org.example.demo.travel.agent.PlanCoordinatorAgent;
import org.example.demo.travel.agent.coordinator.TravelCoordinatorAiService;
import org.example.demo.travel.client.AmapClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class PlanCoordinatorAgentImpl implements PlanCoordinatorAgent {

    private static final Logger log = LoggerFactory.getLogger(PlanCoordinatorAgentImpl.class);

    private final AmapClient amapClient;
    private final TravelCoordinatorAiService coordinatorAiService;
    private final String apiKey;

    public PlanCoordinatorAgentImpl(AmapClient amapClient,
                                    TravelCoordinatorAiService coordinatorAiService,
                                    @Value("${langchain4j.open-ai.chat-model.api-key:}") String apiKey) {
        this.amapClient = amapClient;
        this.coordinatorAiService = coordinatorAiService;
        this.apiKey = apiKey;
    }

    @Override
    public PlanDraft coordinate(String sessionId,
                                TravelPlanRequest request,
                                LocalDate startDate,
                                int days,
                                List<TravelPlanResponse.SightRecommendation> attractions,
                                List<TravelPlanResponse.HotelRecommendation> hotels,
                                List<TravelPlanResponse.WeatherSnapshot> weather,
                                String revisionMessage) {
        var destination = amapClient.resolveDestination(request.destination());
        String overviewPrompt = buildOverviewPrompt(request, days, destination.summary(), attractions, weather, revisionMessage);
        String llmOutput = callLlm(sessionId, overviewPrompt);

        String overview = extractOverview(llmOutput);
        List<TravelPlanResponse.DailyPlan> itinerary = parseItinerary(llmOutput, request, startDate, days, attractions);

        Map<Integer, Set<String>> dayAttractionNames = extractDayAttractionNames(itinerary, attractions);
        List<TravelPlanResponse.MapPoint> mapPoints = buildMapPoints(attractions, hotels, days, dayAttractionNames);
        List<TravelPlanResponse.RouteDay> routeDays = buildRouteDays(attractions, hotels, days, dayAttractionNames);
        List<String> travelTips = buildTravelTips(request, weather, destination);

        return new PlanDraft(
                overview,
                destination.bestArea(),
                destination.transportAdvice(),
                itinerary,
                mapPoints,
                routeDays,
                travelTips
        );
    }

    private String callLlm(String sessionId, String prompt) {
        if (apiKey == null || apiKey.isBlank() || "demo-key".equals(apiKey) || "dashscope-api-key".equals(apiKey)) {
            throw new IllegalStateException("未配置有效的大模型 API Key：langchain4j.open-ai.chat-model.api-key");
        }
        try {
            return coordinatorAiService.respond(sessionId, prompt);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("大模型行程生成失败", exception);
        }
    }

    private String buildOverviewPrompt(TravelPlanRequest request,
                                       int days,
                                       String destinationSummary,
                                       List<TravelPlanResponse.SightRecommendation> attractions,
                                       List<TravelPlanResponse.WeatherSnapshot> weather,
                                       String revisionMessage) {
        String weatherNote = weather.isEmpty() ? "暂未获取到天气信息。" : "";
        for (int i = 0; i < weather.size(); i++) {
            var w = weather.get(i);
            weatherNote += "第" + (i + 1) + "天天气：" + w.condition() + "，" + w.temperature() + "；";
        }
        String reviseNote = revisionMessage == null || revisionMessage.isBlank()
                ? ""
                : "\n用户调整要求：" + revisionMessage;
        String attractionNames = attractions.isEmpty() ? "暂无" :
                String.join("、", attractions.stream().limit(12).map(a -> a.name() + "(" + a.district() + ")").toList());

        return "你是一位资深旅行规划师。请为" + request.destination() + "设计一份" + days + "天的中文行程。\n"
                + "\n基本信息："
                + "\n- 出发城市：" + request.departureCity()
                + "\n- 行程天数：" + days + "天"
                + "\n- 预算偏好：" + request.budgetLevel()
                + "\n- 出行节奏：" + request.pace()
                + "\n- 住宿风格：" + request.hotelStyle()
                + "\n- 兴趣偏好：" + String.join("、", request.interests())
                + "\n- 备注：" + (request.notes() == null ? "无" : request.notes())
                + "\n- 可选景点：" + attractionNames
                + "\n- " + weatherNote
                + reviseNote
                + "\n\n请严格按以下格式输出，不要添加多余内容：\n"
                + "\n【概览】"
                + "\n（一段简洁的行程概览，2-4句话）"
                + "\n"
                + "\n【第1天】"
                + "\n主题：（当天主题，如\"西湖经典漫步\"）"
                + "\n行程：（上午的第一个行程安排，1-2句）"
                + "\n行程：（下午的行程安排，1-2句）"
                + "\n行程：（如还有安排，继续添加行程行）"
                + "\n美食：（当天餐饮建议，1-2句，要具体推荐菜系或餐厅类型）"
                + "\n夜晚：（当晚活动建议，1-2句）"
                + "\n"
                + "\n【第2天】"
                + "\n主题：..."
                + "\n行程：..."
                + "\n..."
                + "\n\n要求："
                + "\n- 每天必须有不同的主题和行程安排，不要重复"
                + "\n- 行程要具体到景点名称，不要泛泛而谈"
                + "\n- 美食建议每天不同，推荐不同的菜系或餐厅"
                + "\n- 夜晚活动每天不同"
                + "\n- 行程节奏符合「" + request.pace() + "」的偏好"
                + "\n- 第一天考虑抵达时间，最后一天考虑返程";
    }

    private String extractOverview(String llmOutput) {
        String[] lines = llmOutput.split("\n");
        StringBuilder overview = new StringBuilder();
        boolean inOverview = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("【概览】")) {
                inOverview = true;
                String rest = trimmed.substring(4).trim();
                if (!rest.isEmpty()) {
                    overview.append(rest);
                }
                continue;
            }
            if (trimmed.startsWith("【第")) {
                break;
            }
            if (inOverview && !trimmed.isEmpty()) {
                if (!overview.isEmpty()) {
                    overview.append(" ");
                }
                overview.append(trimmed);
            }
        }
        return overview.isEmpty() ? "这是一份为您精心规划的行程。" : overview.toString();
    }

    private List<TravelPlanResponse.DailyPlan> parseItinerary(String llmOutput,
                                                               TravelPlanRequest request,
                                                               LocalDate startDate,
                                                               int days,
                                                               List<TravelPlanResponse.SightRecommendation> attractions) {
        List<TravelPlanResponse.DailyPlan> result = new ArrayList<>();
        int currentDay = -1;
        String theme = "";
        List<String> agenda = new ArrayList<>();
        String food = "";
        String evening = "";

        for (String rawLine : llmOutput.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("【第") && line.contains("天】")) {
                if (currentDay > 0) {
                    result.add(buildDailyPlan(currentDay, startDate, theme, agenda, food, evening, request));
                }
                currentDay = extractDayNumber(line);
                theme = "";
                agenda = new ArrayList<>();
                food = "";
                evening = "";
                continue;
            }

            if (currentDay <= 0) continue;

            if (line.startsWith("主题：") || line.startsWith("主题:")) {
                theme = line.substring(line.indexOf('：') + 1).trim();
            } else if (line.startsWith("行程：") || line.startsWith("行程:")) {
                String item = line.substring(line.indexOf('：') + 1).trim();
                if (!item.isEmpty()) agenda.add(item);
            } else if (line.startsWith("美食：") || line.startsWith("美食:")) {
                food = line.substring(line.indexOf('：') + 1).trim();
            } else if (line.startsWith("夜晚：") || line.startsWith("夜晚:")) {
                evening = line.substring(line.indexOf('：') + 1).trim();
            }
        }

        if (currentDay > 0) {
            result.add(buildDailyPlan(currentDay, startDate, theme, agenda, food, evening, request));
        }

        if (result.isEmpty()) {
            log.warn("LLM 行程解析失败，降级到模板生成");
            return buildFallbackItinerary(request, startDate, days, attractions);
        }

        return result;
    }

    private int extractDayNumber(String line) {
        try {
            int start = line.indexOf('第') + 1;
            int end = line.indexOf('天');
            if (start > 0 && end > start) {
                return Integer.parseInt(line.substring(start, end));
            }
        } catch (NumberFormatException ignored) {
        }
        return -1;
    }

    private TravelPlanResponse.DailyPlan buildDailyPlan(int day,
                                                         LocalDate startDate,
                                                         String theme,
                                                         List<String> agenda,
                                                         String food,
                                                         String evening,
                                                         TravelPlanRequest request) {
        if (theme.isEmpty()) theme = "第" + day + "天行程";
        if (agenda.isEmpty()) agenda.add("根据当天时间灵活安排景点游览。");
        if (food.isEmpty()) food = fallbackFood(request.budgetLevel());
        if (evening.isEmpty()) evening = fallbackEvening(request.pace());

        return new TravelPlanResponse.DailyPlan(
                day,
                startDate.plusDays(day - 1).toString(),
                theme,
                List.copyOf(agenda),
                food,
                evening
        );
    }

    private List<TravelPlanResponse.DailyPlan> buildFallbackItinerary(TravelPlanRequest request,
                                                                       LocalDate startDate,
                                                                       int days,
                                                                       List<TravelPlanResponse.SightRecommendation> attractions) {
        List<TravelPlanResponse.DailyPlan> itinerary = new ArrayList<>();
        int cursor = 0;
        for (int day = 1; day <= days; day++) {
            List<String> agenda = new ArrayList<>();
            agenda.add(day == 1 ? "抵达后先在交通便利的核心区域办理入住，稍作休整再开始当天行程。" : "早餐后出发，行程中预留机动时间，避免赶路太紧。");

            for (int index = 0; index < 2 && cursor < attractions.size(); index++, cursor++) {
                TravelPlanResponse.SightRecommendation attraction = attractions.get(cursor);
                agenda.add("前往" + attraction.district() + "的" + attraction.name() + "，建议停留" + attraction.recommendedDuration() + "。");
            }

            if (agenda.size() == 1 && !attractions.isEmpty()) {
                TravelPlanResponse.SightRecommendation attraction = attractions.get((day - 1) % attractions.size());
                agenda.add("可回到" + attraction.name() + "周边慢逛，顺带安排咖啡、小店或拍照时间。");
            }

            itinerary.add(new TravelPlanResponse.DailyPlan(
                    day,
                    startDate.plusDays(day - 1).toString(),
                    buildTheme(day, request.interests()),
                    List.copyOf(agenda),
                    fallbackFood(request.budgetLevel()),
                    fallbackEvening(request.pace())
            ));
        }
        return itinerary;
    }

    private String buildTheme(int day, List<String> interests) {
        if (interests.isEmpty()) return "城市经典漫游";
        return "第" + day + "天：" + interests.get((day - 1) % interests.size());
    }

    private String fallbackFood(String budgetLevel) {
        return switch (budgetLevel) {
            case "品质升级" -> "建议提前预订一顿口碑餐厅，午餐则安排在景点附近轻松解决。";
            case "轻松省心" -> "优先选择本地小馆和街边特色小吃，整体更灵活也更省预算。";
            default -> "可以搭配一顿热门餐厅和一顿灵活的小吃或简餐。";
        };
    }

    private String fallbackEvening(String pace) {
        return switch (pace) {
            case "轻松慢游" -> "晚上尽量早点回酒店，附近散步放松即可。";
            case "紧凑高效" -> "如果体力允许，晚上可加一个夜景点或在地演出。";
            default -> "晚上留给夜市、江边散步或城市观景点，自由调整即可。";
        };
    }

    private int findAssignedDay(String attractionName,
                                Map<Integer, Set<String>> dayAttractionNames,
                                int index,
                                int days) {
        for (var entry : dayAttractionNames.entrySet()) {
            if (entry.getValue().contains(attractionName)) {
                return entry.getKey();
            }
        }
        return Math.min((index / 2) + 1, Math.max(days, 1));
    }

    private Map<Integer, Set<String>> extractDayAttractionNames(List<TravelPlanResponse.DailyPlan> itinerary,
                                                                 List<TravelPlanResponse.SightRecommendation> attractions) {
        Map<Integer, Set<String>> result = new java.util.HashMap<>();
        List<String> attractionNames = attractions.stream().map(TravelPlanResponse.SightRecommendation::name).toList();
        for (TravelPlanResponse.DailyPlan day : itinerary) {
            Set<String> matched = new java.util.HashSet<>();
            String allText = String.join(" ", day.agenda());
            for (String name : attractionNames) {
                if (allText.contains(name)) {
                    matched.add(name);
                }
            }
            if (!matched.isEmpty()) {
                result.put(day.day(), matched);
            }
        }
        return result;
    }

    private List<TravelPlanResponse.MapPoint> buildMapPoints(List<TravelPlanResponse.SightRecommendation> attractions,
                                                             List<TravelPlanResponse.HotelRecommendation> hotels,
                                                             int days,
                                                             Map<Integer, Set<String>> dayAttractionNames) {
        Set<String> seen = new LinkedHashSet<>();
        List<TravelPlanResponse.MapPoint> points = new ArrayList<>();

        for (TravelPlanResponse.HotelRecommendation hotel : hotels) {
            if (seen.add("hotel:" + hotel.name())) {
                points.add(new TravelPlanResponse.MapPoint(
                        0,
                        0,
                        hotel.name(),
                        "酒店",
                        hotel.district(),
                        hotel.address(),
                        hotel.highlight(),
                        hotel.latitude(),
                        hotel.longitude(),
                        hotel.imageUrl()
                ));
            }
        }

        int[] dayCounter = new int[days + 1];
        for (int index = 0; index < attractions.size(); index++) {
            TravelPlanResponse.SightRecommendation attraction = attractions.get(index);
            if (seen.add("spot:" + attraction.name())) {
                int assignedDay = findAssignedDay(attraction.name(), dayAttractionNames, index, days);
                dayCounter[assignedDay]++;
                points.add(new TravelPlanResponse.MapPoint(
                        assignedDay,
                        dayCounter[assignedDay],
                        attraction.name(),
                        "景点",
                        attraction.district(),
                        attraction.address(),
                        attraction.highlight(),
                        attraction.latitude(),
                        attraction.longitude(),
                        attraction.imageUrl()
                ));
            }
        }
        return points;
    }

    private List<TravelPlanResponse.RouteDay> buildRouteDays(List<TravelPlanResponse.SightRecommendation> attractions,
                                                             List<TravelPlanResponse.HotelRecommendation> hotels,
                                                             int days,
                                                             Map<Integer, Set<String>> dayAttractionNames) {
        if (hotels.isEmpty()) {
            return List.of();
        }

        TravelPlanResponse.HotelRecommendation hotel = hotels.get(0);
        List<TravelPlanResponse.RouteDay> routeDays = new ArrayList<>();
        Map<Integer, List<TravelPlanResponse.SightRecommendation>> attractionsByDay = assignAttractionsByDay(attractions, days, dayAttractionNames);

        for (int day = 1; day <= days; day++) {
            List<TravelPlanResponse.SightRecommendation> dailyAttractions = attractionsByDay.getOrDefault(day, List.of());
            if (dailyAttractions.size() < 2) {
                continue;
            }

            List<AmapClient.RouteStop> stops = new ArrayList<>();
            stops.add(new AmapClient.RouteStop(hotel.name(), hotel.latitude(), hotel.longitude()));
            for (TravelPlanResponse.SightRecommendation attraction : dailyAttractions) {
                stops.add(new AmapClient.RouteStop(attraction.name(), attraction.latitude(), attraction.longitude()));
            }
            stops.add(new AmapClient.RouteStop(hotel.name(), hotel.latitude(), hotel.longitude()));

            try {
                AmapClient.TravelRoute route = amapClient.planDrivingRoute(stops);
                if (route.polyline().isEmpty()) {
                    continue;
                }
                routeDays.add(new TravelPlanResponse.RouteDay(
                        day,
                        "driving",
                        List.of(new TravelPlanResponse.RouteSegment(
                                1,
                                hotel.name(),
                                hotel.name(),
                                route.distanceMeters(),
                                route.durationSeconds(),
                                route.polyline()
                        ))
                ));
            } catch (RuntimeException exception) {
                log.warn("第{}天真实路线规划失败，已跳过该日路线", day, exception);
            }
        }

        return routeDays;
    }

    private Map<Integer, List<TravelPlanResponse.SightRecommendation>> assignAttractionsByDay(List<TravelPlanResponse.SightRecommendation> attractions,
                                                                                                int days,
                                                                                                Map<Integer, Set<String>> dayAttractionNames) {
        Map<Integer, List<TravelPlanResponse.SightRecommendation>> result = new java.util.LinkedHashMap<>();
        for (int day = 1; day <= days; day++) {
            result.put(day, new ArrayList<>());
        }
        for (int index = 0; index < attractions.size(); index++) {
            TravelPlanResponse.SightRecommendation attraction = attractions.get(index);
            int assignedDay = findAssignedDay(attraction.name(), dayAttractionNames, index, days);
            result.computeIfAbsent(assignedDay, ignored -> new ArrayList<>()).add(attraction);
        }
        return result;
    }

    private List<String> buildTravelTips(TravelPlanRequest request,
                                         List<TravelPlanResponse.WeatherSnapshot> weather,
                                         org.example.demo.travel.gateway.MapGateway.DestinationProfile destination) {
        List<String> tips = new ArrayList<>();
        tips.add("优先住在" + destination.bestArea() + "附近，能明显减少跨区往返时间。");
        tips.add(destination.transportAdvice());
        if (!weather.isEmpty()) {
            tips.add("天气提醒：" + weather.get(0).advice());
        }
        if ("亲子友好".equalsIgnoreCase(request.hotelStyle()) || request.children() > 0) {
            tips.add("建议尽早预订更大的房型，并在每天下午留出一段低强度活动时间。 ");
        }
        if ("品质升级".equalsIgnoreCase(request.budgetLevel())) {
            tips.add("热门餐厅和观景位建议提前预约，避免高峰排队影响节奏。 ");
        }
        return tips;
    }
}
