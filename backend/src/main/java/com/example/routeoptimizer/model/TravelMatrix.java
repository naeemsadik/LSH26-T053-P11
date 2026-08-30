package com.example.routeoptimizer.model;

import java.util.HashMap;
import java.util.Map;

public class TravelMatrix {
    private int defaultSameAreaBufferMinutes = 10;
    private Map<String, Integer> travelTimes = new HashMap<>();

    public TravelMatrix() {
    }

    public TravelMatrix(int defaultSameAreaBufferMinutes, Map<String, Integer> travelTimes) {
        this.defaultSameAreaBufferMinutes = defaultSameAreaBufferMinutes;
        this.travelTimes = travelTimes != null ? travelTimes : new HashMap<>();
    }

    public static String buildKey(Area areaA, Area areaB) {
        if (areaA.name().compareTo(areaB.name()) <= 0) {
            return areaA.name() + "::" + areaB.name();
        } else {
            return areaB.name() + "::" + areaA.name();
        }
    }

    public void setTravelTime(Area areaA, Area areaB, int minutes) {
        if (areaA == areaB) {
            this.defaultSameAreaBufferMinutes = minutes;
        }
        travelTimes.put(buildKey(areaA, areaB), minutes);
    }

    public int getTravelTime(Area areaA, Area areaB) {
        if (areaA == areaB) {
            return travelTimes.getOrDefault(buildKey(areaA, areaB), defaultSameAreaBufferMinutes);
        }
        String key = buildKey(areaA, areaB);
        Integer minutes = travelTimes.get(key);
        if (minutes == null) {
            throw new IllegalArgumentException("No travel time configured between " + areaA + " and " + areaB);
        }
        return minutes;
    }

    public int getDefaultSameAreaBufferMinutes() {
        return defaultSameAreaBufferMinutes;
    }

    public void setDefaultSameAreaBufferMinutes(int defaultSameAreaBufferMinutes) {
        this.defaultSameAreaBufferMinutes = defaultSameAreaBufferMinutes;
    }

    public Map<String, Integer> getTravelTimes() {
        return travelTimes;
    }

    public void setTravelTimes(Map<String, Integer> travelTimes) {
        this.travelTimes = travelTimes;
    }
}
