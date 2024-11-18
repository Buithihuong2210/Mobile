package com.example.universalyogaadmin.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.universalyogaadmin.R;
import com.example.universalyogaadmin.database.DatabaseHelper;
import com.example.universalyogaadmin.model.Course;
import com.example.universalyogaadmin.utils.NetworkUtil;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditCourseActivity extends AppCompatActivity {

    private EditText editTextCourseName, editTextTime, editTextCapacity, editTextDuration, editTextPrice, editTextDescription;
    private Spinner spinnerDayOfWeek, spinnerTypeOfClass;
    private Button buttonSave, buttonCancel;
    private DatabaseHelper databaseHelper;

    private String firestoreId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_course);
        editTextCourseName = findViewById(R.id.editTextCourseName);
        spinnerDayOfWeek = findViewById(R.id.spinnerDayOfWeek);
        spinnerTypeOfClass = findViewById(R.id.spinnerTypeOfClass);
        editTextTime = findViewById(R.id.editTextTime);
        editTextCapacity = findViewById(R.id.editTextCapacity);
        editTextDuration = findViewById(R.id.editTextDuration);
        editTextPrice = findViewById(R.id.editTextPrice);
        editTextDescription = findViewById(R.id.editTextDescription);
        buttonSave = findViewById(R.id.buttonSave);
        buttonCancel = findViewById(R.id.buttonCancel);

        databaseHelper = new DatabaseHelper(this);

        int courseId = getIntent().getIntExtra("courseId", -1);
        firestoreId = getIntent().getStringExtra("firestoreId");


        Log.d("EditCourseActivity", "Received Course ID: " + courseId);
        Log.d("EditCourseActivity", "Received Firestore ID: " + firestoreId);


        Course course = databaseHelper.getCourseById(courseId);

        if (course == null) {
            Toast.makeText(this, "Course does not exist", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (firestoreId == null || firestoreId.isEmpty()) {
            Toast.makeText(this, "Invalid Firestore ID", Toast.LENGTH_SHORT).show();
            return;
        }

        editTextCourseName.setText(course.getCourseName());
        editTextTime.setText(course.getTime());
        editTextCapacity.setText(String.valueOf(course.getCapacity()));
        editTextDuration.setText(course.getDuration());
        editTextPrice.setText(String.valueOf(course.getPrice()));
        editTextDescription.setText(course.getDescription());

        ArrayAdapter<CharSequence> dayAdapter = ArrayAdapter.createFromResource(this,
                R.array.days_of_week_array, android.R.layout.simple_spinner_item);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDayOfWeek.setAdapter(dayAdapter);
        int dayIndex = dayAdapter.getPosition(course.getDayOfWeek());
        spinnerDayOfWeek.setSelection(dayIndex);

        ArrayAdapter<CharSequence> classTypeAdapter = ArrayAdapter.createFromResource(this,
                R.array.class_type_array, android.R.layout.simple_spinner_item);
        classTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTypeOfClass.setAdapter(classTypeAdapter);
        int typeIndex = classTypeAdapter.getPosition(course.getType());
        spinnerTypeOfClass.setSelection(typeIndex);

        buttonSave.setOnClickListener(v -> {
            if (firestoreId == null || firestoreId.isEmpty()) {
                Toast.makeText(EditCourseActivity.this, "Invalid Firestore ID", Toast.LENGTH_SHORT).show();
                return;
            }

            String courseName = editTextCourseName.getText().toString();
            String dayOfWeek = spinnerDayOfWeek.getSelectedItem().toString();
            String type = spinnerTypeOfClass.getSelectedItem().toString();
            String time = editTextTime.getText().toString();
            int capacity = Integer.parseInt(editTextCapacity.getText().toString());
            String duration = editTextDuration.getText().toString();
            double price = Double.parseDouble(editTextPrice.getText().toString());
            String description = editTextDescription.getText().toString();

            course.setCourseName(courseName);
            course.setDayOfWeek(dayOfWeek);
            course.setType(type);
            course.setTime(time);
            course.setCapacity(capacity);
            course.setDuration(duration);
            course.setPrice(price);
            course.setDescription(description);

            if (NetworkUtil.isNetworkAvailable(this)) {
                // Cập nhật SQLite trước
                if (databaseHelper.updateCourseByFirestoreId(firestoreId, course)) {
                    // Cập nhật Firestore
                    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                    firestore.collection("courses")
                            .document(firestoreId)
                            .set(course)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(EditCourseActivity.this, "Course updated successfully!", Toast.LENGTH_SHORT).show();
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("updated_course", course);
                                setResult(RESULT_OK, resultIntent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Firestore", "Error updating course in Firestore: ", e);
                                Toast.makeText(EditCourseActivity.this, "Error updating course in Firestore.", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(EditCourseActivity.this, "Error updating course in SQLite.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No network connection. Please try again later.", Toast.LENGTH_SHORT).show();
            }
        });

        buttonCancel.setOnClickListener(v -> finish());
    }
}
