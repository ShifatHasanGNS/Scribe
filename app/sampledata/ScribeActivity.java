package com.plasma.scribe;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.plasma.scribe.databinding.ActivityScribeBinding;

public class ScribeActivity extends AppCompatActivity {

    ActivityScribeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityScribeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        replaceFragment(new HomeFragment());

        binding.bottomNavView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navItemHome) {
                replaceFragment(new HomeFragment());
            } else if (item.getItemId() == R.id.navItemLibrary) {
                replaceFragment(new LibraryFragment());
            } else if (item.getItemId() == R.id.navItemProfile) {
                replaceFragment(new ProfileFragment());
            }
            return true;
        });
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .commit();
    }
}