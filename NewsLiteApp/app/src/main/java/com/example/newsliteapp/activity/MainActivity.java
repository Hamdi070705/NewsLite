package com.example.newsliteapp.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.newsliteapp.R;
import com.example.newsliteapp.ui.HomeFragment;
import com.example.newsliteapp.ui.SavedFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private BottomNavigationView bottomNavigationView;
    private static final String TAG_HOME_FRAGMENT = "HOME_FRAGMENT";
    private static final String TAG_SAVED_FRAGMENT = "SAVED_FRAGMENT";

    // Konstanta untuk menyimpan preferensi tema
    private static final String PREFS_NAME = "ThemePrefs";
    private static final String KEY_THEME = "theme_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // PENTING: Terapkan tema yang tersimpan SEBELUM setContentView()
        applyTheme();

        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), TAG_HOME_FRAGMENT);
        }
    }

    /**
     * Metode untuk menerapkan tema yang tersimpan.
     * Dipanggil di awal onCreate.
     */
    private void applyTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        // Defaultnya adalah mengikuti sistem (MODE_NIGHT_FOLLOW_SYSTEM)
        int savedTheme = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        // Set tema default untuk seluruh aplikasi
        AppCompatDelegate.setDefaultNightMode(savedTheme);
    }

    /**
     * Method baru untuk toggle dark mode
     */
    public void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int currentTheme = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        int newTheme;
        // Tentukan tema baru berdasarkan tema saat ini
        if (currentTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            newTheme = AppCompatDelegate.MODE_NIGHT_NO;
        } else {
            newTheme = AppCompatDelegate.MODE_NIGHT_YES;
        }

        // Simpan tema baru ke SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_THEME, newTheme);
        editor.apply();

        // Terapkan tema baru
        AppCompatDelegate.setDefaultNightMode(newTheme);
    }

    /**
     * Method untuk mendapatkan tema saat ini
     */
    public int getCurrentTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    /**
     * Method untuk mengecek apakah sedang dalam dark mode
     */
    public boolean isDarkMode() {
        int currentTheme = getCurrentTheme();
        if (currentTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            return true;
        } else if (currentTheme == AppCompatDelegate.MODE_NIGHT_NO) {
            return false;
        } else {
            // MODE_NIGHT_FOLLOW_SYSTEM - cek system setting
            int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        }
    }

    private boolean loadFragment(Fragment fragment, String tag) {
        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment, tag);
            fragmentTransaction.commit();
            return true;
        }
        return false;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        String selectedTag = null;
        int itemId = item.getItemId();

        if (itemId == R.id.navigation_home) {
            selectedFragment = getSupportFragmentManager().findFragmentByTag(TAG_HOME_FRAGMENT);
            if (selectedFragment == null) {
                selectedFragment = new HomeFragment();
            }
            selectedTag = TAG_HOME_FRAGMENT;
        } else if (itemId == R.id.navigation_saved) {
            selectedFragment = getSupportFragmentManager().findFragmentByTag(TAG_SAVED_FRAGMENT);
            if (selectedFragment == null) {
                selectedFragment = new SavedFragment();
            }
            selectedTag = TAG_SAVED_FRAGMENT;
        }

        if (selectedFragment != null) {
            return loadFragment(selectedFragment, selectedTag);
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (!(currentFragment instanceof HomeFragment) && bottomNavigationView.getSelectedItemId() != R.id.navigation_home) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        } else {
            super.onBackPressed();
        }
    }
}