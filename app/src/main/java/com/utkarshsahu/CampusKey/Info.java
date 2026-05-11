package com.utkarshsahu.CampusKey;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class Info extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ThanksAdapter adapter;
    private List<ThanksMember> thanksList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        recyclerView = findViewById(R.id.thanksRecyclerView);


        // Inside onCreate, replace the previous LayoutManager setup with this:
        int numberOfColumns = 4;
        recyclerView.setLayoutManager(new GridLayoutManager(this, numberOfColumns));
        // Prepare your data
        thanksList = new ArrayList<>();
        thanksList.add(new ThanksMember("Raj Patel", "https://scontent-bom5-1.cdninstagram.com/v/t51.2885-19/621680327_18415899607187001_705335206668071697_n.jpg?efg=eyJ2ZW5jb2RlX3RhZyI6InByb2ZpbGVfcGljLmRqYW5nby4xMDgwLmMyIn0&_nc_ht=scontent-bom5-1.cdninstagram.com&_nc_cat=109&_nc_oc=Q6cZ2QFOriLUI7Xvv12e1LXjXJ8W-tZhARhXC39EZsZY3yWiTLJFiGUxzjQ-p459wnWGpjI&_nc_ohc=qdHJHO3oEMcQ7kNvwFVVqBE&_nc_gid=52ybXZIER3qEfksEOR5JmQ&edm=AP4sbd4BAAAA&ccb=7-5&oh=00_Afs0Ln19OIzBM0q-MTYxbSZnByBLG3nQgifXQRaRqiIogw&oe=69940DC2&_nc_sid=7a9f4b", "https://linkedin.com/in/raj"));
        thanksList.add(new ThanksMember("Anya Singh", "https://example.com/anya.jpg", "https://github.com/anya"));
        thanksList.add(new ThanksMember("Raj Patel", "https://example.com/raj.jpg", "https://linkedin.com/in/raj"));
        thanksList.add(new ThanksMember("Raj Patel", "https://example.com/raj.jpg", "https://linkedin.com/in/raj"));
        thanksList.add(new ThanksMember("Raj Patel", "https://example.com/raj.jpg", "https://linkedin.com/in/raj"));
        thanksList.add(new ThanksMember("Raj Patel", "https://example.com/raj.jpg", "https://linkedin.com/in/raj"));
        thanksList.add(new ThanksMember("Raj Patel", "https://example.com/raj.jpg", "https://linkedin.com/in/raj"));
        thanksList.add(new ThanksMember("Raj Patel", "https://example.com/raj.jpg", "https://linkedin.com/in/raj"));
        thanksList.add(new ThanksMember("Raj Patel", "https://example.com/raj.jpg", "https://linkedin.com/in/raj"));
        thanksList.add(new ThanksMember("Raj Patel", "https://example.com/raj.jpg", "https://linkedin.com/in/raj"));
        thanksList.add(new ThanksMember("Raj Patel", "https://example.com/raj.jpg", "https://linkedin.com/in/raj"));
        thanksList.add(new ThanksMember("Raj Patel", "https://example.com/raj.jpg", "https://linkedin.com/in/raj"));
        thanksList.add(new ThanksMember("Raj Patel", "https://example.com/raj.jpg", "https://linkedin.com/in/raj"));
        thanksList.add(new ThanksMember("Raj Patel", "https://example.com/raj.jpg", "https://linkedin.com/in/raj"));
        thanksList.add(new ThanksMember("Raj Patel", "https://example.com/raj.jpg", "https://linkedin.com/in/raj"));
        thanksList.add(new ThanksMember("Raj Patel", "https://example.com/raj.jpg", "https://linkedin.com/in/raj"));
        thanksList.add(new ThanksMember("Raj Patel", "https://example.com/raj.jpg", "https://linkedin.com/in/raj"));


        thanksList.add(new ThanksMember("John Doe", "https://example.com/john.jpg", "https://twitter.com/john"));
// Ensure your adapter is set as before




        // Set the adapter
        adapter = new ThanksAdapter(thanksList, this);
        recyclerView.setAdapter(adapter);

        applyAnimations();
    }

    private void applyAnimations() {
        android.view.animation.Animation scaleBounce = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.scale_bounce);
        android.view.animation.Animation slideUp = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_fade_in);

        findViewById(android.R.id.content).post(() -> {
            findViewById(R.id.profileContainer).startAnimation(scaleBounce);
            
            slideUp.setStartOffset(200);
            findViewById(R.id.userName).startAnimation(slideUp);
            
            android.view.animation.Animation slideUp2 = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_fade_in);
            slideUp2.setStartOffset(400);
            findViewById(R.id.socialRow).startAnimation(slideUp2);
            
            android.view.animation.Animation slideUp3 = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_fade_in);
            slideUp3.setStartOffset(600);
            findViewById(R.id.thanksSection).startAnimation(slideUp3);
        });
    }
}