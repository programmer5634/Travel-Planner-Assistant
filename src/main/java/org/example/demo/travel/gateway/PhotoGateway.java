package org.example.demo.travel.gateway;

public interface PhotoGateway {

    String resolveHotelImage(String destination, String hotelName, String hotelStyle);

    String resolveSightImage(String destination, String sightName);
}
