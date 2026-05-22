package org.example.demo.travel.gateway.impl;

import org.example.demo.travel.gateway.PhotoGateway;
import org.springframework.stereotype.Component;

@Component
public class PhotoGatewayImpl implements PhotoGateway {

    @Override
    public String resolveHotelImage(String destination, String hotelName, String hotelStyle) {
        String seed = (destination + "-" + hotelName).replace(' ', '-').toLowerCase();
        return "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80&travel=" + seed;
    }

    @Override
    public String resolveSightImage(String destination, String sightName) {
        String seed = (destination + "-" + sightName).replace(' ', '-').toLowerCase();
        return "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?auto=format&fit=crop&w=1200&q=80&sight=" + seed;
    }
}
