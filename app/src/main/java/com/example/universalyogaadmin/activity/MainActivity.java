package com.example.universalyogaadmin.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.universalyogaadmin.R;
import com.google.firebase.Firebase;


public class MainActivity extends AppCompatActivity {

    private Button btnManageCourses, btnManageClassInstances;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnManageCourses = findViewById(R.id.btnManageCourses);
        btnManageClassInstances = findViewById(R.id.btnManageClassInstances);

        btnManageCourses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ManageCoursesActivity.class));
            }
        });

        btnManageClassInstances.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ManageClassInstancesActivity.class));
            }
        });


    }
}
