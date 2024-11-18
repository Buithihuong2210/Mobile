package com.example.universalyogaadmin.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.universalyogaadmin.R;
import com.example.universalyogaadmin.activity.ClassInstanceDetailActivity;
import com.example.universalyogaadmin.model.ClassInstance;
import com.example.universalyogaadmin.model.Course;
import com.example.universalyogaadmin.database.DatabaseHelper;
import com.example.universalyogaadmin.utils.NetworkUtil;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Constructor to initialize the adapter with class instances, courses, and context
public class ClassInstanceAdapter extends RecyclerView.Adapter<ClassInstanceAdapter.ViewHolder> {
    private List<ClassInstance> classInstanceList;
    private List<Course> courseList;
    private Context context;
    private DatabaseHelper databaseHelper;

    public ClassInstanceAdapter(List<ClassInstance> classInstanceList, List<Course> courseList, Context context) {
        this.classInstanceList = classInstanceList;
        this.courseList = courseList;
        this.context = context;
        this.databaseHelper = new DatabaseHelper(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_class_instance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClassInstance classInstance = classInstanceList.get(position);

        // Set the data for the current item (e.g., course name, date, teacher, comments)
        holder.courseName.setText(classInstance.getCourseName());
        holder.date.setText(classInstance.getDate());
        holder.teacher.setText(classInstance.getTeacher());

        String commentsText = "Comment: " + classInstance.getComments();
        holder.comments.setText(commentsText);

        // Handle the "Deatails" button click
        holder.buttonDetails.setOnClickListener(v -> {
            Intent intent = new Intent(context, ClassInstanceDetailActivity.class);

            Course course = getCourseById(classInstance.getCourseId());

            if (classInstance != null && course != null) {
                intent.putExtra("classInstance", classInstance);
                intent.putExtra("course", course);
                context.startActivity(intent);
            } else {
                Log.e("ClassInstanceAdapter", "ClassInstance or Course is null");
            }
        });

        // Handle the "Edit" button click
        holder.buttonEdit.setOnClickListener(v -> {
            int classInstanceId = classInstance.getId();

            ClassInstance retrievedInstance = databaseHelper.getClassInstanceById(classInstanceId);

            if (retrievedInstance != null) {
                String firestoreId = retrievedInstance.getFirestoreId();

                if (firestoreId != null) {
                    Log.d("FirestoreID", "Retrieved Firestore ID: " + firestoreId);
                } else {
                    Log.e("FirestoreID", "Firestore ID is null for ClassInstance ID: " + classInstanceId);
                }
                edit(firestoreId, retrievedInstance);
            } else {
                Log.e("Edit", "ClassInstance not found for ID: " + classInstanceId);
                Toast.makeText(context, "Error: Class not found to edit.", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle the "Delete" button click
        holder.buttonDelete.setOnClickListener(v -> {
            int classId = classInstance.getId(); // Lấy ID lớp học
            ClassInstance instanceToDelete = databaseHelper.getClassInstanceById(classId);

            if (instanceToDelete != null) {
                String firestoreId = instanceToDelete.getFirestoreId();
                Log.d("ClassInstanceAdapter", "Firestore ID: " + firestoreId);


                if (firestoreId != null && !firestoreId.isEmpty()) {
                    delete(firestoreId);
                } else {
                    Toast.makeText(context, "Could not find Firestore ID!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Class not found to delete!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    // Method to edit the class instance
    private void edit(String firestoreId, ClassInstance classInstance) {
        if (classInstance == null) {
            Toast.makeText(context, "Class not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_edit_class_instance, null);
        dialogBuilder.setView(dialogView);

        EditText editTextDate = dialogView.findViewById(R.id.editTextDate);
        EditText editTextTeacher = dialogView.findViewById(R.id.editTextTeacher);
        EditText editTextComments = dialogView.findViewById(R.id.editTextComments);
        Spinner spinnerCourses = dialogView.findViewById(R.id.spinnerCourseName);

        ArrayAdapter<Course> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, courseList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourses.setAdapter(adapter);

        editTextDate.setText(classInstance.getDate());
        editTextTeacher.setText(classInstance.getTeacher());
        editTextComments.setText(classInstance.getComments());

        int selectedPosition = 0;
        for (int i = 0; i < courseList.size(); i++) {
            if (courseList.get(i).getId() == classInstance.getCourseId()) {
                selectedPosition = i;
                break;
            }
        }
        spinnerCourses.setSelection(selectedPosition);

        dialogBuilder.setTitle("Edit Class Instance")
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog alertDialog = dialogBuilder.create();

        alertDialog.setOnShowListener(dialog -> {
            Button saveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(view -> {
                if (!NetworkUtil.isNetworkAvailable(context)) {
                    Toast.makeText(context, "No network connection. Please check and try again.", Toast.LENGTH_SHORT).show();
                    return;
                }

                final String date = editTextDate.getText().toString().trim();
                final String teacher = editTextTeacher.getText().toString().trim();
                final String comments = editTextComments.getText().toString().trim();
                final Course selectedCourse = (Course) spinnerCourses.getSelectedItem();

                if (date.isEmpty() || teacher.isEmpty() || selectedCourse == null) {
                    Toast.makeText(context, "Please fill in all required fields!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String courseDayOfWeek = selectedCourse.getDayOfWeek();
                if (!validateDate(date, courseDayOfWeek)) {
                    Toast.makeText(context, "The date does not match the course's day of the week (" + courseDayOfWeek + ").", Toast.LENGTH_SHORT).show();
                    return;
                }

                classInstance.setDate(date);
                classInstance.setTeacher(teacher);
                classInstance.setComments(comments);
                classInstance.setCourseId(selectedCourse.getId());
                classInstance.setCourseName(selectedCourse.getCourseName());

                if (databaseHelper.updateClassInstance(firestoreId, classInstance)) {
                    notifyItemChanged(classInstanceList.indexOf(classInstance));
                    loadClassInstances(); // Tải lại danh sách sau khi cập nhật
                    Toast.makeText(context, "Class instance updated successfully!", Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss(); // Đóng dialog sau khi thành công
                } else {
                    Toast.makeText(context, "Failed to update class instance!", Toast.LENGTH_SHORT).show();
                }
            });

        });

        alertDialog.show();
    }

    private boolean validateDate(String date, String expectedDayOfWeek) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            sdf.setLenient(false);

            Date parsedDate = sdf.parse(date);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedDate);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

            String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
            String actualDayOfWeek = days[dayOfWeek - 1];

            Log.d("Debug", "Input date: " + actualDayOfWeek + ", Expected date: " + expectedDayOfWeek);
            return actualDayOfWeek.equalsIgnoreCase(expectedDayOfWeek);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Method to delete a class instance
    private void delete(String firestoreId) {
        if (!NetworkUtil.isNetworkAvailable(context)) {
            Toast.makeText(context, "No network connection. Please check and try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle("Delete Class")
                .setMessage("Are you sure you want to delete this class instance?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Log.d("ClassInstanceAdapter", "Deleting class instance with Firestore ID: " + firestoreId);

                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("classInstances").document(firestoreId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d("ClassInstanceAdapter", "Successfully deleted from Firestore.");

                                if (databaseHelper.deleteClassInstance(firestoreId)) {
                                    if (firestoreId != null) {
                                        classInstanceList.removeIf(instance ->
                                                firestoreId.equals(instance.getFirestoreId())); // Xóa khỏi danh sách
                                        loadClassInstances();
                                    }
                                    Toast.makeText(context, "Class deleted!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context, "Failed to delete class from SQLite!", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("ClassInstanceAdapter", "Error deleting document from Firestore", e);
                                Toast.makeText(context, "Failed to delete class from Firestore!", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss());

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public int getItemCount() {
        return classInstanceList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView courseName, date, teacher, comments;
        Button buttonDetails, buttonEdit, buttonDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            courseName = itemView.findViewById(R.id.textViewCourseName);
            date = itemView.findViewById(R.id.textViewDate);
            teacher = itemView.findViewById(R.id.textViewTeacher);
            comments = itemView.findViewById(R.id.textViewComments);
            buttonDetails = itemView.findViewById(R.id.buttonDetails);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }

    // Method to get the course by its ID
    private Course getCourseById(int courseId) {
        for (Course course : courseList) {
            if (course.getId() == courseId) {
                return course;
            }
        }
        return null;
    }

    private void loadClassInstances() {
        if (!NetworkUtil.isNetworkAvailable(context)) {
            Toast.makeText(context, "No network connection. Please check your connection and try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("classInstances").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    classInstanceList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        ClassInstance classInstance = document.toObject(ClassInstance.class);
                        classInstance.setFirestoreId(document.getId());
                        classInstanceList.add(classInstance);
                    }
                    notifyDataSetChanged(); // Update RecyclerView
                    Toast.makeText(context, "Class instances reloaded successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("ClassInstanceAdapter", "Error loading class instances", e);
                    Toast.makeText(context, "Failed to reload class instances!", Toast.LENGTH_SHORT).show();
                });
    }

}
