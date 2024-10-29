package com.example.universalyogaadmin.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.universalyogaadmin.R;
import com.example.universalyogaadmin.database.DatabaseHelper;
import com.example.universalyogaadmin.model.Course;

public class CourseDetailActivity extends AppCompatActivity {

    private Button buttonEdit; // Khai báo nút Edit
    private Button buttonDelete; // Khai báo nút Delete
    private TextView textViewCourseName, textViewDayOfWeek, textViewTime,
            textViewCapacity, textViewDuration, textViewPrice, textViewDescription, textViewType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        // Khởi tạo các thành phần giao diện
        buttonEdit = findViewById(R.id.buttonEdit);
        buttonDelete = findViewById(R.id.buttonDelete);
        textViewCourseName = findViewById(R.id.textViewCourseName);
        textViewDayOfWeek = findViewById(R.id.textViewDayOfWeek);
        textViewTime = findViewById(R.id.textViewTime);
        textViewCapacity = findViewById(R.id.textViewCapacity);
        textViewDuration = findViewById(R.id.textViewDuration);
        textViewPrice = findViewById(R.id.textViewPrice);
        textViewDescription = findViewById(R.id.textViewDescription);
        textViewType = findViewById(R.id.textViewType);

        // Lấy dữ liệu khóa học từ Intent
        Course course = (Course) getIntent().getSerializableExtra("course");

        // Hiển thị thông tin khóa học
        if (course != null) {
            textViewCourseName.setText(course.getCourseName());
            textViewDayOfWeek.setText(course.getDayOfWeek());
            textViewTime.setText(course.getTime());
            textViewCapacity.setText(String.valueOf(course.getCapacity()));
            textViewDuration.setText(course.getDuration());
            textViewPrice.setText(String.valueOf(course.getPrice()));
            textViewDescription.setText(course.getDescription());
            textViewType.setText(course.getType());
        }

        // Xử lý nút Edit
        buttonEdit.setOnClickListener(v -> {
            Intent editIntent = new Intent(CourseDetailActivity.this, EditCourseActivity.class);
            editIntent.putExtra("course", course); // Truyền đối tượng Course
            startActivityForResult(editIntent, 1); // Bắt đầu hoạt động chỉnh sửa để nhận kết quả
        });



        // Xử lý nút Delete
        buttonDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(CourseDetailActivity.this)
                    .setTitle("Confirm Delete")
                    .setMessage("Are you sure you want to delete this course?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        DatabaseHelper dbHelper = new DatabaseHelper(CourseDetailActivity.this);
                        if (dbHelper.deleteCourse(course.getId())) {
                            Toast.makeText(CourseDetailActivity.this, "Course deleted successfully", Toast.LENGTH_SHORT).show();
                            finish(); // Quay lại màn hình chính
                        } else {
                            Toast.makeText(CourseDetailActivity.this, "Error deleting course", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) { // Mã yêu cầu bạn đã sử dụng
            if (resultCode == RESULT_OK) {
                Course updatedCourse = (Course) data.getSerializableExtra("updated_course");
                if (updatedCourse != null) {
                    // Cập nhật giao diện với dữ liệu khóa học mới ngay lập tức
                    textViewCourseName.setText(updatedCourse.getCourseName());
                    textViewDayOfWeek.setText(updatedCourse.getDayOfWeek());
                    textViewTime.setText(updatedCourse.getTime());
                    textViewCapacity.setText(String.valueOf(updatedCourse.getCapacity()));
                    textViewDuration.setText(updatedCourse.getDuration());
                    textViewPrice.setText(String.valueOf(updatedCourse.getPrice()));
                    textViewDescription.setText(updatedCourse.getDescription());
                    textViewType.setText(updatedCourse.getType());
                }
            }
        }
    }


}
