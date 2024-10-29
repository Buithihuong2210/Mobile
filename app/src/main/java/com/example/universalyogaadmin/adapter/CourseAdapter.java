package com.example.universalyogaadmin.adapter;

import static com.example.universalyogaadmin.R.layout.item_course;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.universalyogaadmin.R;
import com.example.universalyogaadmin.activity.CourseDetailActivity;
import com.example.universalyogaadmin.activity.EditCourseActivity;
import com.example.universalyogaadmin.activity.ManageCoursesActivity;
import com.example.universalyogaadmin.database.DatabaseHelper;
import com.example.universalyogaadmin.model.Course;

import java.util.List;
import com.example.universalyogaadmin.listener.OnCourseDeleteListener; // Add this line

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {
    private List<Course> courseList;
    private OnCourseDeleteListener deleteListener;
    private Context context;
    private DatabaseHelper databaseHelper;
    private OnAddClassClickListener addClassClickListener; // Step 1: Declare the listener


    public interface OnAddClassClickListener { // Step 2: Define the interface
        void onAddClassClick(Course course);
    }


    public CourseAdapter(List<Course> courseList, Context context, OnCourseDeleteListener deleteListener, OnAddClassClickListener addClassClickListener) {
        this.courseList = courseList;
        this.context = context;
        this.deleteListener = deleteListener;
        this.databaseHelper = new DatabaseHelper(context);
        this.addClassClickListener = addClassClickListener; // Step 3: Initialize the listener

    }


    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(item_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courseList.get(position);
        holder.textViewCourseName.setText(course.getCourseName());
        holder.textViewDetails.setText(course.getDetails());

        holder.buttonDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onCourseDelete(course.getId()); // Call the delete method on the listener
            }
        });

        holder.buttonEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditCourseActivity.class);
            intent.putExtra("course", course); // Gửi đối tượng course qua intent
            context.startActivity(intent);
        });



        holder.buttonDetail.setOnClickListener(v -> {
            Intent intent = new Intent(context, CourseDetailActivity.class);
            intent.putExtra("course", course); // Truyền khóa học
            context.startActivity(intent); // Bắt đầu activity chi tiết
        });

        // Step 4: Set up click listener for "Add Class" button
        holder.buttonAddClass.setOnClickListener(v -> addClassClickListener.onAddClassClick(course));

    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    private void deleteCourse(int courseId) {
        boolean isDeleted = databaseHelper.deleteCourse(courseId);
        if (isDeleted) {
            // Cập nhật danh sách và thông báo
            courseList.removeIf(course -> course.getId() == courseId);
            notifyDataSetChanged();
        }
    }

    public static class CourseViewHolder extends RecyclerView.ViewHolder {
        TextView textViewCourseName;
        TextView textViewDetails;
        Button buttonDelete;
        Button buttonEdit;
        Button buttonDetail;
        Button buttonAddClass; // Step 1: Declare buttonAddClass

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewCourseName = itemView.findViewById(R.id.textViewCourseName);
            textViewDetails = itemView.findViewById(R.id.textViewDetails);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            buttonDetail = itemView.findViewById(R.id.buttonDetail);
            buttonAddClass = itemView.findViewById(R.id.buttonAddClass); // Step 1: Initialize buttonAddClass
        }
    }
}
