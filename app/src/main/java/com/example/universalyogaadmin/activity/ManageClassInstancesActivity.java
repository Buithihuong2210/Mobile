package com.example.universalyogaadmin.activity;

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
import com.example.universalyogaadmin.model.Course;
import com.example.universalyogaadmin.utils.NetworkUtil;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ManageClassInstancesActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EditText editTextSearchTeacher;
    private EditText editTextSearchDate;
    private Button buttonSearch;
    private ClassInstanceAdapter adapter;
    private DatabaseHelper databaseHelper;
    private List<ClassInstance> classInstanceList;
    private List<Course> courseList;

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

        courseList = databaseHelper.getAllCourses();
        adapter = new ClassInstanceAdapter(classInstanceList, courseList, this);
        recyclerView.setAdapter(adapter);

        loadClassInstances();

        buttonAddClassInstance.setOnClickListener(v -> showAddClassDialog());

        editTextSearchTeacher = findViewById(R.id.editTextSearchTeacher);
        editTextSearchDate = findViewById(R.id.editTextSearchDate);
        buttonSearch = findViewById(R.id.buttonSearch);

        buttonSearch.setOnClickListener(v -> performSearch());
    }

    private void performSearch() {
        String teacherName = editTextSearchTeacher.getText().toString().trim();
        String date = editTextSearchDate.getText().toString().trim();

        if (teacherName.isEmpty() && date.isEmpty()) {
            classInstanceList.clear();
            classInstanceList.addAll(databaseHelper.getAllClassInstances());
            adapter.notifyDataSetChanged();
        } else {
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

    @Override
    protected void onResume() {
        super.onResume();
        loadClassInstances();
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

        ArrayAdapter<Course> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courseList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourses.setAdapter(adapter);

        dialogBuilder.setTitle("Add Class Instance")
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog alertDialog = dialogBuilder.create();

        alertDialog.setOnShowListener(dialog -> {
            Button addButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            addButton.setOnClickListener(v -> {
                if (!NetworkUtil.isNetworkAvailable(this)) {
                    Toast.makeText(this, "No network connection. Please check and try again.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (validateInput(editTextDate, editTextTeacher, spinnerCourses)) {
                    String date = editTextDate.getText().toString().trim();
                    String teacher = editTextTeacher.getText().toString().trim();
                    String comments = editTextComments.getText().toString().trim();
                    Course selectedCourse = (Course) spinnerCourses.getSelectedItem();

                    ClassInstance newClassInstance = new ClassInstance();
                    newClassInstance.setCourseId(selectedCourse.getId());
                    newClassInstance.setCourseName(selectedCourse.getCourseName());
                    newClassInstance.setDate(date);
                    newClassInstance.setTeacher(teacher);
                    newClassInstance.setComments(comments);

                    Log.d("ManageClassInstances", "firestoreId value: " + newClassInstance.getFirestoreId());
                    long newId = databaseHelper.addClassInstance(newClassInstance, newClassInstance.getFirestoreId());
                    if (newId != -1) {
                        newClassInstance.setId((int) newId);
                        classInstanceList.add(newClassInstance);
                        loadClassInstances();
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "Class added successfully!", Toast.LENGTH_SHORT).show();

                        if (NetworkUtil.isNetworkAvailable(this)) {
                            syncWithFirestore(newClassInstance);
                        } else {
                            Toast.makeText(this, "No network connection. Class will be synced later.", Toast.LENGTH_SHORT).show();
                        }
                        alertDialog.dismiss ();
                    } else {
                        Toast.makeText(this, "Add class failed!", Toast.LENGTH_SHORT).show();
                    }
                    Log.d("DB_INSERT", "Add class learn with ID: " + newId);
                }
            });
        });

        alertDialog.show();
    }

    private boolean validateInput(EditText editTextDate, EditText editTextTeacher, Spinner spinnerCourses) { String date = editTextDate.getText().toString().trim();
        String teacher = editTextTeacher.getText().toString().trim();
        Course selectedCourse = (Course) spinnerCourses.getSelectedItem();

        if (date.isEmpty() || teacher.isEmpty() || selectedCourse == null) { Toast.makeText(this, "Please fill in all required fields!", Toast.LENGTH_SHORT).show();
            return false;
        } String courseDayOfWeek = selectedCourse.getDayOfWeek();


        if (!validateDate(date, courseDayOfWeek)) {
            Toast.makeText(this, "The date must match the day of the week " + courseDayOfWeek + " of the course.", Toast.LENGTH_SHORT).show();
            return false;
        } return true;
    }

    public void syncWithFirestore(ClassInstance classInstance) {
        if (!NetworkUtil.isNetworkAvailable(this)) {
            Toast.makeText(this, "No network connection. Class will be synced later.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("classInstances")
                .add(classInstance.toMap())
                .addOnSuccessListener(documentReference -> {
                    String firestoreId = documentReference.getId();
                    classInstance.setFirestoreId(firestoreId);

                    long updateResult = databaseHelper.updateFirestoreIdInSQLite(classInstance.getId(), firestoreId);
                    if (updateResult == -1) {
                        Log.e("Firestore", "Failed to update Firestore ID into SQLite");
                    } else {
                        Log.d("Firestore", "Successfully updated Firestore ID in SQLite: " + firestoreId);
                    }

                    Log.d("Firestore", "Class has been synced to Firestore with ID: " + firestoreId);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error syncing with Firestore", e));
    }

    private boolean validateDate(String date, String courseDayOfWeek) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            sdf.setLenient(false); // Disallow invalid dates
            Date parsedDate = sdf.parse(date);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedDate);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

            if (dayOfWeek == Calendar.SUNDAY) {
                dayOfWeek = 7;
            } else {
                dayOfWeek--;
            }

            int courseDayOfWeekInt = convertDayOfWeekStringToInt(courseDayOfWeek);

            return dayOfWeek == courseDayOfWeekInt;

        } catch (ParseException e) {
            Toast.makeText(this, "Invalid date, please enter in dd/MM/yyyy format!", Toast.LENGTH_SHORT).show();
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
                return -1;
        }
    }

    private void loadClassInstances() {
        List<ClassInstance> updatedList = databaseHelper.getAllClassInstances();
        classInstanceList.clear();
        classInstanceList.addAll(updatedList);
        adapter.notifyDataSetChanged();
    }

}
