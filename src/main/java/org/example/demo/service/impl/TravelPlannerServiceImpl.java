package org.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.demo.config.TravelPlannerProperties;
import org.example.demo.entity.ItineraryEntity;
import org.example.demo.entity.ItineraryRevisionEntity;
import org.example.demo.entity.TripSessionEntity;
import org.example.demo.mapper.ItineraryMapper;
import org.example.demo.mapper.ItineraryRevisionMapper;
import org.example.demo.mapper.TripSessionMapper;
import org.example.demo.model.ItinerarySummaryResponse;
import org.example.demo.model.PageResult;
import org.example.demo.model.StoredTravelPlanResponse;
import org.example.demo.model.TravelPlanRequest;
import org.example.demo.model.TravelPlanResponse;
import org.example.demo.model.TravelPlanRevisionRequest;
import org.example.demo.model.TravelPlanningOptionsResponse;
import org.example.demo.service.TravelPlannerService;
import org.example.demo.service.support.TravelBudgetCalculator;
import org.example.demo.service.support.TravelPlanAssembler;
import org.example.demo.travel.agent.AttractionSearchAgent;
import org.example.demo.travel.agent.HotelRecommendationAgent;
import org.example.demo.travel.agent.PlanCoordinatorAgent;
import org.example.demo.travel.agent.WeatherQueryAgent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

@Service
public class TravelPlannerServiceImpl implements TravelPlannerService {

    private final TravelPlannerProperties properties;
    private final AttractionSearchAgent attractionSearchAgent;
    private final WeatherQueryAgent weatherQueryAgent;
    private final HotelRecommendationAgent hotelRecommendationAgent;
    private final PlanCoordinatorAgent planCoordinatorAgent;
    private final TripSessionMapper tripSessionMapper;
    private final ItineraryMapper itineraryMapper;
    private final ItineraryRevisionMapper itineraryRevisionMapper;
    private final TravelPlanAssembler travelPlanAssembler;
    private final TravelBudgetCalculator travelBudgetCalculator;
    private final TaskExecutor travelPlanningTaskExecutor;

    public TravelPlannerServiceImpl(TravelPlannerProperties properties,
                                    AttractionSearchAgent attractionSearchAgent,
                                    WeatherQueryAgent weatherQueryAgent,
                                    HotelRecommendationAgent hotelRecommendationAgent,
                                    PlanCoordinatorAgent planCoordinatorAgent,
                                    TripSessionMapper tripSessionMapper,
                                    ItineraryMapper itineraryMapper,
                                    ItineraryRevisionMapper itineraryRevisionMapper,
                                    TravelPlanAssembler travelPlanAssembler,
                                    TravelBudgetCalculator travelBudgetCalculator,
                                    @Qualifier("travelPlanningTaskExecutor") TaskExecutor travelPlanningTaskExecutor) {
        this.properties = properties;
        this.attractionSearchAgent = attractionSearchAgent;
        this.weatherQueryAgent = weatherQueryAgent;
        this.hotelRecommendationAgent = hotelRecommendationAgent;
        this.planCoordinatorAgent = planCoordinatorAgent;
        this.tripSessionMapper = tripSessionMapper;
        this.itineraryMapper = itineraryMapper;
        this.itineraryRevisionMapper = itineraryRevisionMapper;
        this.travelPlanAssembler = travelPlanAssembler;
        this.travelBudgetCalculator = travelBudgetCalculator;
        this.travelPlanningTaskExecutor = travelPlanningTaskExecutor;
    }

    @Override
    public TravelPlanningOptionsResponse options() {
        return new TravelPlanningOptionsResponse(
                List.copyOf(properties.getFeaturedDestinations()),
                List.copyOf(properties.getInterests()),
                List.copyOf(properties.getBudgetLevels()),
                List.copyOf(properties.getPaceOptions()),
                List.copyOf(properties.getHotelStyles())
        );
    }

    @Override
    public TravelPlanResponse createPlan(TravelPlanRequest request) {
        return buildPlan("preview-" + request.destination() + '-' + UUID.randomUUID(), request, null);
    }

    @Override
    @Transactional
    public StoredTravelPlanResponse createItinerary(Long userId, TravelPlanRequest request) {
        Instant now = Instant.now();

        TripSessionEntity session = new TripSessionEntity();
        session.setSessionCode(UUID.randomUUID().toString());
        session.setDestination(request.destination());
        session.setStatus("ACTIVE");
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        tripSessionMapper.insert(session);

        TravelPlanResponse plan = buildPlan(session.getSessionCode(), request, null);

        ItineraryEntity itinerary = new ItineraryEntity();
        itinerary.setSessionId(session.getId());
        itinerary.setUserId(userId);
        itinerary.setTitle(plan.title());
        itinerary.setOverview(plan.overview());
        itinerary.setDepartureCity(request.departureCity());
        itinerary.setDestination(request.destination());
        itinerary.setStartDate(parseDate(request.startDate(), "startDate"));
        itinerary.setEndDate(parseDate(request.endDate(), "endDate"));
        itinerary.setRequestJson(travelPlanAssembler.writeRequest(request));
        itinerary.setCurrentPlanJson(travelPlanAssembler.writePlan(plan));
        itinerary.setCurrentRevision(1);
        itinerary.setFavorite(false);
        itinerary.setCreatedAt(now);
        itinerary.setUpdatedAt(now);
        itineraryMapper.insert(itinerary);

        ItineraryRevisionEntity revision = new ItineraryRevisionEntity();
        revision.setItineraryId(itinerary.getId());
        revision.setRevisionNo(1);
        revision.setUserMessage("Initial itinerary request");
        revision.setStructuredPlanJson(travelPlanAssembler.writePlan(plan));
        revision.setCreatedAt(now);
        itineraryRevisionMapper.insert(revision);

        return toStoredResponse(itinerary, session, request, plan);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ItinerarySummaryResponse> listItineraries(Long userId,
                                                                String keyword,
                                                                String destination,
                                                                Boolean favoriteOnly,
                                                                String startDate,
                                                                String endDate,
                                                                long pageNo,
                                                                long pageSize) {
        LocalDate start = parseOptionalDate(startDate, "startDate");
        LocalDate end = parseOptionalDate(endDate, "endDate");
        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }

        long safePageNo = Math.max(pageNo, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);

        LambdaQueryWrapper<ItineraryEntity> query = new LambdaQueryWrapper<ItineraryEntity>()
                .eq(ItineraryEntity::getUserId, userId)
                .isNull(ItineraryEntity::getDeletedAt)
                .orderByDesc(ItineraryEntity::getUpdatedAt);

        if (keyword != null && !keyword.isBlank()) {
            query.and(wrapper -> wrapper
                    .like(ItineraryEntity::getTitle, keyword.trim())
                    .or()
                    .like(ItineraryEntity::getOverview, keyword.trim()));
        }
        if (destination != null && !destination.isBlank()) {
            query.eq(ItineraryEntity::getDestination, destination.trim());
        }
        if (Boolean.TRUE.equals(favoriteOnly)) {
            query.eq(ItineraryEntity::isFavorite, true);
        }
        if (start != null) {
            query.ge(ItineraryEntity::getStartDate, start);
        }
        if (end != null) {
            query.le(ItineraryEntity::getEndDate, end);
        }

        Page<ItineraryEntity> page = itineraryMapper.selectPage(new Page<>(safePageNo, safePageSize), query);
        List<ItinerarySummaryResponse> records = page.getRecords().stream()
                .map(this::toSummaryResponse)
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public StoredTravelPlanResponse getItinerary(Long userId, Long itineraryId) {
        ItineraryEntity itinerary = getItineraryEntity(userId, itineraryId);
        return toStoredResponse(itinerary, readRequest(itinerary.getRequestJson()), travelPlanAssembler.readPlan(itinerary.getCurrentPlanJson()));
    }

    @Override
    @Transactional
    public StoredTravelPlanResponse reviseItinerary(Long userId, Long itineraryId, TravelPlanRevisionRequest request) {
        ItineraryEntity itinerary = getItineraryEntity(userId, itineraryId);
        TripSessionEntity session = getTripSession(itinerary.getSessionId());

        TravelPlanRequest originalRequest = readRequest(itinerary.getRequestJson());
        TravelPlanResponse revisedPlan = buildPlan(session.getSessionCode(), originalRequest, request.message());

        int nextRevision = itinerary.getCurrentRevision() + 1;
        itinerary.setTitle(revisedPlan.title());
        itinerary.setOverview(revisedPlan.overview());
        itinerary.setDepartureCity(originalRequest.departureCity());
        itinerary.setDestination(originalRequest.destination());
        itinerary.setStartDate(parseDate(originalRequest.startDate(), "startDate"));
        itinerary.setEndDate(parseDate(originalRequest.endDate(), "endDate"));
        itinerary.setCurrentPlanJson(travelPlanAssembler.writePlan(revisedPlan));
        itinerary.setCurrentRevision(nextRevision);
        itinerary.setUpdatedAt(Instant.now());
        itineraryMapper.updateById(itinerary);

        ItineraryRevisionEntity revision = new ItineraryRevisionEntity();
        revision.setItineraryId(itinerary.getId());
        revision.setRevisionNo(nextRevision);
        revision.setUserMessage(request.message());
        revision.setStructuredPlanJson(travelPlanAssembler.writePlan(revisedPlan));
        revision.setCreatedAt(Instant.now());
        itineraryRevisionMapper.insert(revision);

        return toStoredResponse(itinerary, originalRequest, revisedPlan);
    }

    @Override
    @Transactional
    public StoredTravelPlanResponse updateFavorite(Long userId, Long itineraryId, boolean favorite) {
        ItineraryEntity itinerary = getItineraryEntity(userId, itineraryId);
        itinerary.setFavorite(favorite);
        itinerary.setUpdatedAt(Instant.now());
        itineraryMapper.updateById(itinerary);
        return toStoredResponse(itinerary, readRequest(itinerary.getRequestJson()), travelPlanAssembler.readPlan(itinerary.getCurrentPlanJson()));
    }

    @Override
    @Transactional
    public void deleteItinerary(Long userId, Long itineraryId) {
        ItineraryEntity itinerary = getItineraryEntity(userId, itineraryId);
        itinerary.setDeletedAt(Instant.now());
        itinerary.setUpdatedAt(Instant.now());
        itineraryMapper.updateById(itinerary);
    }

    private ItineraryEntity getItineraryEntity(Long userId, Long itineraryId) {
        ItineraryEntity itinerary = itineraryMapper.selectOne(new LambdaQueryWrapper<ItineraryEntity>()
                .eq(ItineraryEntity::getId, itineraryId)
                .eq(ItineraryEntity::getUserId, userId)
                .isNull(ItineraryEntity::getDeletedAt)
                .last("LIMIT 1"));
        if (itinerary == null) {
            throw new IllegalArgumentException("itineraryId not found");
        }
        return itinerary;
    }

    private TripSessionEntity getTripSession(Long sessionId) {
        TripSessionEntity session = tripSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalStateException("session not found for itinerary");
        }
        return session;
    }

    private TravelPlanResponse buildPlan(String sessionId, TravelPlanRequest request, String revisionMessage) {
        LocalDate startDate = parseDate(request.startDate(), "startDate");
        LocalDate endDate = parseDate(request.endDate(), "endDate");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }

        int days = Math.toIntExact(ChronoUnit.DAYS.between(startDate, endDate)) + 1;
        CompletableFuture<List<TravelPlanResponse.SightRecommendation>> attractionsFuture = supplyAsync(
                () -> attractionSearchAgent.search(request, days),
                "景点检索失败"
        );
        CompletableFuture<List<TravelPlanResponse.WeatherSnapshot>> weatherFuture = supplyAsync(
                () -> weatherQueryAgent.forecast(request, startDate, endDate),
                "天气查询失败"
        );
        CompletableFuture<List<TravelPlanResponse.HotelRecommendation>> hotelsFuture = supplyAsync(
                () -> hotelRecommendationAgent.recommend(request),
                "酒店推荐失败"
        );

        try {
            CompletableFuture.allOf(attractionsFuture, weatherFuture, hotelsFuture).join();
        } catch (CompletionException exception) {
            throw toRuntimeException(unwrapCompletionException(exception));
        }

        List<TravelPlanResponse.SightRecommendation> attractions = joinFuture(attractionsFuture);
        List<TravelPlanResponse.WeatherSnapshot> weather = joinFuture(weatherFuture);
        List<TravelPlanResponse.HotelRecommendation> hotels = joinFuture(hotelsFuture);
        PlanCoordinatorAgent.PlanDraft draft = planCoordinatorAgent.coordinate(
                sessionId, request, startDate, days, attractions, hotels, weather, revisionMessage
        );

        TravelPlanResponse.TripSummary summary = new TravelPlanResponse.TripSummary(
                days,
                Math.max(days - 1, 0),
                request.budgetLevel(),
                request.pace(),
                draft.stayArea(),
                draft.transportAdvice()
        );
        TravelPlanResponse.BudgetSummary budget = travelBudgetCalculator.calculate(
                request,
                summary,
                attractions,
                hotels,
                draft.routeDays()
        );

        return new TravelPlanResponse(
                request.destination() + "智能行程方案",
                draft.overview(),
                summary,
                draft.itinerary(),
                attractions,
                hotels,
                weather,
                draft.mapPoints(),
                draft.routeDays(),
                budget,
                draft.travelTips()
        );
    }

    private <T> CompletableFuture<T> supplyAsync(java.util.function.Supplier<T> supplier, String message) {
        return CompletableFuture.supplyAsync(supplier, travelPlanningTaskExecutor)
                .orTimeout(45, TimeUnit.SECONDS)
                .handle((value, throwable) -> {
                    if (throwable == null) {
                        return value;
                    }
                    Throwable cause = unwrapCompletionException(throwable);
                    if (cause instanceof java.util.concurrent.TimeoutException) {
                        throw new IllegalStateException(message + "：调用超时", cause);
                    }
                    throw new IllegalStateException(message, cause);
                });
    }

    private <T> T joinFuture(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException exception) {
            Throwable cause = unwrapCompletionException(exception);
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(cause == null ? "Unexpected async error" : cause.getMessage(), cause);
        }
    }

    private RuntimeException toRuntimeException(Throwable throwable) {
        if (throwable instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(throwable == null ? "Unexpected async error" : throwable.getMessage(), throwable);
    }

    private Throwable unwrapCompletionException(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private StoredTravelPlanResponse toStoredResponse(ItineraryEntity itinerary,
                                                      TripSessionEntity session,
                                                      TravelPlanRequest request,
                                                      TravelPlanResponse plan) {
        return new StoredTravelPlanResponse(
                itinerary.getId(),
                session.getSessionCode(),
                itinerary.getCurrentRevision(),
                itinerary.isFavorite(),
                request.departureCity(),
                request.destination(),
                request.startDate(),
                request.endDate(),
                itinerary.getCreatedAt(),
                itinerary.getUpdatedAt(),
                request,
                plan
        );
    }

    private StoredTravelPlanResponse toStoredResponse(ItineraryEntity itinerary,
                                                      TravelPlanRequest request,
                                                      TravelPlanResponse plan) {
        return toStoredResponse(itinerary, getTripSession(itinerary.getSessionId()), request, plan);
    }

    private ItinerarySummaryResponse toSummaryResponse(ItineraryEntity itinerary) {
        return new ItinerarySummaryResponse(
                itinerary.getId(),
                itinerary.getCurrentRevision(),
                itinerary.isFavorite(),
                itinerary.getDepartureCity(),
                itinerary.getDestination(),
                itinerary.getStartDate() == null ? null : itinerary.getStartDate().toString(),
                itinerary.getEndDate() == null ? null : itinerary.getEndDate().toString(),
                itinerary.getCreatedAt(),
                itinerary.getUpdatedAt(),
                itinerary.getTitle(),
                itinerary.getOverview()
        );
    }

    private TravelPlanRequest readRequest(String json) {
        try {
            return travelPlanAssembler.readRequest(json);
        } catch (IllegalStateException exception) {
            throw new IllegalStateException("Failed to deserialize travel request", exception);
        }
    }

    private LocalDate parseOptionalDate(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseDate(value, field);
    }

    private LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(field + " must use ISO date format yyyy-MM-dd", exception);
        }
    }
}
