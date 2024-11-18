package com.example.universalyogaadmin.adapter;

import static com.example.universalyogaadmin.R.layout.item_course;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
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
import com.example.universalyogaadmin.database.DatabaseHelper;
import com.example.universalyogaadmin.model.Course;

import java.util.List;
import com.example.universalyogaadmin.listener.OnCourseDeleteListener;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {
    private List<Course> courseList;
    private OnCourseDeleteListener deleteListener;
    private Context context;
    private DatabaseHelper databaseHelper;
    private OnAddClassClickListener addClassClickListener;

    // Interface to handle add class click event
    public interface OnAddClassClickListener {
        void onAddClassClick(Course course);
    }

    // Constructor for the adapter, initializing required variables
    public CourseAdapter(List<Course> courseList, Context context, OnCourseDeleteListener deleteListener,
                         OnAddClassClickListener addClassClickListener) {
        this.courseList = courseList;
        this.context = context;
        this.deleteListener = deleteListener;
        this.databaseHelper = new DatabaseHelper(context);
        this.addClassClickListener = addClassClickListener;
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

        // Delete button action
        holder.buttonDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onCourseDelete(course.getId(), course.getFirestoreId());
            }
        });

        // Edit button action
        holder.buttonEdit.setOnClickListener(v -> {
            int courseId = course.getId();

            Course courseToEdit = databaseHelper.getCourseById(courseId);

            if (courseToEdit != null) {
                Log.d("CourseEdit", "Firestore ID: " + courseToEdit.getFirestoreId());

                Intent intent = new Intent(context, EditCourseActivity.class);
                intent.putExtra("courseId", courseToEdit.getId());
                intent.putExtra("firestoreId", courseToEdit.getFirestoreId());
                context.startActivity(intent);
            } else {
                Log.e("CourseEdit", "Course not found for ID: " + courseId);
            }
        });

        // Detail button action
        holder.buttonDetail.setOnClickListener(v -> {
            Intent intent = new Intent(context, CourseDetailActivity.class);
            intent.putExtra("course", course); // Truyền khóa học
            context.startActivity(intent); // Bắt đầu activity chi tiết
        });

        holder.buttonAddClass.setOnClickListener(v -> addClassClickListener.onAddClassClick(course));

    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    // Method to remove a course by ID
    public void removeCourseById(int courseId) {
        courseList.removeIf(course -> course.getId() == courseId);
        notifyDataSetChanged();
    }

    // ViewHolder class to hold references to the views for each course item
    public static class CourseViewHolder extends RecyclerView.ViewHolder {
        TextView textViewCourseName;
        TextView textViewDetails;
        Button buttonDelete;
        Button buttonEdit;
        Button buttonDetail;
        Button buttonAddClass;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewCourseName = itemView.findViewById(R.id.textViewCourseName);
            textViewDetails = itemView.findViewById(R.id.textViewDetails);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            buttonDetail = itemView.findViewById(R.id.buttonDetail);
            buttonAddClass = itemView.findViewById(R.id.buttonAddClass);
        }
    }
}
