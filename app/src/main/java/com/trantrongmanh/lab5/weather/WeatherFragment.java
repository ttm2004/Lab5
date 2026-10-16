package com.trantrongmanh.lab5.weather;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.trantrongmanh.lab5.R;
import com.trantrongmanh.lab5.model.ForecastResponse;
import com.trantrongmanh.lab5.model.WeatherResponse;
import com.trantrongmanh.lab5.network.WeatherRepository;
import com.trantrongmanh.lab5.util.LocationHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeatherFragment extends Fragment {

    private static final String PREFS_NAME   = "weather_prefs";
    private static final String KEY_CITY     = "last_city";
    private static final String KEY_UNIT_IDX = "unit_index";

    // 0=Celsius, 1=Fahrenheit, 2=Kelvin
    private int unitIndex = 0;

    private SwipeRefreshLayout swipeRefresh;
    private EditText etCitySearch;
    private ImageButton btnSearch;
    private ImageButton btnLocation; // nút GPS mới
    private ImageView ivWeatherIcon;
    private TextView tvCityName, tvDate, tvTemperature, tvDescription;
    private TextView tvTempMax, tvTempMin;
    private TextView tvSunrise, tvSunset;
    private TextView tvError;
    private RecyclerView rvForecast;

    private View detailHumidity, detailWind, detailFeelsLike;
    private View detailPressure, detailVisibility, detailClouds;

    private ForecastAdapter forecastAdapter;
    private WeatherRepository repository;
    private SharedPreferences prefs;
    private LocationHelper locationHelper;

    private String currentCity = "Ho Chi Minh City";
    private WeatherResponse lastWeatherResponse;

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("vi", "VN"));
    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_weather, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository     = WeatherRepository.getInstance();
        locationHelper = new LocationHelper(requireContext());
        prefs          = requireContext().getSharedPreferences(PREFS_NAME, 0);
        currentCity    = prefs.getString(KEY_CITY, "Ho Chi Minh City");
        unitIndex      = prefs.getInt(KEY_UNIT_IDX, 0);

        bindViews(view);
        setupRecyclerView();
        setupSearch();
        setupSwipeRefresh();
        setupLocationButton();
        loadWeather();
    }

    // ── View binding ───────────────────────────────────────────────────────────

    private void bindViews(View v) {
        swipeRefresh    = v.findViewById(R.id.swipeRefresh);
        etCitySearch    = v.findViewById(R.id.etCitySearch);
        btnSearch       = v.findViewById(R.id.btnSearch);
        btnLocation     = v.findViewById(R.id.btnLocation);
        ivWeatherIcon   = v.findViewById(R.id.ivWeatherIcon);
        tvCityName      = v.findViewById(R.id.tvCityName);
        tvDate          = v.findViewById(R.id.tvDate);
        tvTemperature   = v.findViewById(R.id.tvTemperature);
        tvDescription   = v.findViewById(R.id.tvDescription);
        tvTempMax       = v.findViewById(R.id.tvTempMax);
        tvTempMin       = v.findViewById(R.id.tvTempMin);
        tvSunrise       = v.findViewById(R.id.tvSunrise);
        tvSunset        = v.findViewById(R.id.tvSunset);
        tvError         = v.findViewById(R.id.tvError);
        rvForecast      = v.findViewById(R.id.rvForecast);

        detailHumidity   = v.findViewById(R.id.detailHumidity);
        detailWind       = v.findViewById(R.id.detailWind);
        detailFeelsLike  = v.findViewById(R.id.detailFeelsLike);
        detailPressure   = v.findViewById(R.id.detailPressure);
        detailVisibility = v.findViewById(R.id.detailVisibility);
        detailClouds     = v.findViewById(R.id.detailClouds);
    }

    private void setupRecyclerView() {
        forecastAdapter = new ForecastAdapter(requireContext());
        rvForecast.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvForecast.setAdapter(forecastAdapter);
        rvForecast.setNestedScrollingEnabled(false);
    }

    private void setupSearch() {
        btnSearch.setOnClickListener(v -> performSearch());
        etCitySearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);
        swipeRefresh.setOnRefreshListener(this::loadWeather);
    }

    // ── Nút lấy vị trí GPS ────────────────────────────────────────────────────

    private void setupLocationButton() {
        if (btnLocation == null) return;

        btnLocation.setOnClickListener(v -> {
            if (locationHelper.hasLocationPermission()) {
                fetchWeatherByLocation();
            } else {
                // Xin quyền
                if (getActivity() != null) {
                    locationHelper.requestLocationPermission(getActivity());
                }
            }
        });
    }

    /**
     * Gọi sau khi user cấp quyền (từ Activity).
     */
    public void onLocationPermissionGranted() {
        fetchWeatherByLocation();
    }

    private void fetchWeatherByLocation() {
        swipeRefresh.setRefreshing(true);
        setError(false);

        Snackbar.make(requireView(), "Đang lấy vị trí GPS...", Snackbar.LENGTH_SHORT).show();

        locationHelper.getCurrentLocation(new LocationHelper.LocationCallback2() {
            @Override
            public void onLocationReceived(double lat, double lon, String cityName) {
                // Lấy tên thành phố tiếng Anh để query API
                String cityEn = locationHelper.getCityNameEnglish(lat, lon);
                currentCity = cityEn;
                prefs.edit().putString(KEY_CITY, cityEn).apply();

                // Cập nhật ô tìm kiếm
                etCitySearch.setText(cityName); // hiển thị tên tiếng Việt

                // Load thời tiết theo tọa độ
                loadWeatherByCoords(lat, lon);
            }

            @Override
            public void onLocationError(String message) {
                swipeRefresh.setRefreshing(false);
                Snackbar.make(requireView(),
                        "Không lấy được vị trí: " + message,
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void loadWeatherByCoords(double lat, double lon) {
        repository.fetchCurrentWeatherByCoords(lat, lon)
                .observe(getViewLifecycleOwner(), response -> {
                    if (response != null) {
                        lastWeatherResponse = response;
                        populateCurrentWeather(response);
                        // Cập nhật tên thành phố từ API response
                        currentCity = response.getCityName();
                        prefs.edit().putString(KEY_CITY, currentCity).apply();
                    } else {
                        showError();
                    }
                    swipeRefresh.setRefreshing(false);
                });

        repository.fetchForecastByCoords(lat, lon)
                .observe(getViewLifecycleOwner(), forecast -> {
                    if (forecast != null && forecast.getList() != null) {
                        forecastAdapter.setItems(filterDailyForecast(forecast.getList()));
                    }
                });
    }

    // ── Data loading ───────────────────────────────────────────────────────────

    private void performSearch() {
        String city = etCitySearch.getText() != null
                ? etCitySearch.getText().toString().trim() : "";
        if (!city.isEmpty()) {
            searchCity(city);
            hideKeyboard();
        }
    }

    public void searchCity(String city) {
        currentCity = city;
        prefs.edit().putString(KEY_CITY, city).apply();
        loadWeather();
    }

    public void refresh() {
        loadWeather();
    }

    private void loadWeather() {
        setError(false);
        swipeRefresh.setRefreshing(true);

        repository.fetchCurrentWeather(currentCity).observe(getViewLifecycleOwner(), response -> {
            if (response != null) {
                lastWeatherResponse = response;
                populateCurrentWeather(response);
            } else {
                showError();
            }
            swipeRefresh.setRefreshing(false);
        });

        repository.fetchForecast(currentCity).observe(getViewLifecycleOwner(), forecast -> {
            if (forecast != null && forecast.getList() != null) {
                forecastAdapter.setItems(filterDailyForecast(forecast.getList()));
            }
        });
    }

    private List<ForecastResponse.ForecastItem> filterDailyForecast(
            List<ForecastResponse.ForecastItem> list) {
        List<ForecastResponse.ForecastItem> daily = new ArrayList<>();
        String lastDay = "";
        for (ForecastResponse.ForecastItem item : list) {
            String day = item.getDtTxt().substring(0, 10);
            if (!day.equals(lastDay)) {
                daily.add(item);
                lastDay = day;
                if (daily.size() == 5) break;
            }
        }
        return daily;
    }

    // ── UI population ──────────────────────────────────────────────────────────

    private void populateCurrentWeather(WeatherResponse r) {
        String city = r.getCityName();
        if (r.getSys() != null && r.getSys().getCountry() != null) {
            city += ", " + r.getSys().getCountry();
        }
        tvCityName.setText(city);
        tvDate.setText(DATE_FORMAT.format(new Date()));
        tvTemperature.setText(formatTemp(r.getMain().getTemp()));
        tvDescription.setText(r.getWeatherDescription());
        tvTempMax.setText("↑ " + formatTemp(r.getMain().getTempMax()));
        tvTempMin.setText("↓ " + formatTemp(r.getMain().getTempMin()));

        Glide.with(requireContext())
                .load(r.getIconUrl())
                .placeholder(android.R.mipmap.sym_def_app_icon)
                .into(ivWeatherIcon);

        if (r.getSys() != null) {
            tvSunrise.setText(TIME_FORMAT.format(new Date(r.getSys().getSunrise() * 1000L)));
            tvSunset.setText(TIME_FORMAT.format(new Date(r.getSys().getSunset()  * 1000L)));
        }

        if (r.getMain() != null) {
            setDetail(detailHumidity,  "💧", getString(R.string.humidity),
                    r.getMain().getHumidity() + "%");
            setDetail(detailFeelsLike, "🌡", getString(R.string.feels_like),
                    formatTemp(r.getMain().getFeelsLike()));
            setDetail(detailPressure,  "📊", getString(R.string.pressure),
                    r.getMain().getPressure() + " hPa");
        }
        if (r.getWind() != null) {
            setDetail(detailWind, "💨", getString(R.string.wind_speed),
                    r.getWind().getSpeedKmh());
        }
        setDetail(detailVisibility, "👁", getString(R.string.visibility),
                (r.getVisibility() / 1000) + " km");
        if (r.getClouds() != null) {
            setDetail(detailClouds, "☁", getString(R.string.cloudiness),
                    r.getClouds().getAll() + "%");
        }

        Animation anim = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in);
        requireView().findViewById(R.id.cardCurrentWeather).startAnimation(anim);
    }

    private void setDetail(View cell, String icon, String label, String value) {
        if (cell == null) return;
        ((TextView) cell.findViewById(R.id.tvDetailIcon)).setText(icon);
        ((TextView) cell.findViewById(R.id.tvDetailLabel)).setText(label);
        ((TextView) cell.findViewById(R.id.tvDetailValue)).setText(value);
    }

    // ── Nhiệt độ ──────────────────────────────────────────────────────────────

    private String formatTemp(double kelvin) {
        switch (unitIndex) {
            case 1: return String.format(Locale.getDefault(), "%.0f°F", (kelvin - 273.15) * 9 / 5 + 32);
            case 2: return String.format(Locale.getDefault(), "%.0f K", kelvin);
            default: return String.format(Locale.getDefault(), "%.0f°C", kelvin - 273.15);
        }
    }

    public void setUnitIndex(int idx) {
        unitIndex = idx;
        prefs.edit().putInt(KEY_UNIT_IDX, idx).apply();
        if (lastWeatherResponse != null) populateCurrentWeather(lastWeatherResponse);
    }

    // ── Error ──────────────────────────────────────────────────────────────────

    private void showError() {
        swipeRefresh.setRefreshing(false);
        setError(true);
        Snackbar.make(requireView(), R.string.error_loading, Snackbar.LENGTH_LONG)
                .setAction(R.string.refresh, v -> loadWeather())
                .show();
    }

    private void setError(boolean show) {
        tvError.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // ── Public API cho MainActivity ────────────────────────────────────────────

    public void showChangeCityDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_city, null);
        TextInputEditText etCity = dialogView.findViewById(R.id.etDialogCity);
        etCity.setText(currentCity);

        new AlertDialog.Builder(requireContext())
                .setTitle("Đổi thành phố")
                .setView(dialogView)
                .setPositiveButton("Tìm kiếm", (d, w) -> {
                    String city = etCity.getText() != null
                            ? etCity.getText().toString().trim() : "";
                    if (!city.isEmpty()) searchCity(city);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    public String getShareText() {
        if (lastWeatherResponse == null) return getString(R.string.app_name);
        WeatherResponse r = lastWeatherResponse;
        return getString(R.string.app_name) + "\n"
                + r.getCityName() + "\n"
                + r.getWeatherDescription() + "\n"
                + formatTemp(r.getMain().getTemp()) + "\n"
                + "Độ ẩm: " + r.getMain().getHumidity() + "%\n"
                + "Gió: " + r.getWind().getSpeedKmh();
    }

    // ── Bàn phím ──────────────────────────────────────────────────────────────

    private void hideKeyboard() {
        if (getActivity() == null) return;
        InputMethodManager imm = (InputMethodManager)
                getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        View focused = getActivity().getCurrentFocus();
        if (focused != null && imm != null) {
            imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }
}
