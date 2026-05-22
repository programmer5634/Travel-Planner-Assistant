package org.example.demo.travel.client.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.demo.travel.client.AmapClient;
import org.example.demo.config.TravelApiProperties;
import org.example.demo.travel.gateway.MapGateway;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@Primary
public class RealAmapClient implements AmapClient {

    private static final Map<String, String> CITY_ALIASES = Map.ofEntries(
            Map.entry("杭州", "杭州"),
            Map.entry("hangzhou", "杭州"),
            Map.entry("成都", "成都"),
            Map.entry("chengdu", "成都"),
            Map.entry("西安", "西安"),
            Map.entry("xian", "西安"),
            Map.entry("xi'an", "西安"),
            Map.entry("三亚", "三亚"),
            Map.entry("sanya", "三亚"),
            Map.entry("桂林", "桂林"),
            Map.entry("guilin", "桂林")
    );

    private static final Map<String, String> INTEREST_ALIASES = Map.ofEntries(
            Map.entry("历史人文", "文化古迹"),
            Map.entry("自然风光", "风景名胜"),
            Map.entry("美食打卡", "餐饮服务"),
            Map.entry("摄影出片", "风景名胜"),
            Map.entry("亲子陪伴", "风景名胜"),
            Map.entry("夜生活", "购物服务")
    );

    private final WebClient.Builder webClientBuilder;
    private final TravelApiProperties properties;

    public RealAmapClient(WebClient.Builder webClientBuilder,
                          TravelApiProperties properties) {
        this.webClientBuilder = webClientBuilder;
        this.properties = properties;
    }

    @Override
    public MapGateway.DestinationProfile resolveDestination(String destination) {
        validateAmapConfig();
        String city = canonicalCity(destination);
        List<PoiItem> poiItems = fetchPois(city, List.of("风景名胜", "文化古迹"), 3);
        if (poiItems.isEmpty()) {
            throw new IllegalStateException("高德地点检索未返回可用目的地信息：" + city);
        }
        String summary = "基于高德地点检索，" + city + "适合围绕"
                + poiItems.stream().map(PoiItem::name).limit(2).reduce((left, right) -> left + "、" + right).orElse(city)
                + "展开游览。";
        String stayArea = poiItems.stream()
                .map(PoiItem::district)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(city + "核心区");
        return new MapGateway.DestinationProfile(city, summary, stayArea, "优先按行政区聚合景点，跨区移动尽量集中到同一时段。");
    }

    @Override
    public List<MapGateway.AttractionProfile> searchAttractions(String destination, List<String> interests, int limit) {
        validateAmapConfig();

        List<String> keywords = interests == null || interests.isEmpty()
                ? List.of("风景名胜")
                : interests.stream().map(this::mapInterestKeyword).distinct().toList();
        List<PoiItem> poiItems = fetchPois(canonicalCity(destination), keywords, Math.max(limit, 6));
        if (poiItems.isEmpty()) {
            throw new IllegalStateException("高德景点检索未返回可用结果：" + destination);
        }

        List<MapGateway.AttractionProfile> results = new ArrayList<>();
        Set<String> seen = new java.util.LinkedHashSet<>();
        for (PoiItem poi : poiItems) {
            if (!seen.add(poi.name())) {
                continue;
            }
            double[] location = parseLocation(poi.location());
            if (location == null) {
                continue;
            }
            results.add(new MapGateway.AttractionProfile(
                    poi.name(),
                    deriveCategory(poi.type()),
                    blankToDefault(poi.district(), canonicalCity(destination)),
                    blankToDefault(poi.address(), blankToDefault(poi.district(), canonicalCity(destination))),
                    buildAttractionHighlight(poi),
                    recommendDuration(poi.type()),
                    location[1],
                    location[0],
                    deriveTags(poi)
            ));
            if (results.size() >= limit) {
                break;
            }
        }

        if (results.isEmpty()) {
            throw new IllegalStateException("高德景点检索结果缺少有效坐标：" + destination);
        }
        return results;
    }

    @Override
    public List<MapGateway.HotelProfile> searchHotels(String destination, String hotelStyle, String budgetLevel, int limit) {
        validateAmapConfig();
        List<PoiItem> poiItems = fetchPois(canonicalCity(destination), List.of("酒店"), Math.max(limit, 6));
        if (poiItems.isEmpty()) {
            throw new IllegalStateException("高德酒店检索未返回可用结果：" + destination);
        }

        List<MapGateway.HotelProfile> results = new ArrayList<>();
        for (PoiItem poi : poiItems) {
            double[] location = parseLocation(poi.location());
            if (location == null) {
                continue;
            }
            results.add(new MapGateway.HotelProfile(
                    poi.name(),
                    blankToDefault(poi.district(), canonicalCity(destination)),
                    blankToDefault(poi.address(), blankToDefault(poi.district(), canonicalCity(destination))),
                    hotelStyle,
                    budgetLevel,
                    buildHotelHighlight(poi),
                    location[1],
                    location[0],
                    extractPoiPhoto(poi)
            ));
            if (results.size() >= limit) {
                break;
            }
        }

        if (results.isEmpty()) {
            throw new IllegalStateException("高德酒店检索结果缺少有效坐标：" + destination);
        }
        return results;
    }

    @Override
    public List<MapGateway.WeatherProfile> forecast(String destination, LocalDate startDate, LocalDate endDate) {
        validateWeatherConfig();

        GeoCodeResponse geoCodeResponse;
        try {
            geoCodeResponse = amapClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v3/geocode/geo")
                            .queryParam("address", canonicalCity(destination))
                            .queryParam("key", properties.amap().apiKey())
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, responseSpec -> responseSpec.createException().flatMap(reactor.core.publisher.Mono::error))
                    .bodyToMono(GeoCodeResponse.class)
                    .block();
        } catch (RuntimeException exception) {
            throw new IllegalStateException("高德地理编码调用失败：" + destination, exception);
        }

        GeoCodeItem city = extractGeoCode(geoCodeResponse);
        if (city == null) {
            throw new IllegalStateException("高德地理编码未返回城市编码：" + destination);
        }

        if (useOpenMeteoWeather()) {
            double[] location = parseLocation(city.location());
            if (location == null) {
                throw new IllegalStateException("高德地理编码未返回有效坐标：" + destination);
            }
            OpenMeteoForecastResponse response;
            try {
                response = weatherClient()
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v1/forecast")
                                .queryParam("latitude", location[1])
                                .queryParam("longitude", location[0])
                                .queryParam("timezone", "Asia/Shanghai")
                                .queryParam("start_date", startDate)
                                .queryParam("end_date", endDate)
                                .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min,wind_direction_10m_dominant,wind_speed_10m_max")
                                .build())
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, responseSpec -> responseSpec.createException().flatMap(reactor.core.publisher.Mono::error))
                        .bodyToMono(OpenMeteoForecastResponse.class)
                        .block();
            } catch (RuntimeException exception) {
                throw new IllegalStateException("Open-Meteo 天气调用失败：" + destination, exception);
            }
            List<MapGateway.WeatherProfile> weatherProfiles = buildWeatherProfiles(response, startDate, endDate);
            if (weatherProfiles.isEmpty()) {
                throw new IllegalStateException("Open-Meteo 未返回可用天气数据：" + destination);
            }
            return weatherProfiles;
        }

        WeatherForecastResponse response;
        try {
            response = weatherClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v3/weather/weatherInfo")
                            .queryParam("city", city.adcode())
                            .queryParam("key", properties.amap().apiKey())
                            .queryParam("extensions", "all")
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, responseSpec -> responseSpec.createException().flatMap(reactor.core.publisher.Mono::error))
                    .bodyToMono(WeatherForecastResponse.class)
                    .block();
        } catch (RuntimeException exception) {
            throw new IllegalStateException("高德天气调用失败：" + destination, exception);
        }
        List<MapGateway.WeatherProfile> weatherProfiles = buildWeatherProfiles(response, startDate, endDate);
        if (weatherProfiles.isEmpty()) {
            throw new IllegalStateException("高德天气接口未返回可用天气数据：" + destination);
        }
        return weatherProfiles;
    }

    @Override
    public TravelRoute planDrivingRoute(List<RouteStop> stops) {
        validateAmapConfig();
        if (stops == null || stops.size() < 2) {
            return new TravelRoute(0, 0, List.of());
        }

        RouteStop origin = stops.get(0);
        RouteStop destination = stops.get(stops.size() - 1);
        List<RouteStop> waypoints = stops.size() > 2 ? stops.subList(1, stops.size() - 1) : List.of();

        DrivingDirectionResponse response;
        try {
            response = amapClient()
                    .get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/v3/direction/driving")
                                .queryParam("origin", toLngLat(origin.longitude(), origin.latitude()))
                                .queryParam("destination", toLngLat(destination.longitude(), destination.latitude()))
                                .queryParam("key", properties.amap().apiKey())
                                .queryParam("extensions", "base")
                                .queryParam("strategy", 0);
                        if (!waypoints.isEmpty()) {
                            builder.queryParam("waypoints", waypoints.stream()
                                    .map(stop -> toLngLat(stop.longitude(), stop.latitude()))
                                    .reduce((left, right) -> left + ";" + right)
                                    .orElse(""));
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, responseSpec -> responseSpec.createException().flatMap(reactor.core.publisher.Mono::error))
                    .bodyToMono(DrivingDirectionResponse.class)
                    .block();
        } catch (RuntimeException exception) {
            throw new IllegalStateException("高德驾车路线规划调用失败", exception);
        }

        DrivingPath path = firstDrivingPath(response);
        if (path == null) {
            throw new IllegalStateException("高德驾车路线规划未返回有效路径");
        }

        List<org.example.demo.model.TravelPlanResponse.RouteCoordinate> polyline = buildDrivingPolyline(path);
        return new TravelRoute(parseDouble(path.distance()), parseDouble(path.duration()), polyline);
    }

    private WebClient amapClient() {
        return webClientBuilder.baseUrl(properties.amap().baseUrl()).build();
    }

    private WebClient weatherClient() {
        if (useOpenMeteoWeather()) {
            return webClientBuilder.baseUrl(properties.weather().baseUrl()).build();
        }
        return amapClient();
    }

    private boolean useOpenMeteoWeather() {
        String baseUrl = properties.weather() == null ? null : properties.weather().baseUrl();
        return !isBlank(baseUrl) && !baseUrl.contains("restapi.amap.com");
    }

    private void validateAmapConfig() {
        if (properties.amap() == null) {
            throw new IllegalStateException("缺少高德地图配置 travel.amap");
        }
        if (isBlank(properties.amap().baseUrl())) {
            throw new IllegalStateException("未配置高德服务地址 travel.amap.base-url");
        }
        if (isDemoKey(properties.amap().apiKey())) {
            throw new IllegalStateException("未配置有效的高德 API Key：travel.amap.api-key");
        }
    }

    private void validateWeatherConfig() {
        validateAmapConfig();
        if (properties.weather() == null || !Boolean.TRUE.equals(properties.weather().enabled())) {
            throw new IllegalStateException("天气真实调用已关闭，请启用 travel.weather.enabled");
        }
    }

    private String canonicalCity(String destination) {
        if (destination == null) {
            return "杭州";
        }
        String normalized = destination.replace("'", "").replace(" ", "").toLowerCase(Locale.ROOT);
        return CITY_ALIASES.getOrDefault(normalized, destination);
    }

    private String mapInterestKeyword(String interest) {
        if (interest == null || interest.isBlank()) {
            return "风景名胜";
        }
        return INTEREST_ALIASES.getOrDefault(interest, interest);
    }

    private List<PoiItem> fetchPois(String city, List<String> keywords, int limit) {
        List<PoiItem> merged = new ArrayList<>();
        Set<String> seen = new java.util.LinkedHashSet<>();
        for (String keyword : keywords) {
            PoiSearchResponse response;
            try {
                response = amapClient()
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v5/place/text")
                                .queryParam("keywords", keyword)
                                .queryParam("region", city)
                                .queryParam("key", properties.amap().apiKey())
                                .queryParam("page_size", Math.max(limit, 3))
                                .build())
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, responseSpec -> responseSpec.createException().flatMap(reactor.core.publisher.Mono::error))
                        .bodyToMono(PoiSearchResponse.class)
                        .block();
            } catch (RuntimeException exception) {
                throw new IllegalStateException("高德地点检索调用失败：" + city + " / " + keyword, exception);
            }
            if (response == null || response.pois() == null) {
                continue;
            }
            for (PoiItem poi : response.pois()) {
                if (poi == null || poi.name() == null || poi.location() == null) {
                    continue;
                }
                if (seen.add(poi.name())) {
                    merged.add(poi);
                }
            }
            if (merged.size() >= limit) {
                break;
            }
        }
        return merged.stream().limit(limit).toList();
    }

    private String deriveCategory(String type) {
        if (type == null || type.isBlank()) {
            return "景点";
        }
        if (type.contains("餐饮")) {
            return "美食";
        }
        if (type.contains("购物")) {
            return "夜游";
        }
        if (type.contains("文化") || type.contains("博物馆") || type.contains("寺庙")) {
            return "历史";
        }
        if (type.contains("风景") || type.contains("公园") || type.contains("自然")) {
            return "自然";
        }
        return "景点";
    }

    private DrivingPath firstDrivingPath(DrivingDirectionResponse response) {
        if (response == null || response.route() == null || response.route().paths() == null || response.route().paths().isEmpty()) {
            return null;
        }
        return response.route().paths().get(0);
    }

    private List<org.example.demo.model.TravelPlanResponse.RouteCoordinate> buildDrivingPolyline(DrivingPath path) {
        if (path.steps() == null || path.steps().isEmpty()) {
            return List.of();
        }
        List<org.example.demo.model.TravelPlanResponse.RouteCoordinate> polyline = new ArrayList<>();
        for (DrivingStep step : path.steps()) {
            if (step == null || step.polyline() == null || step.polyline().isBlank()) {
                continue;
            }
            for (String pair : step.polyline().split(";")) {
                double[] location = parseLocation(pair);
                if (location == null) {
                    continue;
                }
                org.example.demo.model.TravelPlanResponse.RouteCoordinate coordinate =
                        new org.example.demo.model.TravelPlanResponse.RouteCoordinate(location[1], location[0]);
                if (!polyline.isEmpty()) {
                    org.example.demo.model.TravelPlanResponse.RouteCoordinate last = polyline.get(polyline.size() - 1);
                    if (Math.abs(last.latitude() - coordinate.latitude()) < 0.000001d
                            && Math.abs(last.longitude() - coordinate.longitude()) < 0.000001d) {
                        continue;
                    }
                }
                polyline.add(coordinate);
            }
        }
        return polyline;
    }

    private String toLngLat(double longitude, double latitude) {
        return longitude + "," + latitude;
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private List<String> deriveTags(PoiItem poi) {
        List<String> tags = new ArrayList<>();
        String category = deriveCategory(poi.type());
        tags.add(category);
        if (poi.type() != null && poi.type().contains("餐饮")) {
            tags.add("美食打卡");
        }
        if (poi.type() != null && (poi.type().contains("公园") || poi.type().contains("风景"))) {
            tags.add("自然风光");
        }
        if (poi.type() != null && (poi.type().contains("文化") || poi.type().contains("博物馆") || poi.type().contains("寺庙"))) {
            tags.add("历史人文");
        }
        return tags.stream().distinct().toList();
    }

    private String buildAttractionHighlight(PoiItem poi) {
        String businessArea = blankToNull(poi.businessArea());
        if (businessArea != null) {
            return "位于" + businessArea + "，适合加入城市经典游览线路。";
        }
        String address = blankToNull(poi.address());
        if (address != null) {
            return "位于" + address + "，方便与周边点位串联安排行程。";
        }
        return "适合纳入当地热门打卡路线。";
    }

    private String buildHotelHighlight(PoiItem poi) {
        String address = blankToNull(poi.address());
        if (address != null) {
            return "位于" + address + "，便于衔接周边游玩与返程。";
        }
        return "位置便利，适合作为行程落脚点。";
    }

    private String extractPoiPhoto(PoiItem poi) {
        if (poi.photos() == null || poi.photos().isEmpty()) {
            return "";
        }
        PoiPhoto first = poi.photos().get(0);
        return first != null && first.url() != null ? first.url() : "";
    }

    private String recommendDuration(String type) {
        if (type == null || type.isBlank()) {
            return "2小时";
        }
        if (type.contains("餐饮") || type.contains("购物")) {
            return "1.5小时";
        }
        if (type.contains("公园") || type.contains("风景")) {
            return "2-3小时";
        }
        return "2小时";
    }

    private GeoCodeItem extractGeoCode(GeoCodeResponse response) {
        if (response == null || response.geocodes() == null || response.geocodes().isEmpty()) {
            return null;
        }
        GeoCodeItem item = response.geocodes().get(0);
        if (item == null || item.adcode() == null || item.adcode().isBlank()) {
            return null;
        }
        return item;
    }

    private List<MapGateway.WeatherProfile> buildWeatherProfiles(WeatherForecastResponse response, LocalDate startDate, LocalDate endDate) {
        if (response == null || response.forecasts() == null || response.forecasts().isEmpty()) {
            return List.of();
        }
        WeatherForecast forecast = response.forecasts().get(0);
        if (forecast == null || forecast.casts() == null || forecast.casts().isEmpty()) {
            return List.of();
        }
        List<MapGateway.WeatherProfile> result = new ArrayList<>();
        for (WeatherCast cast : forecast.casts()) {
            if (cast == null || cast.date() == null || cast.date().isBlank()) {
                continue;
            }
            LocalDate date;
            try {
                date = LocalDate.parse(cast.date());
            } catch (RuntimeException exception) {
                continue;
            }
            if (date.isBefore(startDate) || date.isAfter(endDate)) {
                continue;
            }
            String condition = buildWeatherCondition(cast.dayweather(), cast.nightweather());
            String temperature = buildTemperatureRange(cast.nighttemp(), cast.daytemp());
            String advice = buildWeatherAdvice(condition, cast.daywind(), cast.daypower());
            result.add(new MapGateway.WeatherProfile(date.toString(), condition, temperature, advice));
        }
        return result;
    }

    private List<MapGateway.WeatherProfile> buildWeatherProfiles(OpenMeteoForecastResponse response, LocalDate startDate, LocalDate endDate) {
        if (response == null || response.daily() == null || response.daily().time() == null || response.daily().time().isEmpty()) {
            return List.of();
        }
        List<MapGateway.WeatherProfile> result = new ArrayList<>();
        for (int index = 0; index < response.daily().time().size(); index++) {
            String rawDate = response.daily().time().get(index);
            if (rawDate == null || rawDate.isBlank()) {
                continue;
            }
            LocalDate date;
            try {
                date = LocalDate.parse(rawDate);
            } catch (RuntimeException exception) {
                continue;
            }
            if (date.isBefore(startDate) || date.isAfter(endDate)) {
                continue;
            }
            Integer weatherCode = valueAt(response.daily().weatherCode(), index);
            Double maxTemp = valueAt(response.daily().temperatureMax(), index);
            Double minTemp = valueAt(response.daily().temperatureMin(), index);
            Integer windDirection = valueAt(response.daily().windDirection(), index);
            Double windSpeed = valueAt(response.daily().windSpeed(), index);
            String condition = mapOpenMeteoWeatherCode(weatherCode);
            String temperature = buildTemperatureRange(minTemp, maxTemp);
            String advice = buildWeatherAdvice(condition, windDirection, windSpeed);
            result.add(new MapGateway.WeatherProfile(date.toString(), condition, temperature, advice));
        }
        return result;
    }

    private <T> T valueAt(List<T> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return null;
        }
        return values.get(index);
    }

    private String buildWeatherCondition(String dayWeather, String nightWeather) {
        String day = blankToNull(dayWeather);
        String night = blankToNull(nightWeather);
        if (day == null && night == null) {
            return "天气待更新";
        }
        if (day == null) {
            return night;
        }
        if (night == null || day.equals(night)) {
            return day;
        }
        return day + "转" + night;
    }

    private String buildTemperatureRange(String nightTemp, String dayTemp) {
        String low = blankToNull(nightTemp);
        String high = blankToNull(dayTemp);
        if (low == null && high == null) {
            return "温度待更新";
        }
        if (low == null) {
            return high + "C";
        }
        if (high == null) {
            return low + "C";
        }
        return low + "-" + high + "C";
    }

    private String buildTemperatureRange(Double lowTemp, Double highTemp) {
        String low = formatNumber(lowTemp);
        String high = formatNumber(highTemp);
        if (low == null && high == null) {
            return "温度待更新";
        }
        if (low == null) {
            return high + "C";
        }
        if (high == null) {
            return low + "C";
        }
        return low + "-" + high + "C";
    }

    private String buildWeatherAdvice(String condition, String windDirection, String windPower) {
        String advice = condition.contains("雨") ? "建议随身带伞，并优先安排室内或短距离点位。" : "建议穿舒适步行鞋，按早晚温差准备薄外套。";
        String wind = blankToNull(windDirection);
        String power = blankToNull(windPower);
        if (wind != null && power != null) {
            return advice + " 当前风向" + wind + "，风力" + power + "级。";
        }
        return advice;
    }

    private String buildWeatherAdvice(String condition, Integer windDirection, Double windSpeed) {
        String advice = condition.contains("雨") ? "建议随身带伞，并优先安排室内或短距离点位。" : "建议穿舒适步行鞋，按早晚温差准备薄外套。";
        String wind = describeWindDirection(windDirection);
        String speed = formatNumber(windSpeed);
        if (wind != null && speed != null) {
            return advice + " 当前主导风向" + wind + "，最大风速约" + speed + "km/h。";
        }
        return advice;
    }

    private String describeWindDirection(Integer degrees) {
        if (degrees == null) {
            return null;
        }
        if (degrees >= 338 || degrees < 23) {
            return "北";
        }
        if (degrees < 68) {
            return "东北";
        }
        if (degrees < 113) {
            return "东";
        }
        if (degrees < 158) {
            return "东南";
        }
        if (degrees < 203) {
            return "南";
        }
        if (degrees < 248) {
            return "西南";
        }
        if (degrees < 293) {
            return "西";
        }
        return "西北";
    }

    private String formatNumber(Double value) {
        if (value == null) {
            return null;
        }
        if (Math.abs(value - Math.rint(value)) < 0.05d) {
            return String.valueOf((int) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private String mapOpenMeteoWeatherCode(Integer code) {
        if (code == null) {
            return "天气待更新";
        }
        return switch (code) {
            case 0 -> "晴";
            case 1, 2 -> "多云";
            case 3 -> "阴";
            case 45, 48 -> "雾";
            case 51, 53, 55, 56, 57 -> "毛毛雨";
            case 61, 63, 65, 66, 67, 80, 81, 82 -> "降雨";
            case 71, 73, 75, 77, 85, 86 -> "降雪";
            case 95, 96, 99 -> "雷暴";
            default -> "天气待更新";
        };
    }

    private double[] parseLocation(String location) {
        if (location == null || location.isBlank() || !location.contains(",")) {
            return null;
        }
        String[] parts = location.split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1])};
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String blankToDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    private boolean isDemoKey(String value) {
        return isBlank(value) || "demo-key".equals(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record PoiSearchResponse(List<PoiItem> pois) {
    }

    private record PoiItem(String name, String address, String type, String location, String pname, String cityname, String adname, String businessArea, List<PoiPhoto> photos) {
        String district() {
            return adname;
        }
    }

    private record PoiPhoto(String title, String url) {
    }

    private record GeoCodeResponse(List<GeoCodeItem> geocodes) {
    }

    private record GeoCodeItem(String adcode, String location) {
    }

    private record DrivingDirectionResponse(DrivingRoute route) {
    }

    private record DrivingRoute(List<DrivingPath> paths) {
    }

    private record DrivingPath(String distance, String duration, List<DrivingStep> steps) {
    }

    private record DrivingStep(String polyline) {
    }

    private record WeatherForecastResponse(List<WeatherForecast> forecasts) {
    }

    private record WeatherForecast(List<WeatherCast> casts) {
    }

    private record WeatherCast(String date,
                               String dayweather,
                               String nightweather,
                               String daytemp,
                               String nighttemp,
                               String daywind,
                               String daypower) {
    }

    private record OpenMeteoForecastResponse(DailyForecast daily) {
    }

    private record DailyForecast(List<String> time,
                                 @JsonProperty("weather_code") List<Integer> weatherCode,
                                 @JsonProperty("temperature_2m_max") List<Double> temperatureMax,
                                 @JsonProperty("temperature_2m_min") List<Double> temperatureMin,
                                 @JsonProperty("wind_direction_10m_dominant") List<Integer> windDirection,
                                 @JsonProperty("wind_speed_10m_max") List<Double> windSpeed) {
    }
}
