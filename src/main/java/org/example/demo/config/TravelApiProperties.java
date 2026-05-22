package org.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "travel")
public record TravelApiProperties(
        Endpoint amap,
        Endpoint weather,
        Unsplash unsplash
) {

    public record Endpoint(
            String baseUrl,
            String apiKey,
            Boolean enabled
    ) {
    }

    public record Unsplash(
            String baseUrl,
            String accessKey
    ) {
    }
}
