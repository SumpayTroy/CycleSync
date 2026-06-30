package com.bsit.cyclesync;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class PaymentResultActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Uri data = intent.getData();

        if (data != null) {
            String host = data.getHost();

            if ("payment_success".equals(host)) {
                Toast.makeText(this, "Payment Successful!", Toast.LENGTH_LONG).show();

                // Safely handle possible null user
                FirebaseAuth auth = FirebaseAuth.getInstance();
                if (auth.getCurrentUser() != null) {
                    String uid = auth.getCurrentUser().getUid();
                    FirebaseDatabase.getInstance().getReference("users")
                            .child(uid)
                            .child("isSubscribed")
                            .setValue(true);
                } else {
                    Toast.makeText(this, "User not logged in. Subscription not updated.", Toast.LENGTH_SHORT).show();
                }

            } else if ("payment_failed".equals(host)) {
                Toast.makeText(this, "Payment Failed!", Toast.LENGTH_LONG).show();
            }

        }


        finish();
    }
}

