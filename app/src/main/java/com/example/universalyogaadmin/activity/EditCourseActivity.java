package com.example.universalyogaadmin.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.universalyogaadmin.R;
import com.example.universalyogaadmin.database.DatabaseHelper;
import com.example.universalyogaadmin.model.Course;

import java.util.Arrays;

public class EditCourseActivity extends AppCompatActivity {
    private EditText editTextCourseName, editTextTime, editTextCapacity, editTextDuration, editTextPrice, editTextDescription;
    private Spinner spinnerDayOfWeek, spinnerTypeOfClass;
    private Button buttonSave, buttonCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_course);

        // Initialize views
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

        // Get the course data from the intent
        Course course = (Course) getIntent().getSerializableExtra("course");

        // Retrieve string arrays from resources
        String[] daysOfWeek = getResources().getStringArray(R.array.days_of_week_array);
        String[] typesOfClass = getResources().getStringArray(R.array.class_type_array);

        // Set up the spinners with the string arrays
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, daysOfWeek);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDayOfWeek.setAdapter(dayAdapter);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, typesOfClass);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTypeOfClass.setAdapter(typeAdapter);

        // Populate fields with existing course data
        if (course != null) {
            editTextCourseName.setText(course.getCourseName());
            editTextTime.setText(course.getTime());
            editTextCapacity.setText(String.valueOf(course.getCapacity()));
            editTextDuration.setText(course.getDuration());
            editTextPrice.setText(String.valueOf(course.getPrice()));
            editTextDescription.setText(course.getDescription());

            // Set selected item in spinners
            int dayPosition = Arrays.asList(daysOfWeek).indexOf(course.getDayOfWeek());
            spinnerDayOfWeek.setSelection(dayPosition >= 0 ? dayPosition : 0);

            int typePosition = Arrays.asList(typesOfClass).indexOf(course.getType());
            spinnerTypeOfClass.setSelection(typePosition >= 0 ? typePosition : 0);
        }

        // Set button listeners for save and cancel
        buttonSave.setOnClickListener(v -> {
            // Lấy thông tin từ các trường EditText và Spinner
            String courseName = editTextCourseName.getText().toString();
            String dayOfWeek = spinnerDayOfWeek.getSelectedItem().toString();
            String type = spinnerTypeOfClass.getSelectedItem().toString(); // Khai báo biến type
            String time = editTextTime.getText().toString();
            int capacity = Integer.parseInt(editTextCapacity.getText().toString());
            String duration = editTextDuration.getText().toString();
            double price = Double.parseDouble(editTextPrice.getText().toString());
            String description = editTextDescription.getText().toString();

            // Tạo một đối tượng Course mới hoặc cập nhật đối tượng hiện tại
            if (course != null) {
                course.setCourseName(courseName);
                course.setDayOfWeek(dayOfWeek);
                course.setType(type); // Sử dụng biến type
                course.setTime(time);
                course.setCapacity(capacity);
                course.setDuration(duration);
                course.setPrice(price);
                course.setDescription(description);

                // Lưu vào database
                DatabaseHelper dbHelper = new DatabaseHelper(EditCourseActivity.this);
                if (dbHelper.updateCourse(course)) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("updated_course", course); // Trả về khóa học đã cập nhật
                    setResult(RESULT_OK, resultIntent); // Đặt kết quả là OK
                    finish(); // Đóng EditCourseActivity
                } else {
                    Toast.makeText(EditCourseActivity.this, "Lỗi khi lưu khóa học", Toast.LENGTH_SHORT).show();
                }
            }
        });


        buttonCancel.setOnClickListener(v -> finish()); // Close the activity
    }

    private int getSpinnerIndex(Spinner spinner, String myString) {
        int index = 0;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(myString)) {
                index = i;
            }
        }
        return index;
    }
}
