package org.example.demo.travel.client.impl;

import org.example.demo.travel.client.UnsplashClient;
import org.example.demo.config.TravelApiProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@Primary
public class RealUnsplashClient implements UnsplashClient {

    private final WebClient.Builder webClientBuilder;
    private final TravelApiProperties properties;

    public RealUnsplashClient(WebClient.Builder webClientBuilder,
                              TravelApiProperties properties) {
        this.webClientBuilder = webClientBuilder;
        this.properties = properties;
    }

    @Override
    public String resolveHotelImage(String destination, String hotelName, String hotelStyle) {
        if (properties.unsplash() == null) {
            throw new IllegalStateException("缺少 Unsplash 配置 travel.unsplash");
        }
        String accessKey = properties.unsplash().accessKey();
        String baseUrl = properties.unsplash().baseUrl();
        if (isDemoKey(accessKey)) {
            throw new IllegalStateException("未配置有效的 Unsplash Access Key：travel.unsplash.access-key");
        }
        if (isBlank(baseUrl)) {
            throw new IllegalStateException("未配置 Unsplash 服务地址 travel.unsplash.base-url");
        }

        Set<String> queries = buildHotelQueries(destination, hotelName, hotelStyle);
        int pickIndex = Math.abs(hotelName.hashCode()) % 10;
        return searchUnsplash(accessKey, baseUrl, queries, 10, pickIndex, destination + " / " + hotelName);
    }

    @Override
    public String resolveSightImage(String destination, String sightName) {
        if (properties.unsplash() == null) {
            throw new IllegalStateException("缺少 Unsplash 配置 travel.unsplash");
        }
        String accessKey = properties.unsplash().accessKey();
        String baseUrl = properties.unsplash().baseUrl();
        if (isDemoKey(accessKey)) {
            throw new IllegalStateException("未配置有效的 Unsplash Access Key：travel.unsplash.access-key");
        }
        if (isBlank(baseUrl)) {
            throw new IllegalStateException("未配置 Unsplash 服务地址 travel.unsplash.base-url");
        }

        Set<String> queries = buildSightQueries(destination, sightName);
        return searchUnsplash(accessKey, baseUrl, queries, 1, 0, destination + " / " + sightName);
    }

    private String searchUnsplash(String accessKey, String baseUrl, Set<String> queries,
                                  int perPage, int pickIndex, String context) {
        RuntimeException lastException = null;

        for (String query : queries) {
            try {
                UnsplashSearchResponse response = webClientBuilder.baseUrl(baseUrl)
                        .build()
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/search/photos")
                                .queryParam("query", query)
                                .queryParam("page", 1)
                                .queryParam("per_page", perPage)
                                .queryParam("orientation", "landscape")
                                .build())
                        .header("Authorization", "Client-ID " + accessKey)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, responseSpec -> responseSpec.createException().flatMap(reactor.core.publisher.Mono::error))
                        .bodyToMono(UnsplashSearchResponse.class)
                        .block();
                String imageUrl = extractImageUrl(response, pickIndex);
                if (imageUrl != null) {
                    return imageUrl;
                }
            } catch (RuntimeException exception) {
                lastException = exception;
            }
        }

        if (lastException != null) {
            throw new IllegalStateException("Unsplash 图片检索失败：" + context, lastException);
        }
        throw new IllegalStateException("Unsplash 未返回配图：" + context);
    }

    private Set<String> buildHotelQueries(String destination, String hotelName, String hotelStyle) {
        Set<String> queries = new LinkedHashSet<>();
        String englishDestination = toEnglishDestination(destination);
        int nameHash = Math.abs(hotelName.hashCode());
        String[] styleKeywords = {"luxury", "boutique", "modern", "cozy", "elegant"};
        String keyword = styleKeywords[nameHash % styleKeywords.length];
        queries.add(englishDestination + " " + keyword + " hotel");
        queries.add(englishDestination + " " + hotelStyle + " hotel");
        queries.add(englishDestination + " hotel");
        queries.add(englishDestination + " resort");
        queries.add(keyword + " hotel interior");
        queries.add("hotel lobby");
        return queries;
    }

    private Set<String> buildSightQueries(String destination, String sightName) {
        Set<String> queries = new LinkedHashSet<>();
        String englishDestination = toEnglishDestination(destination);
        queries.add(destination + " " + sightName);
        queries.add(sightName + " " + destination);
        queries.add(englishDestination + " " + sightName);
        queries.add(destination + " travel scenery");
        queries.add(englishDestination + " travel");
        queries.add("travel scenery");
        return queries;
    }

    private String toEnglishDestination(String destination) {
        if (destination == null) {
            return "travel";
        }
        return switch (destination.replace("'", "").replace(" ", "").toLowerCase(Locale.ROOT)) {
            case "杭州", "hangzhou" -> "Hangzhou";
            case "成都", "chengdu" -> "Chengdu";
            case "西安", "xian", "xi'an" -> "Xian";
            case "三亚", "sanya" -> "Sanya";
            case "桂林", "guilin" -> "Guilin";
            case "上海", "shanghai" -> "Shanghai";
            case "北京", "beijing" -> "Beijing";
            default -> destination;
        };
    }

    private String extractImageUrl(UnsplashSearchResponse response, int pickIndex) {
        if (response == null || response.results() == null || response.results().isEmpty()) {
            return null;
        }
        int index = Math.min(pickIndex, response.results().size() - 1);
        Result picked = response.results().get(index);
        if (picked == null || picked.urls() == null) {
            return null;
        }
        return picked.urls().regular();
    }

    private boolean isDemoKey(String value) {
        return isBlank(value) || "demo-key".equals(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record UnsplashSearchResponse(List<Result> results) {
    }

    private record Result(Urls urls) {
    }

    private record Urls(String regular, String full, String small, String thumb) {
    }
}
