package com.trantrongmanh.lab5.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Maps the /forecast endpoint (5-day / 3-hour) from OpenWeatherMap.
 */
public class ForecastResponse {

    @SerializedName("list")
    private List<ForecastItem> list;

    @SerializedName("city")
    private City city;

    public List<ForecastItem> getList() { return list; }
    public City getCity() { return city; }

    // ── Inner classes ──────────────────────────────────────────────────────────

    public static class ForecastItem {
        @SerializedName("dt")      private long timestamp;
        @SerializedName("main")    private WeatherResponse.Main main;
        @SerializedName("weather") private List<WeatherResponse.Weather> weatherList;
        @SerializedName("wind")    private WeatherResponse.Wind wind;
        @SerializedName("dt_txt")  private String dtTxt;

        public long getTimestamp() { return timestamp; }
        public WeatherResponse.Main getMain() { return main; }
        public List<WeatherResponse.Weather> getWeatherList() { return weatherList; }
        public WeatherResponse.Wind getWind() { return wind; }
        public String getDtTxt() { return dtTxt; }

        public String getWeatherIcon() {
            if (weatherList != null && !weatherList.isEmpty()) {
                return weatherList.get(0).getIcon();
            }
            return "01d";
        }

        public String getIconUrl() {
            return "https://openweathermap.org/img/wn/" + getWeatherIcon() + "@2x.png";
        }

        public String getDescription() {
            if (weatherList != null && !weatherList.isEmpty()) {
                String d = weatherList.get(0).getDescription();
                return d.substring(0, 1).toUpperCase() + d.substring(1);
            }
            return "";
        }
    }

    public static class City {
        @SerializedName("name")    private String name;
        @SerializedName("country") private String country;
        @SerializedName("sunrise") private long sunrise;
        @SerializedName("sunset")  private long sunset;

        public String getName() { return name; }
        public String getCountry() { return country; }
        public long getSunrise() { return sunrise; }
        public long getSunset() { return sunset; }
    }
}
