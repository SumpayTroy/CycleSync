package com.bsit.cyclesync;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.airbnb.lottie.LottieAnimationView;
import com.bsit.cyclesync.data.FirstLaunchPreferences;
import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_LAST = 1;
    private final List<OnboardingItem> onboardingItems;

    public OnboardingAdapter(List<OnboardingItem> onboardingItems) {
        this.onboardingItems = onboardingItems;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == onboardingItems.size() - 1) ? TYPE_LAST : TYPE_NORMAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_LAST) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_onboarding_last, parent, false);
            return new LastViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_onboarding, parent, false);
            return new NormalViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        OnboardingItem item = onboardingItems.get(position);

        if (getItemViewType(position) == TYPE_LAST) {
            LastViewHolder lastHolder = (LastViewHolder) holder;
            lastHolder.textWelcome.setText(item.title);
            lastHolder.textRide.setText(item.description);

            lastHolder.buttonGetStarted.setOnClickListener(v -> {
                Activity activity = (Activity) v.getContext();

                new Thread(() -> {
                    try {
                        new FirstLaunchPreferences(activity).markFirstLaunchDoneBlocking();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    activity.runOnUiThread(() -> {
                        Intent intent = new Intent(activity, MainActivity.class);
                        activity.startActivity(intent);
                        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        activity.finish();
                    });
                }).start();
            });

        } else {
            NormalViewHolder normalHolder = (NormalViewHolder) holder;
            normalHolder.textTitle.setText(item.title);
            normalHolder.textDescription.setText(item.description);

            if (item.animationRes != 0) {
                normalHolder.lottieAnimation.setAnimation(item.animationRes);
                normalHolder.lottieAnimation.playAnimation();
            }
        }
    }

    @Override
    public int getItemCount() {
        return onboardingItems.size();
    }

    static class NormalViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textDescription;
        LottieAnimationView lottieAnimation;

        NormalViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textDescription = itemView.findViewById(R.id.textDescription);
            lottieAnimation = itemView.findViewById(R.id.lottieAnimation);
        }
    }

    static class LastViewHolder extends RecyclerView.ViewHolder {
        TextView textWelcome, textRide;
        Button buttonGetStarted;

        LastViewHolder(@NonNull View itemView) {
            super(itemView);
            textWelcome = itemView.findViewById(R.id.textWelcome);
            textRide = itemView.findViewById(R.id.textRide);
            buttonGetStarted = itemView.findViewById(R.id.buttonGetStarted);
        }
    }
}
