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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;


import java.util.List;

public class ClassInstanceAdapter extends RecyclerView.Adapter<ClassInstanceAdapter.ViewHolder> {
    private List<ClassInstance> classInstanceList;
    private List<Course> courseList; // List of courses
    private Context context;
    private DatabaseHelper databaseHelper;

    // Constructor for the adapter
    public ClassInstanceAdapter(List<ClassInstance> classInstanceList, List<Course> courseList, Context context) {
        this.classInstanceList = classInstanceList;
        this.courseList = courseList; // Assign the course list
        this.context = context;
        this.databaseHelper = new DatabaseHelper(context); // Khởi tạo DatabaseHelper
    }

    // New method to update the data in the adapter
    public void updateData(List<ClassInstance> newClassInstances) {
        this.classInstanceList.clear(); // Clear the current list
        this.classInstanceList.addAll(newClassInstances); // Add new items
        notifyDataSetChanged(); // Notify the RecyclerView to refresh
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

        holder.courseName.setText(classInstance.getCourseName());
        holder.date.setText(classInstance.getDate());
        holder.teacher.setText(classInstance.getTeacher());

        String commentsText = "Comment: " + classInstance.getComments();
        holder.comments.setText(commentsText);

        holder.buttonDetails.setOnClickListener(v -> {
            Intent intent = new Intent(context, ClassInstanceDetailActivity.class);

            Course course = getCourseById(classInstance.getCourseId()); // Call the new method

            if (classInstance != null && course != null) {
                intent.putExtra("classInstance", classInstance);
                intent.putExtra("course", course);
                context.startActivity(intent);
            } else {
                Log.e("ClassInstanceAdapter", "ClassInstance or Course is null");
                // Optionally, show a toast
            }
        });

        holder.buttonEdit.setOnClickListener(v -> {
            // Assuming classInstance has a method to get its ID
            int classInstanceId = classInstance.getId(); // Get the ID of the ClassInstance

            // Retrieve the ClassInstance from the database to get the firestoreId
            ClassInstance retrievedInstance = databaseHelper.getClassInstanceById(classInstanceId);

            if (retrievedInstance != null) {
                String firestoreId = retrievedInstance.getFirestoreId(); // Get the Firestore ID from the retrieved instance

                // Log the Firestore ID for debugging
                if (firestoreId != null) {
                    Log.d("FirestoreID", "Retrieved Firestore ID: " + firestoreId);
                } else {
                    Log.e("FirestoreID", "Firestore ID is null for ClassInstance ID: " + classInstanceId);
                }

                // Call the edit method with both parameters
                edit(firestoreId, retrievedInstance);
            } else {
                Log.e("Edit", "ClassInstance not found for ID: " + classInstanceId);
                Toast.makeText(context, "Lỗi: Không tìm thấy lớp học để chỉnh sửa.", Toast.LENGTH_SHORT).show();
            }
        });



        holder.buttonDelete.setOnClickListener(v -> {
            int classId = classInstance.getId(); // Lấy ID lớp học
            ClassInstance instanceToDelete = databaseHelper.getClassInstanceById(classId); // Lấy classInstance từ DB

            if (instanceToDelete != null) {
                String firestoreId = instanceToDelete.getFirestoreId(); // Lấy firestoreId từ classInstance
                Log.d("ClassInstanceAdapter", "Firestore ID: " + firestoreId);


                if (firestoreId != null && !firestoreId.isEmpty()) {
                    delete(firestoreId); // Gọi hàm delete với firestoreId
                } else {
                    Toast.makeText(context, "Không thể tìm thấy Firestore ID!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Không tìm thấy lớp học để xóa!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void edit(String firestoreId, ClassInstance classInstance) {
        // Check if classInstance is null
        if (classInstance == null) {
            Toast.makeText(context, "Không tìm thấy lớp học!", Toast.LENGTH_SHORT).show();
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

        // Setup the adapter for Spinner
        ArrayAdapter<Course> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, courseList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourses.setAdapter(adapter);

        // Fill in the current information into the fields
        editTextDate.setText(classInstance.getDate());
        editTextTeacher.setText(classInstance.getTeacher());
        editTextComments.setText(classInstance.getComments());

        // Select the current course in the spinner
        int selectedPosition = 0;
        for (int i = 0; i < courseList.size(); i++) {
            if (courseList.get(i).getId() == classInstance.getCourseId()) {
                selectedPosition = i;
                break;
            }
        }
        spinnerCourses.setSelection(selectedPosition);

        Log.d("Edit ClassInstance", "Current Firestore ID: " + firestoreId);

        dialogBuilder.setTitle("Edit Class Instance")
                .setPositiveButton("Save", (dialog, which) -> {
                    // Declare the variables as final for use in the lambda
                    final String date = editTextDate.getText().toString().trim();
                    final String teacher = editTextTeacher.getText().toString().trim();
                    final String comments = editTextComments.getText().toString().trim();
                    final Course selectedCourse = (Course) spinnerCourses.getSelectedItem();

                    // Validate input fields
                    if (date.isEmpty() || teacher.isEmpty() || selectedCourse == null) {
                        Toast.makeText(context, "Vui lòng điền tất cả các trường bắt buộc!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Update the ClassInstance
                    classInstance.setDate(date);
                    classInstance.setTeacher(teacher);
                    classInstance.setComments(comments);
                    classInstance.setCourseId(selectedCourse.getId());
                    classInstance.setCourseName(selectedCourse.getCourseName());

                    // Log Firestore ID
                    if (firestoreId == null) {
                        Log.e("Firestore", "firestoreId không hợp lệ!"); // Log error message
                    } else {
                        Log.d("Firestore", "firestoreId hợp lệ: " + firestoreId); // Log valid ID message
                    }

                    // Update SQLite database
                    if (databaseHelper.updateClassInstance(firestoreId, classInstance)) {
                        notifyItemChanged(classInstanceList.indexOf(classInstance)); // Update RecyclerView
                        Toast.makeText(context, "Cập nhật lớp học thành công!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Cập nhật lớp học thất bại!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }


    private void delete(String firestoreId) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle("Xóa lớp học")
                .setMessage("Bạn có chắc chắn muốn xóa lớp học này?")
                .setPositiveButton("Có", (dialog, which) -> {
                    Log.d("ClassInstanceAdapter", "Deleting class instance with Firestore ID: " + firestoreId);

                    // Xóa lớp học từ Firestore
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("class_instances").document(firestoreId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Nếu xóa thành công từ Firestore, xóa từ SQLite
                                if (databaseHelper.deleteClassInstance(firestoreId)) { // Gọi với firestoreId
                                    if (firestoreId != null) {
                                        classInstanceList.removeIf(instance ->
                                                firestoreId.equals(instance.getFirestoreId())); // Xóa khỏi danh sách
                                    }
                                    notifyDataSetChanged(); // Cập nhật RecyclerView
                                    Toast.makeText(context, "Lớp học đã được xóa!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context, "Xóa lớp học thất bại từ SQLite!", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("ClassInstanceAdapter", "Error deleting document from Firestore", e);
                                Toast.makeText(context, "Xóa lớp học thất bại từ Firestore!", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Không", (dialog, which) -> dialog.dismiss());

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

    private Course getCourseById(int courseId) {
        for (Course course : courseList) {
            if (course.getId() == courseId) {
                return course; // Return the matching course
            }
        }
        return null; // Return null if no matching course is found
    }

    public void loadClassInstances() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("class_instances").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    ClassInstance classInstance = new ClassInstance();
                    classInstance.setFirestoreId(document.getId()); // Gán firestoreId
                    // Gán các thuộc tính khác từ document

                    classInstanceList.add(classInstance); // Thêm vào danh sách
                }
                notifyDataSetChanged(); // Cập nhật RecyclerView
            } else {
                Log.e("ClassInstanceAdapter", "Error getting documents: ", task.getException());
            }
        });
    }

}
