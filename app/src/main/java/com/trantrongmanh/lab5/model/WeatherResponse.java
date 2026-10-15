package com.trantrongmanh.lab5.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class WeatherResponse {

    @SerializedName("name")
    private String cityName;

    @SerializedName("weather")
    private List<Weather> weatherList;

    @SerializedName("main")
    private Main main;

    @SerializedName("wind")
    private Wind wind;

    @SerializedName("sys")
    private Sys sys;

    @SerializedName("visibility")
    private int visibility;

    @SerializedName("dt")
    private long timestamp;

    @SerializedName("clouds")
    private Clouds clouds;

    // Getters
    public String getCityName() { return cityName; }
    public List<Weather> getWeatherList() { return weatherList; }
    public Main getMain() { return main; }
    public Wind getWind() { return wind; }
    public Sys getSys() { return sys; }
    public int getVisibility() { return visibility; }
    public long getTimestamp() { return timestamp; }
    public Clouds getClouds() { return clouds; }

    public String getWeatherDescription() {
        if (weatherList != null && !weatherList.isEmpty()) {
            String desc = weatherList.get(0).getDescription();
            return desc.substring(0, 1).toUpperCase() + desc.substring(1);
        }
        return "";
    }

    public String getWeatherIcon() {
        if (weatherList != null && !weatherList.isEmpty()) {
            return weatherList.get(0).getIcon();
        }
        return "01d";
    }

    public String getIconUrl() {
        return "https://openweathermap.org/img/wn/" + getWeatherIcon() + "@2x.png";
    }

    // ── Inner classes ──────────────────────────────────────────────────────────

    public static class Weather {
        @SerializedName("id")      private int id;
        @SerializedName("main")    private String main;
        @SerializedName("description") private String description;
        @SerializedName("icon")    private String icon;

        public int getId() { return id; }
        public String getMain() { return main; }
        public String getDescription() { return description; }
        public String getIcon() { return icon; }
    }

    public static class Main {
        @SerializedName("temp")       private double temp;
        @SerializedName("feels_like") private double feelsLike;
        @SerializedName("temp_min")   private double tempMin;
        @SerializedName("temp_max")   private double tempMax;
        @SerializedName("pressure")   private int pressure;
        @SerializedName("humidity")   private int humidity;

        public double getTemp() { return temp; }
        public double getFeelsLike() { return feelsLike; }
        public double getTempMin() { return tempMin; }
        public double getTempMax() { return tempMax; }
        public int getPressure() { return pressure; }
        public int getHumidity() { return humidity; }

        public String getTempCelsius() {
            return String.format("%.0f°C", temp - 273.15);
        }
        public String getFeelsLikeCelsius() {
            return String.format("%.0f°C", feelsLike - 273.15);
        }
        public String getTempMinCelsius() {
            return String.format("%.0f°C", tempMin - 273.15);
        }
        public String getTempMaxCelsius() {
            return String.format("%.0f°C", tempMax - 273.15);
        }
    }

    public static class Wind {
        @SerializedName("speed") private double speed;
        @SerializedName("deg")   private int deg;

        public double getSpeed() { return speed; }
        public int getDeg() { return deg; }
        public String getSpeedKmh() {
            return String.format("%.1f km/h", speed * 3.6);
        }
    }

    public static class Sys {
        @SerializedName("country") private String country;
        @SerializedName("sunrise") private long sunrise;
        @SerializedName("sunset")  private long sunset;

        public String getCountry() { return country; }
        public long getSunrise() { return sunrise; }
        public long getSunset() { return sunset; }
    }

    public static class Clouds {
        @SerializedName("all") private int all;
        public int getAll() { return all; }
    }
}
