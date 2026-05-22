package org.example.demo.travel.client;

public interface UnsplashClient {

    String resolveHotelImage(String destination, String hotelName, String hotelStyle);

    String resolveSightImage(String destination, String sightName);
}
