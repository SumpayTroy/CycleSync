package com.bsit.cyclesync;

import android.net.Uri; // Added for Uri.parse if you uncomment that part
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RideStatsActivity extends AppCompatActivity {

    private TextView textDistance, textElevation, textMovingTime,
            textAvgSpeed, textAvgHeartRate, textMaxElevation;
    private ImageView bgImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // From the first onCreate
        setContentView(R.layout.activity_ride_stats); // Set content view ONCE

        // Logic from the first onCreate for window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // UI refs from the second onCreate
        bgImage = findViewById(R.id.bgImage);
        textDistance = findViewById(R.id.textDistance);
        textElevation = findViewById(R.id.textElevation);
        textMovingTime = findViewById(R.id.textMovingTime);
        textAvgSpeed = findViewById(R.id.textAvgSpeed);
        textAvgHeartRate = findViewById(R.id.textAvgHeartRate);
        textMaxElevation = findViewById(R.id.textMaxElevation);

        // Optional: background logic from the second onCreate
        // String bgPath = getIntent().getStringExtra("bg_image_path");
        // if (bgPath != null && bgImage != null) { // Added null check for bgImage
        //    bgImage.setImageURI(Uri.parse(bgPath));
        // } else if (bgImage != null) { // Added null check for bgImage
        //    // bgImage.setImageResource(R.drawable.default_bg); // Ensure default_bg exists
        // }

        // TODO: Add logic here to fetch and display actual ride statistics
        // For example:
        // textDistance.setText("10.5 km");
        // textElevation.setText("120 m");
        // ... and so on for other TextViews
    }
}
