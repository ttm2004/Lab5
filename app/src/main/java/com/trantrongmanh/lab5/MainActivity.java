package com.trantrongmanh.lab5;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.trantrongmanh.lab5.util.LocationHelper;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseUser;
import com.trantrongmanh.lab5.auth.AuthManager;
import com.trantrongmanh.lab5.auth.LoginActivity;
import com.trantrongmanh.lab5.news.NewsActivity;
import com.trantrongmanh.lab5.profile.UserProfileActivity;
import com.trantrongmanh.lab5.weather.WeatherFragment;

/**
 * Host activity containing:
 *  - Options Menu (search, refresh, sub-menu settings, about, logout)
 *  - Navigation Drawer (NavigationView)
 *  - Bottom Navigation (weather / news / profile)
 *  - Fragment container hosting WeatherFragment
 */
public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNav;
    private Toolbar toolbar;
    private ActionBarDrawerToggle drawerToggle;

    private WeatherFragment weatherFragment;
    private int currentBottomTab = R.id.nav_weather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupToolbar();
        setupDrawer();
        setupBottomNav();
        updateNavHeader();

        // Default fragment
        if (savedInstanceState == null) {
            loadWeatherFragment();
        }
    }

    // ── View binding ───────────────────────────────────────────────────────────

    private void bindViews() {
        drawerLayout   = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        bottomNav      = findViewById(R.id.bottomNavView);
        toolbar        = findViewById(R.id.toolbar);
    }

    // ── Toolbar ────────────────────────────────────────────────────────────────

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }
    }

    // ── Navigation Drawer ──────────────────────────────────────────────────────

    private void setupDrawer() {
        drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.nav_home, R.string.nav_home);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawers();
            int id = item.getItemId();

            if (id == R.id.drawer_weather) {
                loadWeatherFragment();
                bottomNav.setSelectedItemId(R.id.nav_weather);

            } else if (id == R.id.drawer_news) {
                openNews();

            } else if (id == R.id.drawer_profile) {
                openProfile();

            } else if (id == R.id.drawer_about) {
                showAboutDialog();

            } else if (id == R.id.drawer_logout) {
                confirmLogout();
            }
            return true;
        });
    }

    private void updateNavHeader() {
        View header = navigationView.getHeaderView(0);
        if (header == null) return;

        FirebaseUser user = AuthManager.getInstance().getCurrentUser();
        if (user == null) return;

        TextView tvName  = header.findViewById(R.id.tvNavDisplayName);
        TextView tvEmail = header.findViewById(R.id.tvNavEmail);
        if (tvName  != null) tvName.setText(user.getDisplayName() != null
                ? user.getDisplayName() : "Người dùng");
        if (tvEmail != null) tvEmail.setText(user.getEmail());
    }

    // ── Bottom Navigation ──────────────────────────────────────────────────────

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == currentBottomTab) return false;
            currentBottomTab = id;

            if (id == R.id.nav_weather) {
                loadWeatherFragment();
            } else if (id == R.id.nav_news) {
                openNews();
                // Reset selection back to weather since news is a separate activity
                bottomNav.setSelectedItemId(R.id.nav_weather);
                currentBottomTab = R.id.nav_weather;
            } else if (id == R.id.nav_profile) {
                openProfile();
                bottomNav.setSelectedItemId(R.id.nav_weather);
                currentBottomTab = R.id.nav_weather;
            }
            return true;
        });
    }

    // ── Options Menu ───────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_options, menu);

        // Attach SearchView
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint(getString(R.string.search_city));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    searchView.clearFocus();
                    searchItem.collapseActionView();
                    if (weatherFragment != null) {
                        weatherFragment.searchCity(query.trim());
                    }
                    return true;
                }
                @Override
                public boolean onQueryTextChange(String newText) { return false; }
            });
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            if (weatherFragment != null) weatherFragment.refresh();
            return true;

        } else if (id == R.id.action_share) {
            shareWeather();
            return true;

        } else if (id == R.id.action_change_city) {
            showChangeCityDialog();
            return true;

        } else if (id == R.id.action_units) {
            showUnitsDialog();
            return true;

        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;

        } else if (id == R.id.action_logout) {
            confirmLogout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ── Fragment loading ───────────────────────────────────────────────────────

    private void loadWeatherFragment() {
        if (weatherFragment == null) {
            weatherFragment = new WeatherFragment();
        }
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragmentContainer, weatherFragment)
                .commit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }
    }

    private void replaceFragment(Fragment fragment, String title) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    // ── Navigation helpers ─────────────────────────────────────────────────────

    private void openProfile() {
        startActivity(new Intent(this, UserProfileActivity.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void openNews() {
        startActivity(new Intent(this, NewsActivity.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    // ── Dialogs ────────────────────────────────────────────────────────────────

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout)
                .setMessage("Bạn có chắc muốn đăng xuất?")
                .setPositiveButton(R.string.logout, (d, w) -> doLogout())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void doLogout() {
        AuthManager.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void showChangeCityDialog() {
        if (weatherFragment != null) {
            weatherFragment.showChangeCityDialog();
        }
    }

    private void showUnitsDialog() {
        String[] units = {"Celsius (°C)", "Fahrenheit (°F)", "Kelvin (K)"};
        new AlertDialog.Builder(this)
                .setTitle("Đơn vị nhiệt độ")
                .setItems(units, (dialog, which) -> {
                    // Units preference - passed to fragment
                    if (weatherFragment != null) weatherFragment.setUnitIndex(which);
                })
                .show();
    }

    private void shareWeather() {
        if (weatherFragment == null) return;
        String text = weatherFragment.getShareText();
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(share, "Chia sẻ thời tiết"));
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isOpen()) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    // ── Xử lý kết quả xin quyền vị trí ───────────────────────────────────────

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LocationHelper.REQUEST_LOCATION_PERMISSION) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED;

            if (granted) {
                // Thông báo cho WeatherFragment biết quyền đã được cấp
                if (weatherFragment != null) {
                    weatherFragment.onLocationPermissionGranted();
                }
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Quyền vị trí bị từ chối")
                        .setMessage("Ứng dụng cần quyền vị trí để hiển thị thời tiết tại nơi bạn đang ở. Bạn vẫn có thể tìm kiếm thành phố thủ công.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        }
    }
}
