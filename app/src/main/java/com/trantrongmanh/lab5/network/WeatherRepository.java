package com.trantrongmanh.lab5.network;

import androidx.lifecycle.MutableLiveData;

import com.trantrongmanh.lab5.BuildConfig;
import com.trantrongmanh.lab5.model.ForecastResponse;
import com.trantrongmanh.lab5.model.WeatherResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Single source of truth for weather data.
 * Activities / Fragments observe LiveData returned from here.
 */
public class WeatherRepository {

    private static WeatherRepository instance;
    private final WeatherApiService api;
    private static final String LANG = "vi"; // Vietnamese descriptions

    private WeatherRepository() {
        api = RetrofitClient.getInstance().getWeatherApiService();
    }

    public static synchronized WeatherRepository getInstance() {
        if (instance == null) instance = new WeatherRepository();
        return instance;
    }

    // ── Current weather ────────────────────────────────────────────────────────

    public MutableLiveData<WeatherResponse> fetchCurrentWeather(String city) {
        MutableLiveData<WeatherResponse> liveData = new MutableLiveData<>();
        api.getCurrentWeatherByCity(city, BuildConfig.WEATHER_API_KEY, LANG)
                .enqueue(new Callback<WeatherResponse>() {
                    @Override
                    public void onResponse(Call<WeatherResponse> call,
                                           Response<WeatherResponse> response) {
                        if (response.isSuccessful()) {
                            liveData.postValue(response.body());
                        } else {
                            liveData.postValue(null);
                        }
                    }
                    @Override
                    public void onFailure(Call<WeatherResponse> call, Throwable t) {
                        liveData.postValue(null);
                    }
                });
        return liveData;
    }

    public MutableLiveData<WeatherResponse> fetchCurrentWeatherByCoords(double lat, double lon) {
        MutableLiveData<WeatherResponse> liveData = new MutableLiveData<>();
        api.getCurrentWeatherByCoords(lat, lon, BuildConfig.WEATHER_API_KEY, LANG)
                .enqueue(new Callback<WeatherResponse>() {
                    @Override
                    public void onResponse(Call<WeatherResponse> call,
                                           Response<WeatherResponse> response) {
                        liveData.postValue(response.isSuccessful() ? response.body() : null);
                    }
                    @Override
                    public void onFailure(Call<WeatherResponse> call, Throwable t) {
                        liveData.postValue(null);
                    }
                });
        return liveData;
    }

    // ── Forecast ───────────────────────────────────────────────────────────────

    public MutableLiveData<ForecastResponse> fetchForecast(String city) {
        MutableLiveData<ForecastResponse> liveData = new MutableLiveData<>();
        // cnt=40 → full 5-day/3-hour list
        api.getForecastByCity(city, BuildConfig.WEATHER_API_KEY, LANG, 40)
                .enqueue(new Callback<ForecastResponse>() {
                    @Override
                    public void onResponse(Call<ForecastResponse> call,
                                           Response<ForecastResponse> response) {
                        liveData.postValue(response.isSuccessful() ? response.body() : null);
                    }
                    @Override
                    public void onFailure(Call<ForecastResponse> call, Throwable t) {
                        liveData.postValue(null);
                    }
                });
        return liveData;
    }
}
