package org.example.demo.travel.gateway.impl;

import org.example.demo.travel.gateway.MapGateway;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class MapGatewayImpl implements MapGateway {

    private final Map<String, CitySeed> cities = new LinkedHashMap<>();

    public MapGatewayImpl() {
        cities.put("hangzhou", new CitySeed(
                new DestinationProfile("Hangzhou", "A lakefront city that suits relaxed cultural walking and food detours.", "West Lake / Hubin", "Use metro for long transfers and split West Lake stops into north and south clusters."),
                List.of(
                        attraction("West Lake", "Nature", "Xihu", "Classic lake views, cycling, and sunset walks.", "2-3 hours", 30.2500, 120.1551, "Nature", "Photography", "Family"),
                        attraction("Lingyin Temple", "History", "Xihu", "Quiet temple grounds with mountain scenery.", "2 hours", 30.2426, 120.1040, "History", "Nature"),
                        attraction("Qinghefang Street", "Food", "Shangcheng", "Traditional street snacks and local crafts.", "1.5 hours", 30.2478, 120.1715, "Food", "Photography"),
                        attraction("Longjing Village", "Nature", "Xihu", "Tea terraces and slower suburban scenery.", "2 hours", 30.2089, 120.0911, "Nature", "Photography"),
                        attraction("Xixi Wetland", "Nature", "Yuhang", "Boat routes and a quieter eco-park circuit.", "3 hours", 30.2724, 120.0636, "Nature", "Family")
                ),
                List.of(
                        hotel("Hubin Lakeside Hotel", "Xihu", "Downtown", "Premium", "Best for first-time visitors who want the easiest night walks.", 30.2560, 120.1638),
                        hotel("Tea Fields Retreat", "Xihu", "Boutique", "Balanced", "A quieter stay close to temple and tea routes.", 30.2148, 120.0970),
                        hotel("West Creek Family Suites", "Yuhang", "Family", "Relaxed", "Larger rooms with easier parking and calmer evenings.", 30.2742, 120.0615)
                )
        ));
        cities.put("chengdu", new CitySeed(
                new DestinationProfile("Chengdu", "A food-forward city with easy pacing, parks, and strong evening atmosphere.", "Taikoo Li / Chunxi Road", "Metro covers the core well; use ride-hailing only for late returns or panda-base departures."),
                List.of(
                        attraction("Chengdu Research Base", "Family", "Chenghua", "Pandas, gardens, and a morning-friendly route.", "3 hours", 30.7335, 104.1527, "Family", "Nature"),
                        attraction("Jinli", "Food", "Wuhou", "Busy snack street and folk-style night scene.", "1.5 hours", 30.6464, 104.0498, "Food", "Nightlife", "Photography"),
                        attraction("Wuhou Shrine", "History", "Wuhou", "Three Kingdoms history with compact walking paths.", "1.5 hours", 30.6458, 104.0491, "History"),
                        attraction("People's Park", "Easy", "Qingyang", "Tea houses and slow local life.", "1.5 hours", 30.6667, 104.0556, "Food", "Family"),
                        attraction("Dongjiao Memory", "Nightlife", "Chenghua", "Creative district for night photos, cafes, and events.", "2 hours", 30.6672, 104.1282, "Nightlife", "Photography")
                ),
                List.of(
                        hotel("Chunxi Urban Club", "Jinjiang", "Downtown", "Balanced", "Strong transit access and easy restaurant density.", 30.6578, 104.0838),
                        hotel("Temple Yard Boutique", "Wuhou", "Boutique", "Premium", "Good for history routes and evening cocktails.", 30.6487, 104.0475),
                        hotel("Panda Family Residence", "Chenghua", "Family", "Relaxed", "Convenient for early panda-base departures.", 30.7031, 104.1236)
                )
        ));
        cities.put("xian", new CitySeed(
                new DestinationProfile("XiAn", "A history-heavy destination best planned around city-wall and museum clusters.", "Bell Tower / South Gate", "Stay inside or near the old wall, and group museum visits by district."),
                List.of(
                        attraction("City Wall", "History", "Beilin", "Wide wall-top cycling with strong skyline views.", "2 hours", 34.2520, 108.9600, "History", "Photography"),
                        attraction("Shaanxi History Museum", "History", "Yanta", "High-value artifact collection, best reserved early.", "2.5 hours", 34.2187, 108.9604, "History"),
                        attraction("Muslim Quarter", "Food", "Lianhu", "Dense local dining corridor with strong night energy.", "2 hours", 34.2653, 108.9469, "Food", "Nightlife"),
                        attraction("Giant Wild Goose Pagoda", "History", "Yanta", "Historic pagoda area with fountains and plazas.", "1.5 hours", 34.2216, 108.9718, "History", "Photography"),
                        attraction("Datang Everbright City", "Nightlife", "Yanta", "Evening promenade with lights and performances.", "2 hours", 34.2159, 108.9752, "Nightlife", "Photography")
                ),
                List.of(
                        hotel("South Gate Heritage House", "Beilin", "Boutique", "Balanced", "Good for wall walks and easy old-city access.", 34.2449, 108.9528),
                        hotel("Bell Tower Grand", "Xincheng", "Downtown", "Premium", "Best positioned for first-time routing efficiency.", 34.2603, 108.9480),
                        hotel("Pagoda Garden Suites", "Yanta", "Family", "Relaxed", "Calmer environment for family pacing.", 34.2198, 108.9698)
                )
        ));
        cities.put("sanya", new CitySeed(
                new DestinationProfile("Sanya", "A resort-style destination that works best with low-density daily planning.", "Dadonghai / Haitang Bay", "Use taxi or hotel shuttle for cross-bay movement and keep beach days light."),
                List.of(
                        attraction("Yalong Bay", "Nature", "Jiyang", "Soft beach, water sports, and resort corridor.", "3 hours", 18.2296, 109.6328, "Nature", "Family"),
                        attraction("Nanshan Cultural Tourism Zone", "History", "Tianya", "Large seafront cultural park with landmark statue.", "3 hours", 18.3026, 109.1558, "History", "Nature"),
                        attraction("Dadonghai Beach", "Nature", "Jiyang", "Convenient urban beach with food and nightlife nearby.", "2 hours", 18.2211, 109.5114, "Nature", "Nightlife"),
                        attraction("Wuzhizhou Island", "Photography", "Haitang", "Clear water and full-day island outing.", "5 hours", 18.3142, 109.7604, "Nature", "Photography"),
                        attraction("Tianya Haijiao", "Photography", "Tianya", "Iconic coastland stop for short scenic walks.", "2 hours", 18.2987, 109.3434, "Photography", "Nature")
                ),
                List.of(
                        hotel("Dadonghai Shoreline", "Jiyang", "Downtown", "Balanced", "Easy restaurant access and shorter beach transfers.", 18.2207, 109.5123),
                        hotel("Haitang Bay Escape", "Haitang", "Resort", "Premium", "Best if the trip is beach-first and slow-paced.", 18.3566, 109.7411),
                        hotel("Yalong Family Villa", "Jiyang", "Family", "Relaxed", "Works well for kids and multi-day pool time.", 18.2330, 109.6309)
                )
        ));
        cities.put("guilin", new CitySeed(
                new DestinationProfile("Guilin", "A scenery-led trip that benefits from early starts and strong weather awareness.", "Two Rivers / Elephant Trunk Hill", "Use one city day and one river or karst day instead of stacking long transfers."),
                List.of(
                        attraction("Elephant Trunk Hill", "Photography", "Xiangshan", "Compact city landmark with river views.", "1.5 hours", 25.2724, 110.2893, "Photography"),
                        attraction("Li River Cruise", "Nature", "Lingchuan", "Classic karst river scenery and photo-heavy pacing.", "4 hours", 25.3080, 110.4170, "Nature", "Photography"),
                        attraction("Reed Flute Cave", "Nature", "Xiufeng", "Short underground route with colorful formations.", "1.5 hours", 25.3032, 110.2592, "Nature", "Family"),
                        attraction("Yangshuo West Street", "Food", "Yangshuo", "Night market energy after a river day.", "2 hours", 24.7786, 110.4959, "Food", "Nightlife"),
                        attraction("Longji Rice Terraces", "Nature", "Longsheng", "Full scenic day with stronger walking demand.", "5 hours", 25.7522, 110.1321, "Nature", "Photography")
                ),
                List.of(
                        hotel("Riverside Karst Hotel", "Xiangshan", "Downtown", "Balanced", "Good base for the first city day.", 25.2714, 110.2900),
                        hotel("Yangshuo Courtyard", "Yangshuo", "Boutique", "Premium", "Best when the trip leans into scenery and night walks.", 24.7768, 110.4945),
                        hotel("Terrace View Lodge", "Longsheng", "Resort", "Relaxed", "Useful for sunrise-focused terrace trips.", 25.7489, 110.1310)
                )
        ));
    }

    @Override
    public DestinationProfile resolveDestination(String destination) {
        return city(destination).destination();
    }

    @Override
    public List<AttractionProfile> searchAttractions(String destination, List<String> interests, int limit) {
        CitySeed city = city(destination);
        List<String> normalizedInterests = interests.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();

        List<AttractionProfile> matched = city.attractions().stream()
                .filter(attraction -> normalizedInterests.isEmpty() || attraction.tags().stream()
                        .map(value -> value.toLowerCase(Locale.ROOT))
                        .anyMatch(normalizedInterests::contains))
                .limit(limit)
                .toList();

        if (matched.size() >= Math.min(limit, 3)) {
            return matched;
        }

        List<AttractionProfile> merged = new ArrayList<>(matched);
        for (AttractionProfile attraction : city.attractions()) {
            if (merged.size() >= limit) {
                break;
            }
            if (!merged.contains(attraction)) {
                merged.add(attraction);
            }
        }
        return merged;
    }

    @Override
    public List<HotelProfile> searchHotels(String destination, String hotelStyle, String budgetLevel, int limit) {
        CitySeed city = city(destination);
        List<HotelProfile> prioritized = city.hotels().stream()
                .sorted((left, right) -> scoreHotel(right, hotelStyle, budgetLevel) - scoreHotel(left, hotelStyle, budgetLevel))
                .limit(limit)
                .toList();
        return prioritized;
    }

    @Override
    public List<WeatherProfile> forecast(String destination, LocalDate startDate, LocalDate endDate) {
        List<WeatherProfile> result = new ArrayList<>();
        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            result.add(new WeatherProfile(
                    date.toString(),
                    seasonCondition(destination, date.getMonthValue()),
                    seasonTemperature(destination, date.getMonthValue()),
                    packingAdvice(destination, date.getMonthValue())
            ));
            date = date.plusDays(1);
        }
        return result;
    }

    private int scoreHotel(HotelProfile hotel, String hotelStyle, String budgetLevel) {
        int score = 0;
        if (hotel.style().equalsIgnoreCase(hotelStyle)) {
            score += 2;
        }
        if (hotel.priceBand().equalsIgnoreCase(budgetLevel)) {
            score += 1;
        }
        return score;
    }

    private CitySeed city(String destination) {
        String key = normalize(destination);
        CitySeed seed = cities.get(key);
        if (seed != null) {
            return seed;
        }
        return cities.get("hangzhou");
    }

    private String normalize(String destination) {
        if (destination == null) {
            return "";
        }
        String normalized = destination.replace("'", "").replace(" ", "").toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "杭州" -> "hangzhou";
            case "成都" -> "chengdu";
            case "西安" -> "xian";
            case "三亚" -> "sanya";
            case "桂林" -> "guilin";
            default -> normalized;
        };
    }

    private String seasonCondition(String destination, int month) {
        if ("sanya".equals(normalize(destination))) {
            return month >= 5 && month <= 10 ? "Warm with showers" : "Sunny and breezy";
        }
        if (month >= 6 && month <= 8) {
            return "Warm with afternoon clouds";
        }
        if (month >= 9 && month <= 11) {
            return "Clear and comfortable";
        }
        if (month >= 12 || month <= 2) {
            return "Cool and dry";
        }
        return "Mild with a chance of light rain";
    }

    private String seasonTemperature(String destination, int month) {
        if ("sanya".equals(normalize(destination))) {
            return month >= 5 && month <= 10 ? "26-31C" : "22-28C";
        }
        if (month >= 6 && month <= 8) {
            return "24-32C";
        }
        if (month >= 9 && month <= 11) {
            return "18-27C";
        }
        if (month >= 12 || month <= 2) {
            return "6-16C";
        }
        return "14-24C";
    }

    private String packingAdvice(String destination, int month) {
        if ("sanya".equals(normalize(destination))) {
            return "Pack sun protection, a light cover-up, and quick-dry clothing.";
        }
        if (month >= 6 && month <= 8) {
            return "Carry breathable layers and keep an umbrella for afternoon showers.";
        }
        if (month >= 12 || month <= 2) {
            return "Bring a light jacket, especially for mornings and night walks.";
        }
        return "Use layered clothing and wear comfortable walking shoes.";
    }

    private AttractionProfile attraction(String name,
                                         String category,
                                         String district,
                                         String highlight,
                                         String recommendedDuration,
                                         double latitude,
                                         double longitude,
                                         String... tags) {
        return new AttractionProfile(name, category, district, district + "核心游览区", highlight, recommendedDuration, latitude, longitude, List.of(tags));
    }

    private HotelProfile hotel(String name,
                               String district,
                               String style,
                               String priceBand,
                               String highlight,
                               double latitude,
                               double longitude) {
        return new HotelProfile(name, district, district + "住宿片区", style, priceBand, highlight, latitude, longitude, "");
    }

    private record CitySeed(
            DestinationProfile destination,
            List<AttractionProfile> attractions,
            List<HotelProfile> hotels
    ) {
    }
}
