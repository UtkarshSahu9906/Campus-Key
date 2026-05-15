package com.utkarshsahu.CampusKey;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AllFeedbacksActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FeedbackAdapter adapter;
    private List<FeedbackModel> feedbackList;
    private DatabaseReference feedbackRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_feedbacks);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.allFeedbacksRecyclerView);
        feedbackList = new ArrayList<>();
        adapter = new FeedbackAdapter(feedbackList);
        recyclerView.setAdapter(adapter);

        feedbackRef = FirebaseDatabase.getInstance().getReference("feedbacks");
        loadAllFeedbacks();
    }

    private void loadAllFeedbacks() {
        feedbackRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                feedbackList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    FeedbackModel feedback = postSnapshot.getValue(FeedbackModel.class);
                    if (feedback != null) feedbackList.add(feedback);
                }
                // Sort by newest first (descending timestamp)
                Collections.sort(feedbackList, (f1, f2) -> Long.compare(f2.getTimestamp(), f1.getTimestamp()));
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AllFeedbacksActivity.this, "Failed to load community wall", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
