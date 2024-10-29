package com.example.universalyogaadmin.activity;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.universalyogaadmin.R;
import com.example.universalyogaadmin.adapter.ClassInstanceAdapter;
import com.example.universalyogaadmin.database.DatabaseHelper;
import com.example.universalyogaadmin.model.ClassInstance;
import com.example.universalyogaadmin.model.Course; // Đảm bảo bạn đã có model cho Course

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ManageClassInstancesActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private RecyclerView recyclerViewSearchResults; // Thêm biến cho RecyclerView tìm kiếm
    private EditText editTextSearchTeacher;
    private EditText editTextSearchDate;
    private Button buttonSearch;
    private ClassInstanceAdapter adapter;
    private DatabaseHelper databaseHelper;
    private List<ClassInstance> classInstanceList;
    private List<Course> courseList; // Danh sách khóa học (only declare it once)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_class_instances);



        recyclerView = findViewById(R.id.recyclerViewClassInstances);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Button buttonAddClassInstance = findViewById(R.id.buttonAddClassInstance);
        databaseHelper = new DatabaseHelper(this);

        classInstanceList = new ArrayList<>();
        classInstanceList.addAll(databaseHelper.getAllClassInstances());

        // Lấy danh sách khóa học từ cơ sở dữ liệu
        courseList = databaseHelper.getAllCourses(); // Cài đặt hàm này trong DatabaseHelper
        adapter = new ClassInstanceAdapter(classInstanceList, courseList, this);
        recyclerView.setAdapter(adapter);

        buttonAddClassInstance.setOnClickListener(v -> showAddClassDialog());

        editTextSearchTeacher = findViewById(R.id.editTextSearchTeacher);
        editTextSearchDate = findViewById(R.id.editTextSearchDate);
        buttonSearch = findViewById(R.id.buttonSearch);
        recyclerViewSearchResults = findViewById(R.id.recyclerViewSearchResults); // Đảm bảo bạn đã thêm RecyclerView này vào layout

        buttonSearch.setOnClickListener(v -> performSearch());
    }

    private void performSearch() {
        String teacherName = editTextSearchTeacher.getText().toString().trim();
        String date = editTextSearchDate.getText().toString().trim();

        // Kiểm tra xem trường tìm kiếm có trống không
        if (teacherName.isEmpty() && date.isEmpty()) {
            // Nếu cả hai trường đều trống, hiển thị lại toàn bộ danh sách
            classInstanceList.clear();
            classInstanceList.addAll(databaseHelper.getAllClassInstances());
            adapter.notifyDataSetChanged();
        } else {
            // Nếu có ít nhất một trường không trống, tiến hành tìm kiếm
            classInstanceList.clear();
            List<ClassInstance> filteredList = new ArrayList<>();

            for (ClassInstance instance : databaseHelper.getAllClassInstances()) {
                boolean matchesTeacher = teacherName.isEmpty() || instance.getTeacher().toLowerCase().contains(teacherName.toLowerCase());
                boolean matchesDate = date.isEmpty() || instance.getDate().contains(date);

                if (matchesTeacher && matchesDate) {
                    filteredList.add(instance);
                }
            }

            classInstanceList.addAll(filteredList);
            adapter.notifyDataSetChanged();
        }
    }

    private void showAddClassDialog() {
        Log.d("Dialog", "Showing Add Class Dialog");

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_class_instance, null);
        dialogBuilder.setView(dialogView);


        EditText editTextDate = dialogView.findViewById(R.id.editTextDate);
        EditText editTextTeacher = dialogView.findViewById(R.id.editTextTeacher);
        EditText editTextComments = dialogView.findViewById(R.id.editTextComments);
        Spinner spinnerCourses = dialogView.findViewById(R.id.spinnerCourseName);

        // Thiết lập adapter cho Spinner
        ArrayAdapter<Course> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courseList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourses.setAdapter(adapter);

        dialogBuilder.setTitle("Add Class Instance")
                .setPositiveButton("Add", (dialog, which) -> {
                    String date = editTextDate.getText().toString().trim();
                    String teacher = editTextTeacher.getText().toString().trim();
                    String comments = editTextComments.getText().toString().trim();
                    Course selectedCourse = (Course) spinnerCourses.getSelectedItem();
                    // Kiểm tra nếu khóa học không phải là null và lấy ngày trong tuần


                        // Kiểm tra các trường nhập liệu
                    if (date.isEmpty() || teacher.isEmpty() || selectedCourse == null) {
                        Toast.makeText(this, "Vui lòng điền tất cả các trường bắt buộc!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                        // Kiểm tra nếu khóa học không phải là null và lấy ngày trong tuần
                        if (selectedCourse != null) {
                            String courseDayOfWeek = selectedCourse.getDayOfWeek(); // Lấy ngày trong tuần từ khóa học

                            // Validate the date against the course day of the week
                            if (!validateDate(date, courseDayOfWeek)) {
                                Toast.makeText(this, "Ngày phải khớp với ngày trong tuần thứ " + courseDayOfWeek + " của khóa học.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }

                    // Tạo đối tượng ClassInstance mới
                    ClassInstance newClassInstance = new ClassInstance();
                    newClassInstance.setCourseId(selectedCourse.getId());
                    newClassInstance.setCourseName(selectedCourse.getCourseName());
                    newClassInstance.setDate(date);
                    newClassInstance.setTeacher(teacher);
                    newClassInstance.setComments(comments);

                    // Lưu vào DB và nhận ID
                    long newId = databaseHelper.addClassInstance(newClassInstance);
                    if (newId != -1) {
                        newClassInstance.setId((int) newId); // Giả sử ID là kiểu int
                        classInstanceList.add(newClassInstance);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "Thêm lớp học thành công!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Thêm lớp học thất bại!", Toast.LENGTH_SHORT).show();
                    }

                    Log.d("DB_INSERT", "Thêm lớp học với ID: " + newId);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }


    private void addClassInstance(Course course, String teacher, String date, String comments) {
        // Assuming course.getId() returns the ID of the course
        ClassInstance newClassInstance = new ClassInstance(0, course.getId(), course.getCourseName(), date, teacher, comments);
        databaseHelper.addClassInstance(newClassInstance);
        classInstanceList.add(newClassInstance);
        adapter.notifyItemInserted(classInstanceList.size() - 1);
    }

    private boolean validateDate(String date, String courseDayOfWeek) {
        try {
            // Date format: "dd/MM/yyyy"
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            sdf.setLenient(false); // Disallow invalid dates
            Date parsedDate = sdf.parse(date);

            // Check the day of the week
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedDate);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

            // Convert to a system of 1-7: Monday = 1, ..., Sunday = 7
            if (dayOfWeek == Calendar.SUNDAY) {
                dayOfWeek = 7; // Sunday
            } else {
                dayOfWeek--; // Monday to Saturday
            }

            // Convert the course day of the week to its corresponding number
            int courseDayOfWeekInt = convertDayOfWeekToInt(courseDayOfWeek);

            // Check if the entered date matches the day of the week for the course
            return dayOfWeek == courseDayOfWeekInt;

        } catch (ParseException e) {
            e.printStackTrace();
            return false; // If unable to parse the date
        }
    }

    // Method to convert day name to corresponding number
    private int convertDayOfWeekToInt(String dayOfWeek) {
        switch (dayOfWeek.substring(0, 1).toUpperCase() + dayOfWeek.substring(1).toLowerCase()) {
            case "Monday":
                return 1;
            case "Tuesday":
                return 2;
            case "Wednesday":
                return 3;
            case "Thursday":
                return 4;
            case "Friday":
                return 5;
            case "Saturday":
                return 6;
            case "Sunday":
                return 7;
            default:
                throw new IllegalArgumentException("Invalid day of week: " + dayOfWeek);
        }
    }



}
