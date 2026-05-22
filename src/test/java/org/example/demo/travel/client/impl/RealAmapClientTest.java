package org.example.demo.travel.client.impl;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.example.demo.config.TravelApiProperties;
import org.example.demo.travel.gateway.MapGateway;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RealAmapClientTest {

    @Test
    void resolveDestinationFailsWhenApiKeyIsDemoKey() {
        RealAmapClient client = new RealAmapClient(
                WebClient.builder(),
                properties("https://restapi.amap.com", "https://api.open-meteo.com", "demo-key", true)
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> client.resolveDestination("杭州"));

        assertEquals("未配置有效的高德 API Key：travel.amap.api-key", exception.getMessage());
    }

    @Test
    void searchAttractionsFailsWhenApiKeyIsMissing() {
        RealAmapClient client = new RealAmapClient(
                WebClient.builder(),
                properties("https://restapi.amap.com", "https://api.open-meteo.com", "", true)
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> client.searchAttractions("杭州", List.of("自然风光"), 3));

        assertEquals("未配置有效的高德 API Key：travel.amap.api-key", exception.getMessage());
    }

    @Test
    void forecastFailsWhenWeatherIsDisabled() {
        RealAmapClient client = new RealAmapClient(
                WebClient.builder(),
                properties("https://restapi.amap.com", "https://api.open-meteo.com", "test-key", false)
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> client.forecast("杭州", LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-02")));

        assertEquals("天气真实调用已关闭，请启用 travel.weather.enabled", exception.getMessage());
    }

    @Test
    void forecastReturnsDistinctDailyForecastsFromOpenMeteo() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v3/geocode/geo", exchange -> writeJson(exchange, """
                {
                  "geocodes": [
                    {
                      "adcode": "330100",
                      "location": "120.155070,30.274084"
                    }
                  ]
                }
                """));
        server.createContext("/v1/forecast", exchange -> writeJson(exchange, """
                {
                  "daily": {
                    "time": ["2026-06-01", "2026-06-02", "2026-06-03"],
                    "weather_code": [0, 61, 3],
                    "temperature_2m_max": [31, 28, 27],
                    "temperature_2m_min": [23, 22, 21],
                    "wind_direction_10m_dominant": [45, 90, 0],
                    "wind_speed_10m_max": [18, 24, 12]
                  }
                }
                """));
        server.start();

        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            RealAmapClient client = new RealAmapClient(
                    WebClient.builder(),
                    properties(baseUrl, baseUrl, "test-key", true)
            );

            List<MapGateway.WeatherProfile> forecast = client.forecast(
                    "杭州",
                    LocalDate.parse("2026-06-01"),
                    LocalDate.parse("2026-06-03")
            );

            assertEquals(3, forecast.size());
            assertEquals("2026-06-01", forecast.get(0).date());
            assertEquals("晴", forecast.get(0).condition());
            assertEquals("23-31C", forecast.get(0).temperature());
            assertEquals("建议穿舒适步行鞋，按早晚温差准备薄外套。 当前主导风向东北，最大风速约18km/h。", forecast.get(0).advice());
            assertEquals("降雨", forecast.get(1).condition());
            assertEquals("22-28C", forecast.get(1).temperature());
            assertEquals("阴", forecast.get(2).condition());
            assertEquals("21-27C", forecast.get(2).temperature());
        } finally {
            server.stop(0);
        }
    }

    private TravelApiProperties properties(String amapBaseUrl, String weatherBaseUrl, String amapApiKey, boolean weatherEnabled) {
        return new TravelApiProperties(
                new TravelApiProperties.Endpoint(amapBaseUrl, amapApiKey, true),
                new TravelApiProperties.Endpoint(weatherBaseUrl, "", weatherEnabled),
                new TravelApiProperties.Unsplash("https://api.unsplash.com", "demo-key")
        );
    }

    private static void writeJson(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
