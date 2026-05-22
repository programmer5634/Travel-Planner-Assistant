package org.example.demo.travel.client.impl;

import org.example.demo.travel.client.UnsplashClient;
import org.example.demo.travel.gateway.impl.PhotoGatewayImpl;
import org.springframework.stereotype.Component;

@Component
public class StubUnsplashClient implements UnsplashClient {

    private final PhotoGatewayImpl delegate;

    public StubUnsplashClient(PhotoGatewayImpl delegate) {
        this.delegate = delegate;
    }

    @Override
    public String resolveHotelImage(String destination, String hotelName, String hotelStyle) {
        return delegate.resolveHotelImage(destination, hotelName, hotelStyle);
    }

    @Override
    public String resolveSightImage(String destination, String sightName) {
        return delegate.resolveSightImage(destination, sightName);
    }
}
