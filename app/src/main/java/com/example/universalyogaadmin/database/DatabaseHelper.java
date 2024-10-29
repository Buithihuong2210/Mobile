package com.example.universalyogaadmin.database;

import android.content.ContentValues;
import android.database.SQLException;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.universalyogaadmin.model.ClassInstance;
import com.example.universalyogaadmin.model.Course;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper"; // Đặt tag cho logging

    private static final String DATABASE_NAME = "yoga_classes.db";
    private static final int DATABASE_VERSION = 2; // Tăng phiên bản nếu có thay đổi
    private static final String TABLE_COURSES = "courses";
    private static final String TABLE_CLASS_INSTANCES = "class_instances";

    private static final String COURSE_ID = "id";
    private static final String CLASS_INSTANCE_ID = "id";
    private static final String CLASS_INSTANCE_COURSE_ID = "course_id";
    private static final String CLASS_INSTANCE_DATE = "date";
    private static final String CLASS_INSTANCE_TEACHER = "teacher";
    private static final String CLASS_INSTANCE_COMMENTS = "comments";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_COURSES);
        db.execSQL(CREATE_TABLE_CLASS_INSTANCES);
    }

    private static final String CREATE_TABLE_COURSES = "CREATE TABLE " + TABLE_COURSES + " (" +
            COURSE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "courseName TEXT, " + // Đặt tên cột là courseName
            "dayOfWeek TEXT, " +
            "time TEXT, " +
            "capacity INTEGER, " +
            "duration TEXT, " +
            "price REAL, " +
            "type TEXT, " +
            "description TEXT " +
            ")";

    private static final String CREATE_TABLE_CLASS_INSTANCES = "CREATE TABLE " + TABLE_CLASS_INSTANCES + " (" +
            CLASS_INSTANCE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            CLASS_INSTANCE_COURSE_ID + " INTEGER NOT NULL," +
            "courseName TEXT," + // Thêm dòng này
            CLASS_INSTANCE_DATE + " TEXT NOT NULL," +
            CLASS_INSTANCE_TEACHER + " TEXT NOT NULL," +
            CLASS_INSTANCE_COMMENTS + " TEXT," +
            "FOREIGN KEY(" + CLASS_INSTANCE_COURSE_ID + ") REFERENCES " + TABLE_COURSES + "(" + COURSE_ID + ")" +
            ");";



    public long addCourse(Course course, String firestoreId) {
        SQLiteDatabase db = this.getWritableDatabase(); // Mở cơ sở dữ liệu ở chế độ ghi
        ContentValues values = new ContentValues();

        // Đặt các giá trị vào ContentValues
        values.put("courseName", course.getCourseName()); // Lưu tên khóa học
        values.put("dayOfWeek", course.getDayOfWeek()); // Lưu ngày trong tuần
        values.put("time", course.getTime()); // Lưu thời gian
        values.put("capacity", course.getCapacity()); // Lưu sức chứa
        values.put("duration", course.getDuration()); // Lưu thời gian khóa học
        values.put("price", course.getPrice()); // Lưu giá
        values.put("type", course.getType()); // Lưu loại khóa học
        values.put("description", course.getDescription()); // Lưu mô tả
        values.put("firestoreId", firestoreId); // Lưu ID Firestore

        long newRowId = -1; // Khởi tạo ID hàng mới

        try {
            // Chèn dữ liệu vào bảng courses và nhận ID hàng mới
            newRowId = db.insert(TABLE_COURSES, null, values);

            // Ghi log thông tin thêm khóa học
            Log.d("Database", "Added Course: " + course.getCourseName() + ", ID: " + newRowId);
        } catch (Exception e) {
            // Ghi log lỗi nếu có
            Log.e("Database", "Error adding course: " + e.getMessage());
        } finally {
            // Đóng cơ sở dữ liệu
            db.close();
        }

        // Trả về ID hàng mới
        return newRowId;
    }


    public List<Course> getAllCourses() {
        List<Course> courseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_COURSES, null);

        if (cursor.moveToFirst()) {
            do {
                Course course = new Course();
                course.setId(cursor.getInt(cursor.getColumnIndex(COURSE_ID)));
                course.setCourseName(cursor.getString(cursor.getColumnIndex("courseName")));
                course.setDayOfWeek(cursor.getString(cursor.getColumnIndex("dayOfWeek")));
                course.setTime(cursor.getString(cursor.getColumnIndex("time")));
                course.setCapacity(cursor.getInt(cursor.getColumnIndex("capacity")));
                course.setDuration(cursor.getString(cursor.getColumnIndex("duration")));
                course.setPrice(cursor.getDouble(cursor.getColumnIndex("price")));
                course.setType(cursor.getString(cursor.getColumnIndex("type")));
                course.setDescription(cursor.getString(cursor.getColumnIndex("description")));
                courseList.add(course);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return courseList;
    }


    public boolean deleteCourse(int courseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_COURSES, "id = ?", new String[]{String.valueOf(courseId)});
        db.close();
        return rowsAffected > 0; // Return true if delete was successful
    }

    public boolean updateCourse(Course course) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("courseName", course.getCourseName());
        values.put("dayOfWeek", course.getDayOfWeek());
        values.put("time", course.getTime());
        values.put("capacity", course.getCapacity());
        values.put("duration", course.getDuration());
        values.put("price", course.getPrice());
        values.put("type", course.getType());
        values.put("description", course.getDescription());

        // Update the course by id
        int rowsAffected = db.update(TABLE_COURSES, values, "id = ?", new String[]{String.valueOf(course.getId())});
        db.close();
        return rowsAffected > 0; // Return true if update was successful
    }

    public Course getCourse(int courseId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_COURSES, null, "id = ?", new String[]{String.valueOf(courseId)}, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                Course course = new Course();
                course.setId(cursor.getInt(cursor.getColumnIndex(COURSE_ID)));
                course.setCourseName(cursor.getString(cursor.getColumnIndex("courseName")));
                course.setDayOfWeek(cursor.getString(cursor.getColumnIndex("dayOfWeek")));
                course.setTime(cursor.getString(cursor.getColumnIndex("time")));
                course.setCapacity(cursor.getInt(cursor.getColumnIndex("capacity")));
                course.setDuration(cursor.getString(cursor.getColumnIndex("duration")));
                course.setPrice(cursor.getDouble(cursor.getColumnIndex("price")));
                course.setType(cursor.getString(cursor.getColumnIndex("type")));
                course.setDescription(cursor.getString(cursor.getColumnIndex("description")));
                cursor.close();
                return course;
            }
            cursor.close();
        }
        return null; // Return null if course not found
    }

    public List<String> getAllCourseNames() {
        List<String> courseNames = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT course_name FROM courses", null);

        if (cursor.moveToFirst()) {
            do {
                courseNames.add(cursor.getString(0)); // Get the course name
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return courseNames;
    }


    public long addClassInstance(ClassInstance classInstance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(CLASS_INSTANCE_COURSE_ID, classInstance.getCourseId());
        values.put("courseName", classInstance.getCourseName()); // Thêm tên khóa học vào giá trị
        values.put(CLASS_INSTANCE_DATE, classInstance.getDate());
        values.put(CLASS_INSTANCE_TEACHER, classInstance.getTeacher());
        values.put(CLASS_INSTANCE_COMMENTS, classInstance.getComments());

        long id = db.insert(TABLE_CLASS_INSTANCES, null, values);
        db.close();
        return id;
    }



    public List<ClassInstance> getAllClassInstances() {
        List<ClassInstance> classInstances = new ArrayList<>();

        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();

            // Truy vấn JOIN giữa class_instances và courses để lấy courseName
            String query = "SELECT ci.*, c.courseName AS courseName " +
                    "FROM " + TABLE_CLASS_INSTANCES + " ci " +
                    "JOIN " + TABLE_COURSES + " c ON ci." + CLASS_INSTANCE_COURSE_ID + " = c." + COURSE_ID;

            cursor = db.rawQuery(query, null);

            if (cursor.moveToFirst()) {
                do {
                    ClassInstance classInstance = new ClassInstance();
                    classInstance.setId(cursor.getInt(cursor.getColumnIndexOrThrow(CLASS_INSTANCE_ID)));
                    classInstance.setCourseId(cursor.getInt(cursor.getColumnIndexOrThrow(CLASS_INSTANCE_COURSE_ID)));

                    // Get course name safely
                    classInstance.setCourseName(cursor.getString(cursor.getColumnIndexOrThrow("courseName")));
                    classInstance.setDate(cursor.getString(cursor.getColumnIndexOrThrow(CLASS_INSTANCE_DATE)));
                    classInstance.setTeacher(cursor.getString(cursor.getColumnIndexOrThrow(CLASS_INSTANCE_TEACHER)));
                    classInstance.setComments(cursor.getString(cursor.getColumnIndexOrThrow(CLASS_INSTANCE_COMMENTS)));

                    classInstances.add(classInstance);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error retrieving class instances", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        return classInstances;
    }



    public boolean deleteClassInstance(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Ghi log ID trước khi xóa
        Log.d("DatabaseHelper", "Attempting to delete class instance with ID: " + id);

        int rowsDeleted = db.delete(TABLE_CLASS_INSTANCES, CLASS_INSTANCE_ID + " = ?", new String[]{String.valueOf(id)});

        if (rowsDeleted > 0) {
            Log.d("DatabaseHelper", "Successfully deleted class instance with ID: " + id);
            return true; // Trả về true nếu xóa thành công
        } else {
            Log.e("DatabaseHelper", "Failed to delete class instance with ID: " + id);
            return false; // Trả về false nếu xóa không thành công
        }
    }

    public boolean updateClassInstance(ClassInstance classInstance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(CLASS_INSTANCE_COURSE_ID, classInstance.getCourseId());
        contentValues.put(CLASS_INSTANCE_DATE, classInstance.getDate());
        contentValues.put(CLASS_INSTANCE_TEACHER, classInstance.getTeacher());
        contentValues.put(CLASS_INSTANCE_COMMENTS, classInstance.getComments());

        int rowsAffected = db.update(TABLE_CLASS_INSTANCES, contentValues, CLASS_INSTANCE_ID + " = ?",
                new String[]{String.valueOf(classInstance.getId())});

        return rowsAffected > 0; // Trả về true nếu cập nhật thành công
    }


    public int getCourseIdByName(String courseName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_COURSES, new String[]{COURSE_ID}, "courseName = ?", new String[]{courseName}, null, null, null);

        int courseId = -1; // Initialize courseId to -1 (not found)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                courseId = cursor.getInt(cursor.getColumnIndex(COURSE_ID)); // Get the course ID
            }
            cursor.close();
        }
        db.close();
        return courseId; // Return the course ID or -1 if not found
    }

    public long addClassToCourse(ClassInstance classInstance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(CLASS_INSTANCE_COURSE_ID, classInstance.getCourseId());
        values.put("courseName", classInstance.getCourseName());
        values.put(CLASS_INSTANCE_DATE, classInstance.getDate());
        values.put(CLASS_INSTANCE_TEACHER, classInstance.getTeacher());
        values.put(CLASS_INSTANCE_COMMENTS, classInstance.getComments());

        Log.d("DatabaseHelper", "Inserting class instance: " + values.toString());

        long result = db.insert(TABLE_CLASS_INSTANCES, null, values);
        db.close();
        return result;
    }


    public Course getCourseById(int courseId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("courses", null, "id=?", new String[]{String.valueOf(courseId)}, null, null, null);
        Course course = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                course = new Course();
                course.setId(cursor.getInt(cursor.getColumnIndex("id")));
                course.setCourseName(cursor.getString(cursor.getColumnIndex("course_name")));
                course.setDayOfWeek(cursor.getString(cursor.getColumnIndex("day_of_week")));
                course.setTime(cursor.getString(cursor.getColumnIndex("time")));
                course.setCapacity(cursor.getInt(cursor.getColumnIndex("capacity")));
                course.setDuration(cursor.getString(cursor.getColumnIndex("duration")));
                course.setPrice(cursor.getDouble(cursor.getColumnIndex("price")));
                course.setType(cursor.getString(cursor.getColumnIndex("type")));
                course.setDescription(cursor.getString(cursor.getColumnIndex("description")));
            }
            cursor.close();
        }
        return course;
    }

    public List<ClassInstance> searchClassInstances(String teacherName, String date) {
        List<ClassInstance> classInstances = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM ClassInstances WHERE 1=1";
        List<String> selectionArgs = new ArrayList<>();

        if (!teacherName.isEmpty()) {
            query += " AND teacher LIKE ?";
            selectionArgs.add("%" + teacherName + "%");
        }
        if (!date.isEmpty()) {
            query += " AND date = ?";
            selectionArgs.add(date);
        }

        Cursor cursor = db.rawQuery(query, selectionArgs.toArray(new String[0]));

        if (cursor.moveToFirst()) {
            do {
                ClassInstance instance = new ClassInstance(
                        cursor.getInt(cursor.getColumnIndex("id")),
                        cursor.getInt(cursor.getColumnIndex("courseId")),      // Lấy courseId từ cursor
                        cursor.getString(cursor.getColumnIndex("courseName")), // Lấy courseName từ cursor
                        cursor.getString(cursor.getColumnIndex("date")),
                        cursor.getString(cursor.getColumnIndex("teacher")),
                        cursor.getString(cursor.getColumnIndex("comments"))     // Lấy comments từ cursor
                );
                classInstances.add(instance);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return classInstances;
    }

    public List<Course> searchCoursesByName(String courseName) {
        List<Course> courses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM courses WHERE courseName LIKE ?";
        Cursor cursor = null; // Khai báo cursor ở đây

        try {
            Log.i("DatabaseHelper", "Executing query: " + query + " with parameter: " + courseName);
            cursor = db.rawQuery(query, new String[]{"%" + courseName + "%"});

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndex("id"));
                    String name = cursor.getString(cursor.getColumnIndex("courseName"));
                    String dayOfWeek = cursor.getString(cursor.getColumnIndex("dayOfWeek")); // Thêm dayOfWeek
                    String time = cursor.getString(cursor.getColumnIndex("time")); // Thêm time
                    int capacity = cursor.getInt(cursor.getColumnIndex("capacity")); // Thêm capacity
                    String duration = cursor.getString(cursor.getColumnIndex("duration")); // Thêm duration
                    double price = cursor.getDouble(cursor.getColumnIndex("price"));
                    String type = cursor.getString(cursor.getColumnIndex("type")); // Thêm type
                    String description = cursor.getString(cursor.getColumnIndex("description")); // Thêm description

                    Course course = new Course(id, name, dayOfWeek, time, capacity, duration, price, type, description);
                    courses.add(course);
                    Log.i("DatabaseHelper", "Course added: " + course.getCourseName());
                } while (cursor.moveToNext());
            } else {
                Log.i("DatabaseHelper", "No courses found for the search query: " + courseName);
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error while searching for courses: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return courses;
    }



    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Xóa bảng courses nếu tồn tại
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_COURSES);
            // Xóa bảng class_instances nếu tồn tại
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLASS_INSTANCES);
            // Tạo lại các bảng
            onCreate(db);
        }
        // Thêm điều kiện cho các phiên bản cao hơn nếu cần
        if (oldVersion < 3) {
            // Nếu bạn có thay đổi ở phiên bản 3, thực hiện ở đây
            // Ví dụ: db.execSQL("ALTER TABLE ..."); hoặc các thay đổi khác
        }
    }



}
