package com.example.universalyogaadmin.database;

import android.content.ContentValues;
import android.database.SQLException;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import com.example.universalyogaadmin.model.ClassInstance;
import com.example.universalyogaadmin.model.Course;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper"; // Đặt tag cho logging

    private static final String DATABASE_NAME = "yoga_classes.db";
    private static final int DATABASE_VERSION = 4; // Tăng phiên bản nếu có thay đổi
    private static final String TABLE_COURSES = "courses";
    private static final String TABLE_CLASS_INSTANCES = "class_instances";

    private static final String COURSE_ID = "id";
    private static final String CLASS_INSTANCE_ID = "id";
    private static final String CLASS_INSTANCE_COURSE_ID = "course_id";
    private static final String CLASS_INSTANCE_DATE = "date";
    private static final String CLASS_INSTANCE_TEACHER = "teacher";
    private static final String CLASS_INSTANCE_COMMENTS = "comments";
    private static final String CLASS_INSTANCE_FIRESTORE_ID = "firestoreId"; // New field for Firestore ID


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
            "description TEXT, " +
            "firestoreId TEXT" +
            ")";

    private static final String CREATE_TABLE_CLASS_INSTANCES = "CREATE TABLE " + TABLE_CLASS_INSTANCES + " (" +
            CLASS_INSTANCE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            CLASS_INSTANCE_COURSE_ID + " INTEGER NOT NULL," +
            "courseName TEXT," +
            CLASS_INSTANCE_DATE + " TEXT NOT NULL," +
            CLASS_INSTANCE_TEACHER + " TEXT NOT NULL," +
            CLASS_INSTANCE_COMMENTS + " TEXT," +
            CLASS_INSTANCE_FIRESTORE_ID + " TEXT," + // Đảm bảo trường này có trong bảng
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

    public boolean deleteCourseByFirestoreId(String firestoreId) {
        if (firestoreId == null || firestoreId.isEmpty()) {
            Log.e("DatabaseHelper", "Cannot delete course: Firestore ID is null or empty.");
            return false; // Prevent deletion attempt with invalid ID
        }

        SQLiteDatabase db = this.getWritableDatabase();

        // Bước 1: Xóa tất cả các lớp học liên quan trong SQLite
        db.delete(TABLE_CLASS_INSTANCES, CLASS_INSTANCE_COURSE_ID + " = ?", new String[]{firestoreId});

        // Bước 2: Xóa khóa học khỏi SQLite
        int rowsAffected = db.delete(TABLE_COURSES, "firestoreId = ?", new String[]{firestoreId});

        if (rowsAffected > 0) {
            Log.d("DatabaseHelper", "Course with Firestore ID " + firestoreId + " deleted successfully.");
            return true; // Deletion was successful
        } else {
            Log.e("DatabaseHelper", "No course found with Firestore ID: " + firestoreId);
            return false; // No course found to delete
        }
    }

    public boolean updateCourseByFirestoreId(String firestoreId, Course course) {
        if (firestoreId == null || firestoreId.isEmpty()) {
            Log.e("DatabaseHelper", "Không thể cập nhật khóa học: Firestore ID là null hoặc rỗng.");
            return false; // Ngăn chặn việc cập nhật với ID không hợp lệ
        }

        SQLiteDatabase db = this.getWritableDatabase();

        // Tạo đối tượng ContentValues để chứa thông tin khóa học được cập nhật
        ContentValues values = new ContentValues();
        values.put("courseName", course.getCourseName());
        values.put("dayOfWeek", course.getDayOfWeek());
        values.put("type", course.getType());
        values.put("time", course.getTime());
        values.put("capacity", course.getCapacity());
        values.put("duration", course.getDuration());
        values.put("price", course.getPrice());
        values.put("description", course.getDescription());

        // Cập nhật khóa học trong cơ sở dữ liệu
        int rowsAffected = db.update(TABLE_COURSES, values, "firestoreId = ?", new String[]{firestoreId});

        if (rowsAffected > 0) {
            Log.d("DatabaseHelper", "Khóa học với Firestore ID " + firestoreId + " đã cập nhật thành công.");
            return true; // Cập nhật thành công
        } else {
            Log.e("DatabaseHelper", "Không tìm thấy khóa học với Firestore ID: " + firestoreId);
            return false; // Không tìm thấy khóa học để cập nhật
        }
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


    public long addClassInstance(ClassInstance classInstance, String firestoreId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Ghi log kiểm tra giá trị firestoreId
        Log.d("DatabaseHelper", "Firestore ID: " + firestoreId);

        // Thiết lập các giá trị để lưu vào SQLite
        values.put(CLASS_INSTANCE_COURSE_ID, classInstance.getCourseId());
        values.put("courseName", classInstance.getCourseName()); // Thêm tên khóa học vào giá trị
        values.put(CLASS_INSTANCE_DATE, classInstance.getDate());
        values.put(CLASS_INSTANCE_TEACHER, classInstance.getTeacher());
        values.put(CLASS_INSTANCE_COMMENTS, classInstance.getComments());
        values.put(CLASS_INSTANCE_FIRESTORE_ID, firestoreId); // Lưu Firestore ID vào giá trị

        long id = db.insert(TABLE_CLASS_INSTANCES, null, values);

        // Kiểm tra và ghi log kết quả
        if (id == -1) {
            Log.e("DatabaseHelper", "Lỗi khi thêm instance của lớp học vào SQLite");
        } else {
            Log.d("DatabaseHelper", "Instance của lớp học đã được thêm vào SQLite với Firestore ID: " + firestoreId);
        }

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


    public boolean deleteClassInstance(String firestoreId) {
        SQLiteDatabase db = this.getWritableDatabase(); // Mở kết nối
        Log.d("DatabaseHelper", "Attempting to delete class instance with Firestore ID: " + firestoreId);

        try {
            // Xóa lớp học dựa trên firestoreId
            int rowsDeleted = db.delete(TABLE_CLASS_INSTANCES, "firestoreId = ?", new String[]{firestoreId});

            if (rowsDeleted > 0) {
                Log.d("DatabaseHelper", "Successfully deleted class instance with Firestore ID: " + firestoreId);
                return true; // Trả về true nếu xóa thành công
            } else {
                Log.e("DatabaseHelper", "Failed to delete class instance with Firestore ID: " + firestoreId);
                return false; // Trả về false nếu xóa không thành công
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error while trying to delete class instance", e);
            return false; // Trả về false nếu có lỗi
        } finally {
            db.close(); // Đóng kết nối tại đây sau khi hoàn tất tất cả thao tác
        }
    }

    public boolean updateClassInstance(String firestoreId, ClassInstance classInstance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(CLASS_INSTANCE_COURSE_ID, classInstance.getCourseId());
        contentValues.put(CLASS_INSTANCE_DATE, classInstance.getDate());
        contentValues.put(CLASS_INSTANCE_TEACHER, classInstance.getTeacher());
        contentValues.put(CLASS_INSTANCE_COMMENTS, classInstance.getComments());

        // Cập nhật cơ sở dữ liệu SQLite
        int rowsAffected = db.update(TABLE_CLASS_INSTANCES, contentValues, CLASS_INSTANCE_ID + " = ?",
                new String[]{String.valueOf(classInstance.getId())});

        // Cập nhật Firestore
        updateFirestore(firestoreId, classInstance); // Gọi với firestoreId

        return rowsAffected > 0;
    }

    private void updateFirestore(String firestoreId, ClassInstance classInstance) {
        FirebaseFirestore dbFirestore = FirebaseFirestore.getInstance();

        Log.d("Firestore", "Kiểm tra firestoreId: " + firestoreId);

        if (firestoreId == null) {
            Log.e("Firestore", "firestoreId không được phép null!");
            return; // Không cần hiển thị Toast ở đây nếu bạn chỉ đang kiểm tra log
        }

        // Cập nhật Firestore với firestoreId đã tồn tại
        dbFirestore.collection("class_instances")
                .document(firestoreId)
                .set(classInstance)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Cập nhật ClassInstance thành công cho ID: " + firestoreId);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Cập nhật ClassInstance thất bại cho ID: " + firestoreId, e);
                });
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

    public long addClassToCourse(ClassInstance classInstance, String firestoreId) {
        // Khởi tạo SQLite
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Thiết lập các giá trị để lưu vào SQLite
        values.put(CLASS_INSTANCE_COURSE_ID, classInstance.getCourseId());
        values.put("courseName", classInstance.getCourseName());
        values.put(CLASS_INSTANCE_DATE, classInstance.getDate());
        values.put(CLASS_INSTANCE_TEACHER, classInstance.getTeacher());
        values.put(CLASS_INSTANCE_COMMENTS, classInstance.getComments());
        values.put(CLASS_INSTANCE_FIRESTORE_ID, firestoreId); // Đồng bộ Firestore ID

        long result = -1; // Khởi tạo biến kết quả

        try {
            // Thực hiện thêm vào SQLite
            result = db.insert(TABLE_CLASS_INSTANCES, null, values);

            // Kiểm tra kết quả thêm vào SQLite
            if (result == -1) {
                Log.e("DatabaseHelper", "Lỗi khi thêm instance của lớp học vào SQLite");
            } else {
                // Ghi log chỉ khi Firestore ID không phải là null
                if (firestoreId != null) {
                    Log.d("DatabaseHelper", "Instance của lớp học đã được thêm vào SQLite với Firestore ID: " + firestoreId);
                }
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Lỗi ngoại lệ khi thêm instance của lớp học: ", e);
        } finally {
            // Đóng kết nối cơ sở dữ liệu
            db.close();
        }

        return result; // Trả về kết quả thêm vào
    }

    public void updateFirestoreId(long id, String firestoreId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(CLASS_INSTANCE_FIRESTORE_ID, firestoreId);

        try {
            db.update(TABLE_CLASS_INSTANCES, values, "id = ?", new String[]{String.valueOf(id)});
            Log.d("DatabaseHelper", "Cập nhật Firestore ID thành công trong SQLite: " + firestoreId);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Lỗi khi cập nhật Firestore ID", e);
        } finally {
            db.close();
        }
    }


    public Course getCourseById(int courseId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_COURSES, null, "id = ?", new String[]{String.valueOf(courseId)}, null, null, null);

        // Log ID khóa học tìm kiếm
        Log.d("Database", "Searching for course with ID: " + courseId);

        // Thêm log để kiểm tra xem cursor có kết quả hay không
        if (cursor != null && cursor.moveToFirst()) {
            // Sử dụng getColumnIndex với tên cột hợp lệ
            int idIndex = cursor.getColumnIndex("id");
            int firestoreIdIndex = cursor.getColumnIndex("firestoreId");
            int nameIndex = cursor.getColumnIndex("courseName"); // Đảm bảo tên cột đúng
            int dayOfWeekIndex = cursor.getColumnIndex("dayOfWeek");
            int timeIndex = cursor.getColumnIndex("time");
            int capacityIndex = cursor.getColumnIndex("capacity");
            int durationIndex = cursor.getColumnIndex("duration");
            int priceIndex = cursor.getColumnIndex("price");
            int typeIndex = cursor.getColumnIndex("type");
            int descriptionIndex = cursor.getColumnIndex("description");

            // Kiểm tra xem cột có hợp lệ không trước khi lấy dữ liệu
            if (idIndex != -1 && nameIndex != -1 && dayOfWeekIndex != -1) {
                int id = cursor.getInt(idIndex);
                String firestoreId = cursor.getString(firestoreIdIndex);
                Log.d("Database", "Firestore ID from DB: " + firestoreId);
                String name = cursor.getString(nameIndex);
                String dayOfWeek = cursor.getString(dayOfWeekIndex);
                String time = cursor.getString(timeIndex);
                int capacity = cursor.getInt(capacityIndex);
                String duration = cursor.getString(durationIndex);
                double price = cursor.getDouble(priceIndex);
                String type = cursor.getString(typeIndex);
                String description = cursor.getString(descriptionIndex);

                // Log thông tin khóa học đã tìm thấy
                Log.d("Database", "Course found: ID=" + id + ", Firestore ID=" + firestoreId + ", Name=" + name + ", Day=" + dayOfWeek + ", Time=" + time);
                return new Course(id, firestoreId, name, dayOfWeek, time, capacity, duration, price, type, description);
            } else {
                Log.e("Database", "Invalid column index");
            }
        } else {
            Log.e("Database", "Cursor is empty or null for course ID: " + courseId);
        }

        if (cursor != null) {
            cursor.close(); // Đóng cursor
        }
        Log.d("Database", "No course found with ID: " + courseId);
        return null; // Không tìm thấy khóa học
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
                        cursor.getString(cursor.getColumnIndex("comments")),     // Lấy comments từ cursor
                        cursor.getString(cursor.getColumnIndex("firestoreId"))   // Lấy firestoreId từ cursor
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
                    String firestoreId = cursor.getString(cursor.getColumnIndex("firestoreId")); // Đảm bảo lấy firestoreId
                    String name = cursor.getString(cursor.getColumnIndex("courseName"));
                    String dayOfWeek = cursor.getString(cursor.getColumnIndex("dayOfWeek")); // Thêm dayOfWeek
                    String time = cursor.getString(cursor.getColumnIndex("time")); // Thêm time
                    int capacity = cursor.getInt(cursor.getColumnIndex("capacity")); // Thêm capacity
                    String duration = cursor.getString(cursor.getColumnIndex("duration")); // Thêm duration
                    double price = cursor.getDouble(cursor.getColumnIndex("price"));
                    String type = cursor.getString(cursor.getColumnIndex("type")); // Thêm type
                    String description = cursor.getString(cursor.getColumnIndex("description")); // Thêm description

                    Course course = new Course(id, firestoreId, name, dayOfWeek, time, capacity, duration, price, type, description);
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



    public long updateFirestoreIdInSQLite(int id, String firestoreId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(CLASS_INSTANCE_FIRESTORE_ID, firestoreId);

        Log.d("DatabaseHelper", "Cập nhật Firestore ID: " + firestoreId + " cho ID lớp học: " + id);

        long result = db.update(TABLE_CLASS_INSTANCES, values, "id = ?", new String[]{String.valueOf(id)});

        if (result == -1) {
            Log.e("DatabaseHelper", "Lỗi khi cập nhật Firestore ID vào SQLite");
        } else {
            Log.d("DatabaseHelper", "Cập nhật Firestore ID thành công cho ID lớp học: " + id);
        }

        db.close();
        return result;
    }

    public ClassInstance getClassInstanceById(int classId) {
        SQLiteDatabase db = this.getReadableDatabase();
        ClassInstance classInstance = null; // Khởi tạo biến để lưu thông tin lớp học

        Cursor cursor = null;
        try {
            // Thực hiện truy vấn
            String query = "SELECT * FROM class_instances WHERE id = ?";
            cursor = db.rawQuery(query, new String[]{String.valueOf(classId)});

            // Kiểm tra xem có kết quả hay không
            if (cursor != null && cursor.moveToFirst()) {
                // Lấy dữ liệu từ cursor và tạo đối tượng ClassInstance
                classInstance = new ClassInstance();
                classInstance.setId(cursor.getInt(cursor.getColumnIndex("id")));
                classInstance.setDate(cursor.getString(cursor.getColumnIndex("date")));
                classInstance.setTeacher(cursor.getString(cursor.getColumnIndex("teacher")));
                classInstance.setComments(cursor.getString(cursor.getColumnIndex("comments")));

                // Lấy firestoreId
                int firestoreIdIndex = cursor.getColumnIndex("firestoreId");
                if (firestoreIdIndex != -1) { // Kiểm tra nếu firestoreId tồn tại
                    classInstance.setFirestoreId(cursor.getString(firestoreIdIndex)); // Thêm trường firestoreId
                    Log.d("DatabaseHelper", "Firestore ID: " + classInstance.getFirestoreId()); // Log firestoreId
                } else {
                    Log.e("DatabaseHelper", "Không tìm thấy cột firestoreId trong kết quả truy vấn.");
                }

                // Log thông tin ID và các trường
                Log.d("DatabaseHelper", "ID lớp học: " + classInstance.getId());
                Log.d("DatabaseHelper", "Ngày: " + classInstance.getDate());
                Log.d("DatabaseHelper", "Giáo viên: " + classInstance.getTeacher());
                Log.d("DatabaseHelper", "Nhận xét: " + classInstance.getComments());
            } else {
                Log.e("DatabaseHelper", "Không tìm thấy lớp học với ID: " + classId);
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Lỗi khi cố gắng lấy lớp học theo ID", e);
        } finally {
            // Đảm bảo đóng cursor
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return classInstance; // Trả về đối tượng ClassInstance hoặc null nếu không tìm thấy
    }

    public List<Course> getUnsyncedCourses() {
        List<Course> unsyncedCourses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("courses", null, "firestore_id IS NULL", null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                // Giả sử Course có các thuộc tính id, name, v.v...
                int id = cursor.getInt(cursor.getColumnIndex("id"));
                String name = cursor.getString(cursor.getColumnIndex("name"));
                // Lấy các trường dữ liệu khác của Course
                Course course = new Course(id, name /* Thêm các thuộc tính khác */);
                unsyncedCourses.add(course);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return unsyncedCourses;
    }

    public void updateCourseFirestoreId(int courseId, String firestoreId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("firestoreId", firestoreId);
        db.update("courses", values, "id = ?", new String[]{String.valueOf(courseId)});
        db.close();
    }






    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Chỉ thực hiện nâng cấp nếu phiên bản cũ nhỏ hơn phiên bản mới
        if (oldVersion < newVersion) {
            // Xóa bảng courses nếu tồn tại
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_COURSES);
            // Xóa bảng class_instances nếu cần
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLASS_INSTANCES);
            // Tạo lại bảng courses
            onCreate(db);
        }
    }




}
