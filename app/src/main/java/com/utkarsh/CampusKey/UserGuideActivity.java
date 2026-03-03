package com.utkarsh.CampusKey;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

public class UserGuideActivity extends AppCompatActivity {

    private ViewPager2     viewPager;
    private Button         btnNext, btnSkip;
    private LinearLayout   dotsContainer;

    // {emoji, title, description, color}
    private static final String[][] SLIDES = {
        {"🎓","Welcome to CampusKey",
         "Your college Wi-Fi, now fully automatic. Connect once — CampusKey handles everything after that.","#4F46E5"},
        {"📶","Auto-Detection",
         "CampusKey watches for the college network in the background. The moment your phone connects to campus Wi-Fi, it logs you in automatically — no app opening needed.","#059669"},
        {"🔑","Save Your Credentials",
         "Enter your college username and password once. They're stored securely on your device and never shared with anyone.","#D97706"},
        {"🔔","Login Notifications",
         "Every time CampusKey logs you in, you'll get a notification. Tap it to see your connection stats and how many others are using the app.","#7C3AED"},
        {"📊","Live Stats",
         "After each login, see real-time stats: how many students use CampusKey today, total users, and all-time login count — updated live from the cloud.","#DC2626"},
        {"⚡","You're All Set!",
         "Just leave CampusKey running in the background. Walk into college and you're connected — it's that simple.","#0891B2"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_guide);

        viewPager     = findViewById(R.id.viewPager);
        btnNext       = findViewById(R.id.btnNext);
        btnSkip       = findViewById(R.id.btnSkip);
        dotsContainer = findViewById(R.id.dotsContainer);

        viewPager.setAdapter(new GuideAdapter());
        setupDots(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int pos) {
                setupDots(pos);
                boolean last = pos == SLIDES.length - 1;
                btnNext.setText(last ? "Get Started" : "Next");
                btnSkip.setVisibility(last ? View.INVISIBLE : View.VISIBLE);
            }
        });

        btnNext.setOnClickListener(v -> {
            int cur = viewPager.getCurrentItem();
            if (cur < SLIDES.length - 1) viewPager.setCurrentItem(cur + 1);
            else finish();
        });
        btnSkip.setOnClickListener(v -> finish());
    }

    private void setupDots(int active) {
        dotsContainer.removeAllViews();
        for (int i = 0; i < SLIDES.length; i++) {
            View dot = new View(this);
            int size = i == active ? dp(10) : dp(7);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(dp(4), 0, dp(4), 0);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(i == active
                ? android.R.drawable.presence_online
                : android.R.drawable.presence_invisible);
            dotsContainer.addView(dot);
        }
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    class GuideAdapter extends FragmentStateAdapter {
        GuideAdapter() { super(UserGuideActivity.this); }
        @Override public int getItemCount() { return SLIDES.length; }
        @NonNull @Override public Fragment createFragment(int pos) {
            return GuideFragment.newInstance(SLIDES[pos]);
        }
    }

    public static class GuideFragment extends Fragment {
        private static final String KEY = "slide";

        public static GuideFragment newInstance(String[] slide) {
            GuideFragment f = new GuideFragment();
            Bundle b = new Bundle();
            b.putStringArray(KEY, slide);
            f.setArguments(b);
            return f;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 ViewGroup container, Bundle saved) {
            LinearLayout root = new LinearLayout(requireContext());
            root.setOrientation(LinearLayout.VERTICAL);
            root.setGravity(android.view.Gravity.CENTER);
            root.setPadding(dp(32), dp(60), dp(32), dp(40));

            String[] slide = getArguments() != null
                ? getArguments().getStringArray(KEY)
                : new String[]{"🎓","Title","Description","#4F46E5"};

            try {
                root.setBackgroundColor(
                    (android.graphics.Color.parseColor(slide[3]) & 0x00FFFFFF) | 0x12000000);
            } catch (Exception ignored) {}

            // Emoji with circle background
            TextView emoji = new TextView(requireContext());
            emoji.setText(slide[0]);
            emoji.setTextSize(64f);
            emoji.setGravity(android.view.Gravity.CENTER);
            emoji.setPadding(dp(24), dp(16), dp(24), dp(16));
            android.graphics.drawable.GradientDrawable circle =
                new android.graphics.drawable.GradientDrawable();
            circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            try {
                int c = android.graphics.Color.parseColor(slide[3]);
                circle.setColor((c & 0x00FFFFFF) | 0x25000000);
            } catch (Exception e) { circle.setColor(0x254F46E5); }
            emoji.setBackground(circle);
            LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-2, -2);
            ep.bottomMargin = dp(28);
            root.addView(emoji, ep);

            // Title
            TextView title = new TextView(requireContext());
            title.setText(slide[1]);
            title.setTextSize(22f);
            title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            title.setTextColor(0xFF1A1A2E);
            title.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1, -2);
            tlp.bottomMargin = dp(14);
            root.addView(title, tlp);

            // Description
            TextView desc = new TextView(requireContext());
            desc.setText(slide[2]);
            desc.setTextSize(15f);
            desc.setTextColor(0xFF4B5563);
            desc.setGravity(android.view.Gravity.CENTER);
            desc.setLineSpacing(dp(4), 1f);
            root.addView(desc, new LinearLayout.LayoutParams(-1, -2));

            return root;
        }

        private int dp(int dp) {
            return Math.round(dp * getResources().getDisplayMetrics().density);
        }
    }
}
