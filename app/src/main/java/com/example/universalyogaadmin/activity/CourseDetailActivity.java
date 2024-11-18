package com.example.universalyogaadmin.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.universalyogaadmin.R;
import com.example.universalyogaadmin.model.Course;

public class CourseDetailActivity extends AppCompatActivity {

    private Button buttonGoHome;
    private TextView textViewCourseName, textViewDayOfWeek, textViewTime,
            textViewCapacity, textViewDuration, textViewPrice, textViewDescription, textViewType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        buttonGoHome = findViewById(R.id.buttonGoHome);
        textViewCourseName = findViewById(R.id.textViewCourseName);
        textViewDayOfWeek = findViewById(R.id.textViewDayOfWeek);
        textViewTime = findViewById(R.id.textViewTime);
        textViewCapacity = findViewById(R.id.textViewCapacity);
        textViewDuration = findViewById(R.id.textViewDuration);
        textViewPrice = findViewById(R.id.textViewPrice);
        textViewDescription = findViewById(R.id.textViewDescription);
        textViewType = findViewById(R.id.textViewType);

        Course course = (Course) getIntent().getSerializableExtra("course");

        if (course != null) {
            textViewCourseName.setText(course.getCourseName());
            textViewDayOfWeek.setText(course.getDayOfWeek());
            textViewTime.setText(course.getTime());
            textViewCapacity.setText(String.valueOf(course.getCapacity()));
            textViewDuration.setText(course.getDuration());
            textViewPrice.setText("$"+(course.getPrice()));
            textViewDescription.setText(course.getDescription());
            textViewType.setText(course.getType());
        }

        buttonGoHome.setOnClickListener(v -> {
            Intent intent = new Intent(CourseDetailActivity.this, ManageCoursesActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
