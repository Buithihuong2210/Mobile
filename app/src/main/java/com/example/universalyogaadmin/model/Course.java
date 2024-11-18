package com.example.universalyogaadmin.model;
import java.io.Serializable;

public class Course implements Serializable {
    private int id;
    private String firestoreId;
    private String courseName;
    private String dayOfWeek;
    private String time;
    private int capacity;
    private String duration;
    private double price;
    private String type;
    private String description;

    public Course() {
    }

    public Course(int id, String name) {
        this.id = id;
        this.courseName = name;
    }

    public Course(int id, String firestoreId, String courseName, String dayOfWeek, String time,
                  int capacity, String duration, double price, String type, String description) {
        this.id = id;
        this.firestoreId = firestoreId; // Đảm bảo gán giá trị
        this.courseName = courseName;
        this.dayOfWeek = dayOfWeek;
        this.time = time;
        this.capacity = capacity;
        this.duration = duration;
        this.price = price;
        this.type = type;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCourseName() {
        return courseName;
    }

    @Override
    public String toString() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFirestoreId() {
        return firestoreId;
    }
    public void setFirestoreId(String firestoreId) {
        this.firestoreId = firestoreId;
    }

    public String getDetails() {
        return "Day: " + dayOfWeek + ", Time: " + time + ", Capacity: " + capacity + ", Price: £" + price;
    }

}
