package com.example.universalyogaadmin.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.universalyogaadmin.R;
import com.example.universalyogaadmin.adapter.CourseAdapter;
import com.example.universalyogaadmin.database.DatabaseHelper;
import com.example.universalyogaadmin.listener.OnCourseDeleteListener;
import com.example.universalyogaadmin.model.ClassInstance;
import com.example.universalyogaadmin.model.Course;
import com.example.universalyogaadmin.utils.NetworkUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class ManageCoursesActivity extends AppCompatActivity implements OnCourseDeleteListener, CourseAdapter.OnAddClassClickListener {

    private static final String TAG = "ManageCoursesActivity";
    private static final String CLASS_INSTANCE_COURSE_ID = "courseId";

    private RecyclerView recyclerView;
    private CourseAdapter courseAdapter;
    private List<Course> courseList;
    private DatabaseHelper databaseHelper;
    private FirebaseFirestore firestore;
    private Button buttonAddCourse, buttonSearch;
    private EditText searchEditText; // Khai báo biến EditText
    private EditText editTextSearchCourseName; // Khai báo biến EditText cho tìm kiếm
    private Spinner spinnerSortPrice;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_courses);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recyclerViewCourses);
        buttonAddCourse = findViewById(R.id.buttonAddCourse);
        buttonSearch = findViewById(R.id.buttonSearch);
        editTextSearchCourseName = findViewById(R.id.editTextSearchCourseName); // Đảm bảo rằng ID đúng
        searchEditText = findViewById(R.id.editTextSearchCourseName);
        databaseHelper = new DatabaseHelper(this);
        courseList = new ArrayList<>();

        // Thiết lập RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo adapter với danh sách trống ban đầu
        courseAdapter = new CourseAdapter(courseList, this, this, this);
        recyclerView.setAdapter(courseAdapter);

        // Sau khi khởi tạo adapter, gọi phương thức loadCourses để tải dữ liệu
        loadCourses();

        // Xử lý sự kiện nhấn nút "Add Course"
        buttonAddCourse.setOnClickListener(v -> showAddCourseDialog());

        // Khởi tạo sự kiện nhấn nút tìm kiếm
        buttonSearch.setOnClickListener(v -> searchCourses());

        // Thiết lập Spinner cho sắp xếp
        spinnerSortPrice = findViewById(R.id.spinnerSortPrice);
        spinnerSortPrice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Tùy chọn sắp xếp đã được chọn, vị trí: " + position);
                sortCoursesByPrice(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "Không có tùy chọn sắp xếp nào được chọn");
            }
        });
    }

    private void searchCourses() {
        String courseName = editTextSearchCourseName.getText().toString().trim(); // Sử dụng editTextSearchCourseName
        if (!courseName.isEmpty()) {
            courseList.clear();
            courseList.addAll(databaseHelper.searchCoursesByName(courseName));
            courseAdapter.notifyDataSetChanged();
        } else {
            loadCourses(); // Nếu không có gì được tìm kiếm, tải lại toàn bộ danh sách
        }
    }

    private void sortCoursesByPrice(int position) {
        if (position == 0) { // Sắp xếp theo giá tăng dần
            Collections.sort(courseList, Comparator.comparingDouble(Course::getPrice));
        } else if (position == 1) { // Sắp xếp theo giá giảm dần
            Collections.sort(courseList, (course1, course2) -> Double.compare(course2.getPrice(), course1.getPrice()));
        }
        courseAdapter.notifyDataSetChanged(); // Thông báo cho adapter biết dữ liệu đã thay đổi
    }

    private void loadCourses() {
        courseList.clear();
        courseList.addAll(databaseHelper.getAllCourses());
        courseAdapter.notifyDataSetChanged();
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadCourses(); // Gọi phương thức để tải lại danh sách khóa học
    }

    private void showAddCourseDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_course);

        // Thiết lập kích thước cho dialog
        dialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        EditText editTextCourseName = dialog.findViewById(R.id.editTextCourseName);
        Spinner spinnerDayOfWeek = dialog.findViewById(R.id.spinnerDayOfWeek);
        Spinner spinnerTypeOfClass = dialog.findViewById(R.id.spinnerTypeOfClass);
        EditText editTextTime = dialog.findViewById(R.id.editTextTime);
        EditText editTextCapacity = dialog.findViewById(R.id.editTextCapacity);
        EditText editTextDuration = dialog.findViewById(R.id.editTextDuration);
        EditText editTextPrice = dialog.findViewById(R.id.editTextPrice);
        EditText editTextDescription = dialog.findViewById(R.id.editTextDescription);
        Button buttonSave = dialog.findViewById(R.id.buttonSave);
        Button buttonCancel = dialog.findViewById(R.id.buttonCancel);

        // Thiết lập Adapter cho Spinner
        ArrayAdapter<CharSequence> dayAdapter = ArrayAdapter.createFromResource(this,
                R.array.days_of_week_array, android.R.layout.simple_spinner_item);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDayOfWeek.setAdapter(dayAdapter);

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.class_type_array, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTypeOfClass.setAdapter(typeAdapter);

        buttonSave.setOnClickListener(v -> {
            String courseName = editTextCourseName.getText().toString().trim();
            String dayOfWeek = spinnerDayOfWeek.getSelectedItem().toString();
            String type = spinnerTypeOfClass.getSelectedItem().toString();
            String time = editTextTime.getText().toString().trim();
            String capacityStr = editTextCapacity.getText().toString().trim();
            String duration = editTextDuration.getText().toString().trim();
            String priceStr = editTextPrice.getText().toString().trim();
            String description = editTextDescription.getText().toString().trim();

            // Kiểm tra thông tin đã đầy đủ chưa
            if (courseName.isEmpty() || time.isEmpty() || capacityStr.isEmpty() || duration.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Kiểm tra xem capacity và price có phải là số không
            if (!isNumeric(capacityStr) || !isNumeric(priceStr)) {
                Toast.makeText(this, "Capacity and Price must be numeric values!", Toast.LENGTH_SHORT).show();
                return;
            }

            int capacity = Integer.parseInt(capacityStr);
            double price = Double.parseDouble(priceStr);

            Course course = new Course();
            course.setCourseName(courseName);
            course.setDayOfWeek(dayOfWeek);
            course.setType(type);
            course.setTime(time);
            course.setCapacity(capacity);
            course.setDuration(duration);
            course.setPrice(price);
            course.setDescription(description);

            // Add course to Firestore
            String firestoreId = addCourseToFirestore(course, dialog);
            if (firestoreId != null) {
                course.setFirestoreId(firestoreId); // Set Firestore ID to course
                long id = databaseHelper.addCourse(course, firestoreId); // Save to SQLite
                if (id != -1) {
                    Toast.makeText(this, "Course added successfully!", Toast.LENGTH_SHORT).show();
                    loadCourses(); // Refresh course list
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Error adding course to database", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error adding course to Firestore", Toast.LENGTH_SHORT).show();
            }
        });

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private String addCourseToFirestore(Course course, Dialog dialog) {
        // Kiểm tra xem course có phải null không
        if (course == null) {
            Log.w("Firestore", "Course object is null");
            return null;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference newCourseRef = db.collection("courses").document();

        // Thêm dữ liệu khóa học vào Firestore
        newCourseRef.set(course)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Course added with ID: " + newCourseRef.getId());
                    dialog.dismiss(); // Đóng dialog khi thêm thành công
                    databaseHelper.updateCourseFirestoreId(course.getId(), newCourseRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error adding course", e);
                    Toast.makeText(dialog.getContext(), "Lỗi khi thêm khóa học vào Firestore!", Toast.LENGTH_SHORT).show();
                });

        return newCourseRef.getId();
    }

    private void syncCoursesToFirestore(Dialog dialog) {
        if (NetworkUtil.isNetworkAvailable(this)) {
            List<Course> coursesToSync = databaseHelper.getUnsyncedCourses();
            dialog.show(); // Hiển thị dialog khi bắt đầu đồng bộ

            for (Course course : coursesToSync) {
                // Kiểm tra nếu course không null
                if (course != null) {
                    String firestoreId = addCourseToFirestore(course, dialog);
                    Log.d("SyncCourses", "Firestore ID for course " + course.getId() + ": " + firestoreId);

                    if (firestoreId != null) {
                        databaseHelper.updateCourseFirestoreId(course.getId(), firestoreId);
                    }
                } else {
                    Log.w("SyncCourses", "Course is null for syncing");
                }
            }

            Toast.makeText(this, "All unsynced courses uploaded to Firestore!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No network connection available. Sync failed.", Toast.LENGTH_SHORT).show();
        }
    }

    // Phương thức kiểm tra xem chuỗi có phải là số không
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    @Override
    public void onAddClassClick(Course course) {
        showAddClassDialog(course);
    }
    private void showAddClassDialog(Course course) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_class);

        EditText editTextClassDate = dialog.findViewById(R.id.editTextClassDate);
        EditText editTextTeacher = dialog.findViewById(R.id.editTextTeacher);
        EditText editTextComments = dialog.findViewById(R.id.editTextComments);
        Button buttonSave = dialog.findViewById(R.id.buttonSaveClass);
        Button buttonCancel = dialog.findViewById(R.id.buttonCancelClass);

        TextView errorTextDate = dialog.findViewById(R.id.errorTextDate);
        TextView errorTextTeacher = dialog.findViewById(R.id.errorTextTeacher);
        TextView errorTextCourse = dialog.findViewById(R.id.errorTextCourse);

        errorTextDate.setVisibility(View.GONE);
        errorTextTeacher.setVisibility(View.GONE);
        errorTextCourse.setVisibility(View.GONE);

        buttonSave.setOnClickListener(v -> {
            String classDate = editTextClassDate.getText().toString().trim();
            String teacher = editTextTeacher.getText().toString().trim();
            String comments = editTextComments.getText().toString().trim();

            errorTextDate.setVisibility(View.GONE);
            errorTextTeacher.setVisibility(View.GONE);
            errorTextCourse.setVisibility(View.GONE);

            boolean isValid = true;

            if (classDate.isEmpty()) {
                errorTextDate.setText("Vui lòng nhập ngày lớp học!");
                errorTextDate.setTextColor(Color.RED);
                errorTextDate.setVisibility(View.VISIBLE);
                isValid = false;
            }
            if (teacher.isEmpty()) {
                errorTextTeacher.setText("Vui lòng nhập tên giáo viên!");
                errorTextTeacher.setTextColor(Color.RED);
                errorTextTeacher.setVisibility(View.VISIBLE);
                isValid = false;
            }
            if (course == null) {
                errorTextCourse.setText("Vui lòng chọn khóa học!");
                errorTextCourse.setTextColor(Color.RED);
                errorTextCourse.setVisibility(View.VISIBLE);
                isValid = false;
            }

            String courseDayOfWeek = course.getDayOfWeek();
            if (!validateDate(classDate, courseDayOfWeek)) {
                editTextClassDate.setError("Date must match the course's day of the week: " + courseDayOfWeek);
                return;
            }

            if (isValid) {
                // Kiểm tra kết nối mạng trước khi thêm lớp
                if (NetworkUtil.isNetworkAvailable(this)) {
                    ClassInstance classInstance = new ClassInstance();
                    classInstance.setCourseId(course.getId());
                    classInstance.setDate(classDate);
                    classInstance.setTeacher(teacher);
                    classInstance.setComments(comments);
                    classInstance.setCourseName(course.getCourseName());

                    // Thêm vào SQLite
                    long result = databaseHelper.addClassToCourse(classInstance, null);
                    if (result != -1) {
                        firestore.collection("class_instances")
                                .add(classInstance.toMap())
                                .addOnSuccessListener(documentReference -> {
                                    String firestoreId = documentReference.getId();

                                    // Chỉ cập nhật Firestore ID trong SQLite
                                    databaseHelper.updateFirestoreId(result, firestoreId);

                                    Toast.makeText(this, "Đã thêm lớp thành công và đồng bộ Firestore!", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi đồng bộ với Firestore", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Lỗi khi thêm lớp", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Không có kết nối mạng. Vui lòng kiểm tra lại.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onCourseDelete(int courseId, String firestoreId) {
        Log.d("ManageCoursesActivity", "Đang xóa khóa học với ID: " + courseId + " và Firestore ID: " + firestoreId);

        // Lấy khóa học từ SQLite để lấy firestoreId
        Course course = databaseHelper.getCourseById(courseId);
        if (course != null) {
            firestoreId = course.getFirestoreId(); // Gán firestoreId từ đối tượng Course

            if (firestoreId == null || firestoreId.isEmpty()) {
                Log.e("ManageCoursesActivity", "Không thể xóa khóa học: Firestore ID không hợp lệ.");
                Toast.makeText(this, "Không thể xóa khóa học: Firestore ID không hợp lệ", Toast.LENGTH_SHORT).show();
                return; // Dừng việc xóa nếu Firestore ID không hợp lệ
            }

            if (NetworkUtil.isNetworkAvailable(this)) {
                // Xóa khóa học từ SQLite
                boolean isDeletedFromSQLite = databaseHelper.deleteCourseByFirestoreId(firestoreId);
                if (isDeletedFromSQLite) {
                    // Xóa khóa học từ Firestore
                    deleteCourseFromFirestore(firestoreId); // Gọi phương thức mà không có ID khóa học
                    // Cập nhật danh sách và thông báo cho adapter
                    courseAdapter.removeCourseById(courseId);
                    Toast.makeText(this, "Khóa học và tất cả lớp học liên quan đã được xóa", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Lỗi khi xóa khóa học từ SQLite", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Không có kết nối mạng. Vui lòng kiểm tra lại.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Không tìm thấy khóa học với ID: " + courseId, Toast.LENGTH_SHORT).show();
        }
    }

    // Phương thức xóa khóa học từ Firestore
    private void deleteCourseFromFirestore(String firestoreId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("courses").document(firestoreId)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("ManageCoursesActivity", "Khóa học với Firestore ID: " + firestoreId + " đã được xóa khỏi Firestore."))
                .addOnFailureListener(e -> Log.e("ManageCoursesActivity", "Xóa khóa học từ Firestore thất bại: ", e));

        // Bước 1: Xóa tất cả các lớp học liên quan từ Firestore
        db.collection("class_instances")
                .whereEqualTo(CLASS_INSTANCE_COURSE_ID, firestoreId) // Sử dụng hằng số đã định nghĩa
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            db.collection("class_instances").document(document.getId()).delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Firestore", "Xóa thành công lớp học với ID: " + document.getId());
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Firestore", "Xóa lớp học thất bại: ", e);
                                    });
                        }
                    } else {
                        Log.e("Firestore", "Lỗi khi lấy lớp học: ", task.getException());
                    }
                });
    }

    private boolean validateDate(String date, String courseDayOfWeek) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()); // Adjusted format
        try {
            Date parsedDate = sdf.parse(date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedDate);

            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            String[] days = {"", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

            return days[dayOfWeek].equalsIgnoreCase(courseDayOfWeek);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }


}
