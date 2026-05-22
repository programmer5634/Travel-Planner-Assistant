package org.example.demo.travel.client.impl;

import org.example.demo.config.TravelApiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RealUnsplashClientTest {

    @Test
    void resolveHotelImageFailsWhenAccessKeyIsDemoKey() {
        RealUnsplashClient client = new RealUnsplashClient(
                WebClient.builder(),
                new TravelApiProperties(
                        new TravelApiProperties.Endpoint("https://restapi.amap.com", "demo-key", true),
                        new TravelApiProperties.Endpoint("https://api.open-meteo.com", "", false),
                        new TravelApiProperties.Unsplash("https://api.unsplash.com", "demo-key")
                )
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> client.resolveHotelImage("杭州", "西湖国宾馆", "市区便利"));

        assertEquals("未配置有效的 Unsplash Access Key：travel.unsplash.access-key", exception.getMessage());
    }
}
