package com.bsit.cyclesync;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.bsit.cyclesync.data.FirstLaunchPreferences;
import com.bsit.cyclesync.ui.utils.FirebaseUtils;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    public ViewPager2 onboardingViewPager;
    private Button buttonSkip;
    private LinearLayout layoutOnboardingIndicators;
    private List<OnboardingItem> onboardingItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        onboardingViewPager = findViewById(R.id.onboardingViewPager);
        buttonSkip = findViewById(R.id.buttonSkip);
        layoutOnboardingIndicators = findViewById(R.id.layoutOnboardingIndicators);

        // 🔹 Skip onboarding if user is already logged in
        if (FirebaseUtils.INSTANCE.getAuth().getCurrentUser() != null) {
            navigateToMain();
            return;
        }

        setupOnboardingItems();
        onboardingViewPager.setAdapter(new OnboardingAdapter(onboardingItems));
        onboardingViewPager.setUserInputEnabled(true);

        setupIndicators();
        setCurrentIndicator(0);

        // 🔹 Skip button: complete onboarding immediately
        buttonSkip.setOnClickListener(v -> completeOnboarding());

        // 🔹 Handle indicator change
        onboardingViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentIndicator(position);
                buttonSkip.setVisibility(position == onboardingItems.size() - 1 ? View.GONE : View.VISIBLE);
            }
        });
    }

    /** Initialize onboarding pages **/
    private void setupOnboardingItems() {
        onboardingItems = new ArrayList<>();
        onboardingItems.add(new OnboardingItem(
                "CycleSync",
                "Stay connected and ride smarter with CycleSync your real-time cycling companion.",
                R.raw.onboarding_image_1
        ));
        onboardingItems.add(new OnboardingItem(
                "Track Your Friends",
                "Never lose sight of your group again with real-time GPS tracking.",
                R.raw.onboarding_plan_and_ride
        ));
        onboardingItems.add(new OnboardingItem(
                "Set Your Meetup",
                "Choose a location where you and your friends will gather.",
                R.raw.onboarding_track_your_friends
        ));
        onboardingItems.add(new OnboardingItem(
                "Welcome to CycleSync!",
                "Ride together, plan together. Tap Get Started to continue.",
                0
        ));
    }

    /**  Mark onboarding as done and go to main **/
    private void completeOnboarding() {
        new Thread(() -> {
            try {
                new FirstLaunchPreferences(OnboardingActivity.this).markFirstLaunchDoneBlocking();
            } catch (Exception e) {
                Log.e("OnboardingActivity", "Error during operation", e);
            }

            runOnUiThread(this::navigateToMain);
        }).start();
    }

    /**  Navigate with smooth fade **/
    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    /** Setup indicator dots **/
    @SuppressLint("UseCompatLoadingForDrawables")
    private void setupIndicators() {
        int count = onboardingItems.size();
        layoutOnboardingIndicators.removeAllViews();

        for (int i = 0; i < count; i++) {
            ImageView indicator = new ImageView(this);
            indicator.setImageDrawable(getDrawable(R.drawable.indicator_inactive));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            indicator.setLayoutParams(params);

            layoutOnboardingIndicators.addView(indicator);
        }
    }

    /** Update indicator color **/
    @SuppressLint("UseCompatLoadingForDrawables")
    private void setCurrentIndicator(int index) {
        int childCount = layoutOnboardingIndicators.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) layoutOnboardingIndicators.getChildAt(i);
            imageView.setImageDrawable(getDrawable(
                    i == index ? R.drawable.indicator_active : R.drawable.indicator_inactive
            ));
        }
    }
}
