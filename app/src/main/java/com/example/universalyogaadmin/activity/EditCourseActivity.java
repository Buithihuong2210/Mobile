package com.example.universalyogaadmin.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

public class EditCourseActivity extends AppCompatActivity {

    private EditText editTextCourseName, editTextTime, editTextCapacity, editTextDuration, editTextPrice, editTextDescription;
    private Spinner spinnerDayOfWeek, spinnerTypeOfClass;
    private Button buttonSave, buttonCancel;
    private DatabaseHelper databaseHelper; // Thêm biến cho DatabaseHelper

    private String firestoreId; // Firestore ID của khóa học

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_course);

        // Khởi tạo các view
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

        // Khởi tạo DatabaseHelper
        databaseHelper = new DatabaseHelper(this);

        int courseId = getIntent().getIntExtra("courseId", -1); // Lấy ID khóa học
        firestoreId = getIntent().getStringExtra("firestoreId"); // Lấy Firestore ID


        // Kiểm tra giá trị nhận được
        Log.d("EditCourseActivity", "Received Course ID: " + courseId);
        Log.d("EditCourseActivity", "Received Firestore ID: " + firestoreId);


        // Lấy khóa học từ SQLite
        Course course = databaseHelper.getCourseById(courseId); // Sử dụng courseId kiểu int

        // Kiểm tra xem đối tượng course có null không
        if (course == null) {
            Toast.makeText(this, "Khóa học không tồn tại", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Kiểm tra giá trị Firestore ID
        if (firestoreId == null || firestoreId.isEmpty()) {
            Toast.makeText(this, "ID Firestore không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Đổ dữ liệu vào các trường editText và spinner
        editTextCourseName.setText(course.getCourseName());
        editTextTime.setText(course.getTime());
        editTextCapacity.setText(String.valueOf(course.getCapacity()));
        editTextDuration.setText(course.getDuration());
        editTextPrice.setText(String.valueOf(course.getPrice()));
        editTextDescription.setText(course.getDescription());
        // Thiết lập giá trị cho spinner nếu cần

        ArrayAdapter<CharSequence> dayAdapter = ArrayAdapter.createFromResource(this,
                R.array.days_of_week_array, android.R.layout.simple_spinner_item);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDayOfWeek.setAdapter(dayAdapter);

        ArrayAdapter<CharSequence> classTypeAdapter = ArrayAdapter.createFromResource(this,
                R.array.class_type_array, android.R.layout.simple_spinner_item);
        classTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTypeOfClass.setAdapter(classTypeAdapter);

        buttonSave.setOnClickListener(v -> {
            // Kiểm tra lại Firestore ID khi nhấn nút lưu
            if (firestoreId == null || firestoreId.isEmpty()) {
                Toast.makeText(EditCourseActivity.this, "ID Firestore không hợp lệ", Toast.LENGTH_SHORT).show();
                return; // Ngăn không cho thực hiện các bước tiếp theo
            }

            // Lấy thông tin từ các trường EditText và Spinner
            String courseName = editTextCourseName.getText().toString();
            String dayOfWeek = spinnerDayOfWeek.getSelectedItem().toString();
            String type = spinnerTypeOfClass.getSelectedItem().toString();
            String time = editTextTime.getText().toString();
            int capacity = Integer.parseInt(editTextCapacity.getText().toString());
            String duration = editTextDuration.getText().toString();
            double price = Double.parseDouble(editTextPrice.getText().toString());
            String description = editTextDescription.getText().toString();

            // Cập nhật thông tin khóa học
            course.setCourseName(courseName);
            course.setDayOfWeek(dayOfWeek);
            course.setType(type);
            course.setTime(time);
            course.setCapacity(capacity);
            course.setDuration(duration);
            course.setPrice(price);
            course.setDescription(description);

            // Kiểm tra kết nối mạng trước khi lưu vào Firestore
            if (NetworkUtil.isNetworkAvailable(this)) {
                // Lưu vào database
                if (databaseHelper.updateCourseByFirestoreId(firestoreId, course)) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("updated_course", course); // Trả về khóa học đã cập nhật
                    setResult(RESULT_OK, resultIntent); // Đặt kết quả là OK
                    finish(); // Đóng EditCourseActivity
                } else {
                    Toast.makeText(EditCourseActivity.this, "Lỗi khi cập nhật khóa học", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Không có kết nối mạng. Vui lòng thử lại sau.", Toast.LENGTH_SHORT).show();
            }
        });

        buttonCancel.setOnClickListener(v -> finish()); // Đóng activity
    }
}
