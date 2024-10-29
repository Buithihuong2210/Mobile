package com.example.universalyogaadmin.model;
import java.io.Serializable;

public class ClassInstance implements Serializable {
    private int id;
    private int courseId;
    private String courseName;
    private String date;
    private String teacher;
    private String comments;


    public ClassInstance() {
    }

    public ClassInstance(int id, int courseId, String courseName, String date, String teacher, String comments) {
        this.id = id;
        this.courseId = courseId;
        this.courseName = courseName;
        this.date = date;
        this.teacher = teacher;
        this.comments = comments;
    }


    // Getters and Setters
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int getCourseId() {
        return courseId;
    }
    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }
    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }
    public String getTeacher() {
        return teacher;
    }
    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }
    public String getComments() {
        return comments;
    }
    public void setComments(String comments) {
        this.comments = comments;
    }
}
