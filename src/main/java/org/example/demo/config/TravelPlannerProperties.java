package org.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "travel")
public class TravelPlannerProperties {

    private List<String> featuredDestinations = new ArrayList<>();
    private List<String> interests = new ArrayList<>();
    private List<String> budgetLevels = new ArrayList<>();
    private List<String> paceOptions = new ArrayList<>();
    private List<String> hotelStyles = new ArrayList<>();

    public List<String> getFeaturedDestinations() {
        return featuredDestinations;
    }

    public void setFeaturedDestinations(List<String> featuredDestinations) {
        this.featuredDestinations = featuredDestinations;
    }

    public List<String> getInterests() {
        return interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests;
    }

    public List<String> getBudgetLevels() {
        return budgetLevels;
    }

    public void setBudgetLevels(List<String> budgetLevels) {
        this.budgetLevels = budgetLevels;
    }

    public List<String> getPaceOptions() {
        return paceOptions;
    }

    public void setPaceOptions(List<String> paceOptions) {
        this.paceOptions = paceOptions;
    }

    public List<String> getHotelStyles() {
        return hotelStyles;
    }

    public void setHotelStyles(List<String> hotelStyles) {
        this.hotelStyles = hotelStyles;
    }
}
