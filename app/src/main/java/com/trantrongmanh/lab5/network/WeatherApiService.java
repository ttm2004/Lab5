package com.trantrongmanh.lab5.network;

import com.trantrongmanh.lab5.model.ForecastResponse;
import com.trantrongmanh.lab5.model.WeatherResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {

    /**
     * Current weather by city name.
     * e.g. GET /weather?q=Hanoi&appid=KEY
     */
    @GET("weather")
    Call<WeatherResponse> getCurrentWeatherByCity(
            @Query("q")     String cityName,
            @Query("appid") String apiKey,
            @Query("lang")  String lang
    );

    /**
     * Current weather by geographic coordinates.
     */
    @GET("weather")
    Call<WeatherResponse> getCurrentWeatherByCoords(
            @Query("lat")   double lat,
            @Query("lon")   double lon,
            @Query("appid") String apiKey,
            @Query("lang")  String lang
    );

    /**
     * 5-day / 3-hour forecast by city name.
     */
    @GET("forecast")
    Call<ForecastResponse> getForecastByCity(
            @Query("q")     String cityName,
            @Query("appid") String apiKey,
            @Query("lang")  String lang,
            @Query("cnt")   int count
    );

    /**
     * 5-day / 3-hour forecast by coordinates.
     */
    @GET("forecast")
    Call<ForecastResponse> getForecastByCoords(
            @Query("lat")   double lat,
            @Query("lon")   double lon,
            @Query("appid") String apiKey,
            @Query("lang")  String lang,
            @Query("cnt")   int count
    );
}
