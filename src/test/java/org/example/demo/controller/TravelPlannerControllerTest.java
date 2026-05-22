//package org.example.ragdemo.controller;
//
//import org.example.ragdemo.config.SecurityConfig;
//import org.example.ragdemo.model.StoredTravelPlanResponse;
//import org.example.ragdemo.model.TravelPlanRequest;
//import org.example.ragdemo.model.TravelPlanResponse;
//import org.example.ragdemo.model.TravelPlanningOptionsResponse;
//import org.example.ragdemo.security.SessionAuthenticationFilter;
//import org.example.ragdemo.service.TravelPlannerService;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.context.annotation.Import;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.time.Instant;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(TravelPlannerController.class)
//@Import({TravelPlannerExceptionHandler.class, SecurityConfig.class, SessionAuthenticationFilter.class})
//class TravelPlannerControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private TravelPlannerService travelPlannerService;
//
//    @Test
//    void optionsReturnsConfiguredLists() throws Exception {
//        when(travelPlannerService.options()).thenReturn(new TravelPlanningOptionsResponse(
//                List.of("杭州", "成都"),
//                List.of("美食打卡", "自然风光"),
//                List.of("均衡舒适"),
//                List.of("经典平衡"),
//                List.of("市区便利")
//        ));
//
//        mockMvc.perform(get("/api/travel/options").sessionAttr("auth.userId", 1L))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.featuredDestinations[0]").value("杭州"))
//                .andExpect(jsonPath("$.interests[1]").value("自然风光"));
//    }
//
//    @Test
//    void planReturnsStructuredPayload() throws Exception {
//        when(travelPlannerService.createPlan(any(TravelPlanRequest.class))).thenReturn(samplePlan());
//
//        mockMvc.perform(post("/api/travel/plan").sessionAttr("auth.userId", 1L)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(requestJson()))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.title").value("杭州智能行程方案"))
//                .andExpect(jsonPath("$.summary.days").value(3))
//                .andExpect(jsonPath("$.itinerary[0].agenda[0]").exists())
//                .andExpect(jsonPath("$.hotels[0].name").value("西湖湖景酒店"));
//    }
//
//    @Test
//    void createItineraryReturnsStoredPayload() throws Exception {
//        when(travelPlannerService.createItinerary(eq(1L), any(TravelPlanRequest.class))).thenReturn(sampleStoredResponse());
//
//        mockMvc.perform(post("/api/travel/itineraries").sessionAttr("auth.userId", 1L)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(requestJson()))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.itineraryId").value(10))
//                .andExpect(jsonPath("$.sessionCode").value("session-1"))
//                .andExpect(jsonPath("$.favorite").value(false))
//                .andExpect(jsonPath("$.destination").value("杭州"))
//                .andExpect(jsonPath("$.plan.title").value("杭州智能行程方案"));
//    }
//
//    @Test
//    void listItinerariesReturnsFilteredPayload() throws Exception {
//        when(travelPlannerService.listItineraries(1L, "西湖", "杭州", true, "2026-06-01", "2026-06-03"))
//                .thenReturn(List.of(sampleStoredResponse()));
//
//        mockMvc.perform(get("/api/travel/itineraries")
//                        .sessionAttr("auth.userId", 1L)
//                        .param("keyword", "西湖")
//                        .param("destination", "杭州")
//                        .param("favoriteOnly", "true")
//                        .param("startDate", "2026-06-01")
//                        .param("endDate", "2026-06-03"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].itineraryId").value(10))
//                .andExpect(jsonPath("$[0].destination").value("杭州"));
//    }
//
//    @Test
//    void getItineraryReturnsStoredPayload() throws Exception {
//        when(travelPlannerService.getItinerary(1L, 10L)).thenReturn(sampleStoredResponse());
//
//        mockMvc.perform(get("/api/travel/itineraries/10").sessionAttr("auth.userId", 1L))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.itineraryId").value(10))
//                .andExpect(jsonPath("$.plan.mapPoints[0].day").value(1));
//    }
//
//    @Test
//    void reviseItineraryReturnsUpdatedPayload() throws Exception {
//        when(travelPlannerService.reviseItinerary(eq(1L), eq(10L), any())).thenReturn(sampleStoredResponse());
//
//        mockMvc.perform(post("/api/travel/itineraries/10/revise").sessionAttr("auth.userId", 1L)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("""
//                                {
//                                  \"message\":\"第二天下午改轻松一点\"
//                                }
//                                """))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.revisionNo").value(2));
//    }
//
//    @Test
//    void deleteItineraryDelegatesToService() throws Exception {
//        doNothing().when(travelPlannerService).deleteItinerary(1L, 10L);
//
//        mockMvc.perform(delete("/api/travel/itineraries/10").sessionAttr("auth.userId", 1L))
//                .andExpect(status().isOk());
//
//        verify(travelPlannerService).deleteItinerary(1L, 10L);
//    }
//
//    @Test
//    void planReturnsBadRequestWhenValidationFails() throws Exception {
//        mockMvc.perform(post("/api/travel/plan").sessionAttr("auth.userId", 1L)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("""
//                                {
//                                  \"departureCity\":\"\",
//                                  \"destination\":\"杭州\",
//                                  \"startDate\":\"2026-06-01\",
//                                  \"endDate\":\"2026-06-03\",
//                                  \"adults\":0,
//                                  \"children\":0,
//                                  \"budgetLevel\":\"均衡舒适\",
//                                  \"pace\":\"经典平衡\",
//                                  \"hotelStyle\":\"市区便利\",
//                                  \"interests\":[],
//                                  \"notes\":\"\"
//                                }
//                                """))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.error").exists());
//    }
//
//    @Test
//    void protectedEndpointReturnsJsonWhenUnauthorized() throws Exception {
//        mockMvc.perform(get("/api/travel/itineraries/10"))
//                .andExpect(status().isUnauthorized())
//                .andExpect(jsonPath("$.error").value("Unauthorized"));
//    }
//
//    private String requestJson() {
//        return """
//                {
//                  \"departureCity\":\"上海\",
//                  \"destination\":\"杭州\",
//                  \"startDate\":\"2026-06-01\",
//                  \"endDate\":\"2026-06-03\",
//                  \"adults\":2,
//                  \"children\":0,
//                  \"budgetLevel\":\"均衡舒适\",
//                  \"pace\":\"经典平衡\",
//                  \"hotelStyle\":\"市区便利\",
//                  \"interests\":[\"自然风光\",\"美食打卡\"],
//                  \"notes\":\"希望安排一个湖边日落机位。\"
//                }
//                """;
//    }
//
//    private StoredTravelPlanResponse sampleStoredResponse() {
//        return new StoredTravelPlanResponse(
//                10L,
//                "session-1",
//                2,
//                false,
//                "上海",
//                "杭州",
//                "2026-06-01",
//                "2026-06-03",
//                Instant.parse("2026-05-01T08:00:00Z"),
//                Instant.parse("2026-05-01T09:00:00Z"),
//                new TravelPlanRequest(
//                        "上海",
//                        "杭州",
//                        "2026-06-01",
//                        "2026-06-03",
//                        2,
//                        0,
//                        "均衡舒适",
//                        "经典平衡",
//                        "市区便利",
//                        List.of("自然风光", "美食打卡"),
//                        "希望安排一个湖边日落机位。"
//                ),
//                samplePlan()
//        );
//    }
//
//    private TravelPlanResponse samplePlan() {
//        return new TravelPlanResponse(
//                "杭州智能行程方案",
//                "整体围绕西湖与清河坊安排，兼顾热门景点与轻松步行节奏。",
//                new TravelPlanResponse.TripSummary(3, 2, "均衡舒适", "经典平衡", "西湖 / 湖滨", "优先地铁出行，跨区打车补充即可。"),
//                List.of(new TravelPlanResponse.DailyPlan(
//                        1,
//                        "2026-06-01",
//                        "第1天：自然风光",
//                        List.of("游览西湖。", "前往清河坊街。"),
//                        "午餐可安排本地热门餐厅，晚餐留给小吃街灵活解决。",
//                        "晚上可在湖滨散步看夜景。"
//                )),
//                List.of(new TravelPlanResponse.SightRecommendation(
//                        "西湖",
//                        "自然",
//                        "西湖区",
//                        "经典湖景与城市步行路线。",
//                        "2-3小时",
//                        30.25,
//                        120.15,
//                        null
//                )),
//                List.of(new TravelPlanResponse.HotelRecommendation(
//                        "西湖湖景酒店",
//                        "西湖区",
//                        "市区便利",
//                        "均衡舒适",
//                        "步行即可到达湖滨一线。",
//                        "https://example.com/hotel.jpg",
//                        30.26,
//                        120.16
//                )),
//                List.of(new TravelPlanResponse.WeatherSnapshot(
//                        "2026-06-01",
//                        "晴到多云",
//                        "18-27C",
//                        "建议分层穿搭。"
//                )),
//                List.of(new TravelPlanResponse.MapPoint(
//                        1,
//                        1,
//                        "西湖",
//                        "景点",
//                        "西湖区",
//                        30.25,
//                        120.15,
//                        null
//                )),
//                List.of("建议住在西湖 / 湖滨附近，减少跨区往返。")
//        );
//    }
//}
