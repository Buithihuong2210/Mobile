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
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class ManageCoursesActivity extends AppCompatActivity implements OnCourseDeleteListener,
        CourseAdapter.OnAddClassClickListener {

    private static final String TAG = "ManageCoursesActivity";
    private static final String CLASS_INSTANCE_COURSE_ID = "courseId";

    private RecyclerView recyclerView;
    private CourseAdapter courseAdapter;
    private List<Course> courseList;
    private DatabaseHelper databaseHelper;
    private FirebaseFirestore firestore;
    private Button buttonAddCourse, buttonSearch;
    private EditText editTextSearchCourseName;
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
        editTextSearchCourseName = findViewById(R.id.editTextSearchCourseName);
        databaseHelper = new DatabaseHelper(this);
        courseList = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        courseAdapter = new CourseAdapter(courseList, this, this, this);
        recyclerView.setAdapter(courseAdapter);

        loadCourses();

        buttonAddCourse.setOnClickListener(v -> showAddCourseDialog());

        buttonSearch.setOnClickListener(v -> searchCourses());

        spinnerSortPrice = findViewById(R.id.spinnerSortPrice);
        spinnerSortPrice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Sort option selected, position: " + position);
                sortCoursesByPrice(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "No sort option selected");
            }
        });
    }

    private void searchCourses() {
        String courseName = editTextSearchCourseName.getText().toString().trim();
        if (!courseName.isEmpty()) {
            courseList.clear();
            courseList.addAll(databaseHelper.searchCoursesByName(courseName));
            courseAdapter.notifyDataSetChanged();
        } else {
            loadCourses();
        }
    }

    private void sortCoursesByPrice(int position) {
        if (position == 0) {
            Collections.sort(courseList, Comparator.comparingDouble(Course::getPrice));
        } else if (position == 1) {
            Collections.sort(courseList, (course1, course2) -> Double.compare(course2.getPrice(), course1.getPrice()));
        }
        courseAdapter.notifyDataSetChanged();
    }

    private void loadCourses() {
        courseList.clear();
        courseList.addAll(databaseHelper.getAllCourses());
        courseAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCourses();
    }

    private void showAddCourseDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_course);

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

        ArrayAdapter<CharSequence> dayAdapter = ArrayAdapter.createFromResource(this,
                R.array.days_of_week_array, android.R.layout.simple_spinner_item);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDayOfWeek.setAdapter(dayAdapter);

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.class_type_array, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTypeOfClass.setAdapter(typeAdapter);

        buttonSave.setOnClickListener(v -> {
            if (!NetworkUtil.isNetworkAvailable(this)) {
                Toast.makeText(this, "No network connection. Please check and try again.", Toast.LENGTH_SHORT).show();
                return;
            }
            String courseName = editTextCourseName.getText().toString().trim();
            String dayOfWeek = spinnerDayOfWeek.getSelectedItem().toString();
            String type = spinnerTypeOfClass.getSelectedItem().toString();
            String time = editTextTime.getText().toString().trim();
            String capacityStr = editTextCapacity.getText().toString().trim();
            String duration = editTextDuration.getText().toString().trim();
            String priceStr = editTextPrice.getText().toString().trim();
            String description = editTextDescription.getText().toString().trim();
            if (courseName.isEmpty() || time.isEmpty() || capacityStr.isEmpty() || duration.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isNumeric(capacityStr) || !isNumeric(priceStr)) {
                Toast.makeText(this, "Capacity and Price must be numeric values!", Toast.LENGTH_SHORT).show();
                return;
            }
            int capacity = Integer.parseInt(capacityStr);
            double price = Double.parseDouble(priceStr);
            showConfirmationDialog(courseName, dayOfWeek, type, time, capacity, duration, price, description, dialog);
        });

        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showConfirmationDialog(String courseName, String dayOfWeek, String type, String time, int capacity,
                                        String duration, double price, String description, Dialog addCourseDialog) {
        final Dialog confirmDialog = new Dialog(this);
        confirmDialog.setContentView(R.layout.dialog_confirm_course);

        TextView textViewCourseName = confirmDialog.findViewById(R.id.textViewCourseName);
        TextView textViewDayOfWeek = confirmDialog.findViewById(R.id.textViewDayOfWeek);
        TextView textViewTypeOfClass = confirmDialog.findViewById(R.id.textViewTypeOfClass);
        TextView textViewTime = confirmDialog.findViewById(R.id.textViewTime);
        TextView textViewCapacity = confirmDialog.findViewById(R.id.textViewCapacity);
        TextView textViewDuration = confirmDialog.findViewById(R.id.textViewDuration);
        TextView textViewPrice = confirmDialog.findViewById(R.id.textViewPrice);
        TextView textViewDescription = confirmDialog.findViewById(R.id.textViewDescription);
        Button buttonConfirm = confirmDialog.findViewById(R.id.buttonConfirm);
        Button buttonEdit = confirmDialog.findViewById(R.id.buttonEdit);

        textViewCourseName.setText(courseName);
        textViewDayOfWeek.setText(dayOfWeek);
        textViewTypeOfClass.setText(type);
        textViewTime.setText(time);
        textViewCapacity.setText(String.valueOf(capacity));
        textViewDuration.setText(duration);
        textViewPrice.setText(String.format("$%.2f", price));
        textViewDescription.setText(description);

        buttonConfirm.setOnClickListener(v -> {
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
            String firestoreId = addCourseToFirestore(course, confirmDialog);
            Log.d("Firestore", "Retrieved Firestore ID in Confirmation Dialog: " + firestoreId);
            if (firestoreId != null) {
                course.setFirestoreId(firestoreId);
                long id = databaseHelper.addCourse(course, firestoreId);
                course.setId((int) id);

                if (id != -1) {
                    Toast.makeText(this, "Course added successfully!", Toast.LENGTH_SHORT).show();
                    loadCourses();
                    addCourseDialog.dismiss();
                    confirmDialog.dismiss();
                } else {
                    Toast.makeText(this, "Error adding course to database", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error adding course to Firestore", Toast.LENGTH_SHORT).show();
            }
        });

        buttonEdit.setOnClickListener(v -> confirmDialog.dismiss());

        confirmDialog.show();
    }


    private String addCourseToFirestore(Course course, Dialog dialog) {
        if (course == null) {
            Log.w("Firestore", "Course object is null");
            return null;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference newCourseRef = db.collection("courses").document();

        String firestoreId = newCourseRef.getId();
        Log.d("Firestore", "Generated Firestore ID: " + firestoreId);

        Map<String, Object> courseData = new HashMap<>();
        courseData.put("courseName", course.getCourseName());
        courseData.put("dayOfWeek", course.getDayOfWeek());
        courseData.put("time", course.getTime());
        courseData.put("capacity", course.getCapacity());
        courseData.put("duration", course.getDuration());
        courseData.put("price", course.getPrice());
        courseData.put("type", course.getType());
        courseData.put("description", course.getDescription());

        newCourseRef.set(courseData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Course successfully added with Firestore ID: " + firestoreId);
                    course.setFirestoreId(firestoreId);
                    databaseHelper.updateCourseFirestoreId(course.getId(), firestoreId);
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Failed to add course with Firestore ID: " + firestoreId, e);
                    Toast.makeText(dialog.getContext(), "Error adding course to Firestore!", Toast.LENGTH_SHORT).show();
                });

        return firestoreId;
    }

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

            if (classDate.isEmpty()) { errorTextDate.setText("Please enter class date!");
                errorTextDate.setTextColor(Color.RED);
                errorTextDate.setVisibility(View.VISIBLE);
                isValid = false;
            } if (teacher.isEmpty()) { errorTextTeacher.setText("Please enter teacher name!");
                errorTextTeacher.setTextColor(Color.RED);
                errorTextTeacher.setVisibility(View.VISIBLE);
                isValid = false;
            } if (course == null) { errorTextCourse.setText("Please select a course!");
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
                if (NetworkUtil.isNetworkAvailable(this)) {
                    ClassInstance classInstance = new ClassInstance();
                    classInstance.setCourseId(course.getId());
                    classInstance.setDate(classDate);
                    classInstance.setTeacher(teacher);
                    classInstance.setComments(comments);
                    classInstance.setCourseName(course.getCourseName());

                    long result = databaseHelper.addClassToCourse(classInstance, null);
                    if (result != -1) {
                        firestore.collection("classInstances")
                                .add(classInstance.toMap())
                                .addOnSuccessListener(documentReference -> {
                                    String firestoreId = documentReference.getId();

                                    databaseHelper.updateFirestoreId(result, firestoreId);

                                    Toast.makeText(this, "Class successfully added and synced Firestore!", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Error syncing with Firestore", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Error adding class", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "No network connection. Please check again.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onCourseDelete(int courseId, String firestoreId) {
        final int finalCourseId = courseId;
        final String finalFirestoreId = firestoreId;

        new AlertDialog.Builder(this)
                .setTitle("Delete course")
                .setMessage("Are you sure you want to delete this course?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Log. d("Firestore", "Deleting course with ID: " + finalCourseId + " and Firestore ID: " + finalFirestoreId);

                    Course course = databaseHelper.getCourseById(finalCourseId);
                    if ( course != null) {
                        String firestoreIdToUse = course.getFirestoreId();
                        Log.d("Firestore", "Retrieved Firestore ID: " + firestoreIdToUse);
                        if (firestoreIdToUse == null || firestoreIdToUse .isEmpty()) {
                            Log.e("ManageCoursesActivity", "Could not delete course: Invalid Firestore ID.");
                            Toast.makeText(this, "Could not delete course: Invalid Firestore ID", Toast.LENGTH_SHORT). show();
                            return;
                        }

                        if (NetworkUtil.isNetworkAvailable(this)) {
                            boolean isDeletedFromSQLite = databaseHelper.deleteCourseByFirestoreId(firestoreIdToUse);
                            if (isDeletedFromSQLite) {
                                deleteCourseFromFirestore(firestoreIdToUse) ;
                                courseAdapter.removeCourseById(finalCourseId);
                                Toast.makeText(this, "The course and all associated classes have been deleted", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Error deleting course from SQLite", Toast .LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "No network connection. Please check again.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText( this, "Course with ID: " + finalCourseId, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }
    private void deleteCourseFromFirestore(String firestoreId) {
        Log.d("Firestore", "Using Firestore ID to delete: " + firestoreId);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        WriteBatch batch = db.batch();

        DocumentReference courseRef = db.collection("courses").document(firestoreId);
        batch.delete(courseRef);
        Log.d("Firestore", "Added course with Firestore ID: " + firestoreId + " to delete batch.");

        db.collection("classInstances")
                .whereEqualTo(CLASS_INSTANCE_COURSE_ID, firestoreId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            Log.d("Firestore", "Found " + task.getResult().size() + " class associated with course with Firestore ID: " + firestoreId);
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                batch.delete(document.getReference());
                                Log.d("Firestore", "Added class with ID: " + document.getId() + " to delete batch.");                            }

                            batch.commit()
                                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "Successfully deleted course and associated classes from Firestore."))
                                    .addOnFailureListener(e -> Log.e("Firestore", "Error deleting course and associated classes from Firestore: ", e));

                        } else {
                            Log.d("Firestore", "No classes found associated with course with Firestore ID: " + firestoreId);

                            batch.commit()
                                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "Successfully deleted course without related class."))
                                    .addOnFailureListener(e -> Log.e("Firestore", "Error while deleting course: ", e));
                        }
                    } else {
                        Log.e("Firestore", "Error while querying related classes: ", task.getException());
                    }
                });
    }

    private boolean validateDate(String date, String courseDayOfWeek) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
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
