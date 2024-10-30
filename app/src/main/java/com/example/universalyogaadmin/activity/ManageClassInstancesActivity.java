package com.example.universalyogaadmin.activity;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
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
import com.google.firebase.firestore.FirebaseFirestore;

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

                    // Kiểm tra các trường nhập liệu
                    if (date.isEmpty() || teacher.isEmpty() || selectedCourse == null) {
                        Toast.makeText(this, "Vui lòng điền tất cả các trường bắt buộc!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Lấy ngày trong tuần từ khóa học
                    String courseDayOfWeek = selectedCourse.getDayOfWeek();

                    // Validate the date against the course day of the week
                    if (!validateDate(date, courseDayOfWeek)) {
                        Toast.makeText(this, "Ngày phải khớp với ngày trong tuần thứ " + courseDayOfWeek + " của khóa học.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Tạo đối tượng ClassInstance mới
                    ClassInstance newClassInstance = new ClassInstance();
                    newClassInstance.setCourseId(selectedCourse.getId());
                    newClassInstance.setCourseName(selectedCourse.getCourseName());
                    newClassInstance.setDate(date);
                    newClassInstance.setTeacher(teacher);
                    newClassInstance.setComments(comments);

                    Log.d("ManageClassInstances", "Giá trị firestoreId: " + newClassInstance.getFirestoreId());
                    long newId = databaseHelper.addClassInstance(newClassInstance, newClassInstance.getFirestoreId());
                    if (newId != -1) {
                        newClassInstance.setId((int) newId);
                        classInstanceList.add(newClassInstance);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "Thêm lớp học thành công!", Toast.LENGTH_SHORT).show();

                        // Đồng bộ hóa với Firestore
                        syncWithFirestore(newClassInstance); // Gọi sau khi thêm vào SQLite
                    } else {
                        Toast.makeText(this, "Thêm lớp học thất bại!", Toast.LENGTH_SHORT).show();
                    }



                    Log.d("DB_INSERT", "Thêm lớp học với ID: " + newId);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    public void syncWithFirestore(ClassInstance classInstance) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("classInstances")
                .add(classInstance.toMap())
                .addOnSuccessListener(documentReference -> {
                    // Lấy Firestore ID sau khi lưu vào Firestore
                    String firestoreId = documentReference.getId();
                    classInstance.setFirestoreId(firestoreId); // Cập nhật firestoreId vào đối tượng

                    // Cập nhật Firestore ID trong SQLite
                    long updateResult = databaseHelper.updateFirestoreIdInSQLite(classInstance.getId(), firestoreId);
                    if (updateResult == -1) {
                        Log.e("Firestore", "Không thể cập nhật Firestore ID vào SQLite");
                    } else {
                        Log.d("Firestore", "Cập nhật Firestore ID thành công trong SQLite: " + firestoreId);
                    }

                    Log.d("Firestore", "Lớp học đã được đồng bộ với Firestore với ID: " + firestoreId);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Lỗi khi đồng bộ với Firestore", e));
    }


    private void addClassInstance(Course course, String teacher, String date, String comments) {
        // Lấy courseId và courseName từ đối tượng Course
        int courseId = course.getId();
        String courseName = course.getCourseName();
        String firestoreId = ""; // Khởi tạo Firestore ID nếu cần

        // Tạo đối tượng ClassInstance mới
        ClassInstance newClassInstance = new ClassInstance(0, courseId, courseName, date, teacher, comments, null);

        // Thêm ClassInstance vào cơ sở dữ liệu và danh sách
        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        long newId = databaseHelper.addClassInstance(newClassInstance, firestoreId);

        if (newId != -1) { // Kiểm tra nếu thêm thành công
            classInstanceList.add(newClassInstance);
            adapter.notifyItemInserted(classInstanceList.size() - 1);
        }
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
            int courseDayOfWeekInt = convertDayOfWeekStringToInt(courseDayOfWeek);

            // Compare the two
            return dayOfWeek == courseDayOfWeekInt;

        } catch (ParseException e) {
            Toast.makeText(this, "Ngày không hợp lệ, vui lòng nhập theo định dạng dd/MM/yyyy!", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private int convertDayOfWeekStringToInt(String day) {
        switch (day.toLowerCase()) {
            case "monday":
                return 1;
            case "tuesday":
                return 2;
            case "wednesday":
                return 3;
            case "thursday":
                return 4;
            case "friday":
                return 5;
            case "saturday":
                return 6;
            case "sunday":
                return 7;
            default:
                return -1; // Invalid day
        }
    }
}
