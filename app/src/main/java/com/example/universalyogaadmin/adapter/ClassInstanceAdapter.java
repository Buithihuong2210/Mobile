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

        holder.buttonEdit.setOnClickListener(v -> edit(classInstance)); // Gọi hàm edit khi nhấn nút chỉnh sửa


        holder.buttonDelete.setOnClickListener(v -> delete(classInstance)); // Gọi hàm delete khi nhấn nút xóa
    }

    private void edit(ClassInstance classInstance) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_edit_class_instance, null);
        dialogBuilder.setView(dialogView);

        EditText editTextDate = dialogView.findViewById(R.id.editTextDate);
        EditText editTextTeacher = dialogView.findViewById(R.id.editTextTeacher);
        EditText editTextComments = dialogView.findViewById(R.id.editTextComments);
        Spinner spinnerCourses = dialogView.findViewById(R.id.spinnerCourseName);

        // Thiết lập adapter cho Spinner
        ArrayAdapter<Course> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, courseList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourses.setAdapter(adapter);

        // Điền thông tin hiện tại vào các trường
        editTextDate.setText(classInstance.getDate());
        editTextTeacher.setText(classInstance.getTeacher());
        editTextComments.setText(classInstance.getComments());
        // Chọn khóa học hiện tại trong spinner
        int selectedPosition = 0;
        for (int i = 0; i < courseList.size(); i++) {
            if (courseList.get(i).getId() == classInstance.getCourseId()) {
                selectedPosition = i;
                break;
            }
        }
        spinnerCourses.setSelection(selectedPosition);

        dialogBuilder.setTitle("Edit Class Instance")
                .setPositiveButton("Save", (dialog, which) -> {
                    String date = editTextDate.getText().toString().trim();
                    String teacher = editTextTeacher.getText().toString().trim();
                    String comments = editTextComments.getText().toString().trim();
                    Course selectedCourse = (Course) spinnerCourses.getSelectedItem();

                    // Kiểm tra các trường nhập liệu
                    if (date.isEmpty() || teacher.isEmpty() || selectedCourse == null) {
                        Toast.makeText(context, "Vui lòng điền tất cả các trường bắt buộc!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Cập nhật thông tin vào ClassInstance
                    classInstance.setDate(date);
                    classInstance.setTeacher(teacher);
                    classInstance.setComments(comments);
                    classInstance.setCourseId(selectedCourse.getId());
                    classInstance.setCourseName(selectedCourse.getCourseName());

                    // Cập nhật vào cơ sở dữ liệu
                    if (databaseHelper.updateClassInstance(classInstance)) { // Giả sử bạn có phương thức updateClassInstance trong DatabaseHelper
                        notifyItemChanged(classInstanceList.indexOf(classInstance)); // Cập nhật RecyclerView
                        Toast.makeText(context, "Cập nhật lớp học thành công!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Cập nhật lớp học thất bại!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    private void delete(ClassInstance classInstance) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle("Xóa lớp học")
                .setMessage("Bạn có chắc chắn muốn xóa lớp học này?")
                .setPositiveButton("Có", (dialog, which) -> {
                    Log.d("ClassInstanceAdapter", "Deleting class instance with ID: " + classInstance.getId());
                    // Gọi phương thức xóa từ DatabaseHelper
                    if (databaseHelper.deleteClassInstance(classInstance.getId())) {
                        classInstanceList.remove(classInstance); // Xóa khỏi danh sách
                        notifyDataSetChanged(); // Cập nhật RecyclerView
                        Toast.makeText(context, "Lớp học đã được xóa!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Xóa lớp học thất bại!", Toast.LENGTH_SHORT).show();
                    }
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


}
