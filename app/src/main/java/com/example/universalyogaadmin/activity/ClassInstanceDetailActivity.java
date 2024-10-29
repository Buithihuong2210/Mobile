package com.example.universalyogaadmin.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.universalyogaadmin.R;
import com.example.universalyogaadmin.model.ClassInstance;
import com.example.universalyogaadmin.model.Course;

public class ClassInstanceDetailActivity extends AppCompatActivity {

    private TextView textViewCourseName, textViewDayOfWeek, textViewTime,
            textViewCapacity, textViewDuration, textViewPrice, textViewType,
            textViewDescription, textViewDate, textViewTeacher, textViewComments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_instance_detail);

        textViewCourseName = findViewById(R.id.textViewCourseName);
        textViewDayOfWeek = findViewById(R.id.textViewDayOfWeek);
        textViewTime = findViewById(R.id.textViewTime);
        textViewCapacity = findViewById(R.id.textViewCapacity);
        textViewDuration = findViewById(R.id.textViewDuration);
        textViewPrice = findViewById(R.id.textViewPrice);
        textViewType = findViewById(R.id.textViewType);
        textViewDescription = findViewById(R.id.textViewDescription);
        textViewDate = findViewById(R.id.textViewDate);
        textViewTeacher = findViewById(R.id.textViewTeacher);
        textViewComments = findViewById(R.id.textViewComments);

        Intent intent = getIntent();
        ClassInstance classInstance = (ClassInstance) intent.getSerializableExtra("classInstance");
        Course course = (Course) intent.getSerializableExtra("course");

        // Log to check if objects are retrieved properly
        Log.d("ClassInstanceDetailActivity", "Received ClassInstance: " + classInstance);
        Log.d("ClassInstanceDetailActivity", "Received Course: " + course);

        if (classInstance != null && course != null) {
            // Proceed with setting up the UI
        } else {
            Log.e("ClassInstanceDetailActivity", "Failed to retrieve classInstance or course");
            Toast.makeText(this, "Failed to load details.", Toast.LENGTH_SHORT).show();
        }

        if (classInstance != null) {
            textViewDate.setText("Date: " + classInstance.getDate());
            textViewTeacher.setText("Teacher: " + classInstance.getTeacher());
            textViewComments.setText("Comment: " + classInstance.getComments());
        }

        if (course != null) {
            textViewCourseName.setText(course.getCourseName());
            textViewDayOfWeek.setText(course.getDayOfWeek());
            textViewTime.setText(course.getTime());
            textViewCapacity.setText(String.valueOf(course.getCapacity()));
            textViewDuration.setText(course.getDuration());
            textViewPrice.setText("£" + course.getPrice());
            textViewType.setText(course.getType());
            textViewDescription.setText(course.getDescription());
        }
    }
}
