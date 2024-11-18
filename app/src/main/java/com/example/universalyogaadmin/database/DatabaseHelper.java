package com.example.universalyogaadmin.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.universalyogaadmin.model.ClassInstance;
import com.example.universalyogaadmin.model.Course;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";

    private static final String DATABASE_NAME = "yoga_classes.db";
    private static final int DATABASE_VERSION = 5;
    private static final String TABLE_COURSES = "courses";
    private static final String TABLE_CLASS_INSTANCES = "class_instances";

    private static final String COURSE_ID = "id";
    private static final String CLASS_INSTANCE_ID = "id";
    private static final String CLASS_INSTANCE_COURSE_ID = "course_id";
    private static final String CLASS_INSTANCE_DATE = "date";
    private static final String CLASS_INSTANCE_TEACHER = "teacher";
    private static final String CLASS_INSTANCE_COMMENTS = "comments";
    private static final String CLASS_INSTANCE_FIRESTORE_ID = "firestoreId";

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
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Đặt các giá trị vào ContentValues
        values.put("courseName", course.getCourseName());
        values.put("dayOfWeek", course.getDayOfWeek());
        values.put("time", course.getTime());
        values.put("capacity", course.getCapacity());
        values.put("duration", course.getDuration());
        values.put("price", course.getPrice());
        values.put("type", course.getType());
        values.put("description", course.getDescription());
        values.put("firestoreId", firestoreId);

        long newRowId = -1;

        try {
            newRowId = db.insert(TABLE_COURSES, null, values);

            Log.d("Database", "Added Course: " + course.getCourseName() + ", ID: " + newRowId);
        } catch (Exception e) {
            Log.e("Database", "Error adding course: " + e.getMessage());
        } finally {
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

    public boolean deleteCourseByFirestoreId(String firestoreId) {
        if (firestoreId == null || firestoreId.isEmpty()) {
            Log.e("DatabaseHelper", "Cannot delete course: Firestore ID is null or empty.");
            return false; // Prevent deletion attempt with invalid ID
        }

        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(TABLE_CLASS_INSTANCES, CLASS_INSTANCE_COURSE_ID + " = ?", new String[]{firestoreId});

        int rowsAffected = db.delete(TABLE_COURSES, "firestoreId = ?", new String[]{firestoreId});

        if (rowsAffected > 0) {
            Log.d("DatabaseHelper", "Course with Firestore ID " + firestoreId + " deleted successfully.");
            return true; // Deletion was successful
        } else {
            Log.e("DatabaseHelper", "No course found with Firestore ID: " + firestoreId);
            return false;
        }
    }

    public boolean updateCourseByFirestoreId(String firestoreId, Course course) {
        if (firestoreId == null || firestoreId.isEmpty()) {
            Log.e("DatabaseHelper", "Could not update course: Firestore ID is null or empty.");
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("courseName", course.getCourseName());
        values.put("dayOfWeek", course.getDayOfWeek());
        values.put("type", course.getType());
        values.put("time", course.getTime());
        values.put("capacity", course.getCapacity());
        values.put("duration", course.getDuration());
        values.put("price", course.getPrice());
        values.put("description", course.getDescription());
        // Do not update the 'details' field

        int rowsAffected = db.update(TABLE_COURSES, values, "firestoreId = ?", new String[]{firestoreId});

        if (rowsAffected > 0) {
            Log.d("DatabaseHelper", "The course with Firestore ID " + firestoreId + " was updated successfully.");
            return true;
        } else {
            Log.e("DatabaseHelper", "Course not found with Firestore ID: " + firestoreId);
            return false;
        }
    }

    public long addClassInstance(ClassInstance classInstance, String firestoreId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        Log.d("DatabaseHelper", "Firestore ID: " + firestoreId);

        values.put(CLASS_INSTANCE_COURSE_ID, classInstance.getCourseId());
        values.put("courseName", classInstance.getCourseName());
        values.put(CLASS_INSTANCE_DATE, classInstance.getDate());
        values.put(CLASS_INSTANCE_TEACHER, classInstance.getTeacher());
        values.put(CLASS_INSTANCE_COMMENTS, classInstance.getComments());
        values.put(CLASS_INSTANCE_FIRESTORE_ID, firestoreId);

        long id = db.insert(TABLE_CLASS_INSTANCES, null, values);

        if (id == -1) {
            Log.e("DatabaseHelper", "Error adding class instance to SQLite");
        } else {
            Log.d("DatabaseHelper", "Instance of class has been added to SQLite with Firestore ID: " + firestoreId);
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
            String query = "SELECT ci.*, c.courseName AS courseName " +
                    "FROM " + TABLE_CLASS_INSTANCES + " ci " +
                    "JOIN " + TABLE_COURSES + " c ON ci." + CLASS_INSTANCE_COURSE_ID + " = c." + COURSE_ID;

            cursor = db.rawQuery(query, null);

            if (cursor.moveToFirst()) {
                do {
                    ClassInstance classInstance = new ClassInstance();
                    classInstance.setId(cursor.getInt(cursor.getColumnIndexOrThrow(CLASS_INSTANCE_ID)));
                    classInstance.setCourseId(cursor.getInt(cursor.getColumnIndexOrThrow(CLASS_INSTANCE_COURSE_ID)));
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
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d("DatabaseHelper", "Attempting to delete class instance with Firestore ID: " + firestoreId);

        try {
            int rowsDeleted = db.delete(TABLE_CLASS_INSTANCES, "firestoreId = ?", new String[]{firestoreId});

            if (rowsDeleted > 0) {
                Log.d("DatabaseHelper", "Successfully deleted class instance with Firestore ID: " + firestoreId);
                return true;
            } else {
                Log.e("DatabaseHelper", "Failed to delete class instance with Firestore ID: " + firestoreId);
                return false;
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error while trying to delete class instance", e);
            return false;
        } finally {
            db.close();
        }
    }

    public boolean updateClassInstance(String firestoreId, ClassInstance classInstance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(CLASS_INSTANCE_COURSE_ID, classInstance.getCourseId());
        contentValues.put(CLASS_INSTANCE_DATE, classInstance.getDate());
        contentValues.put(CLASS_INSTANCE_TEACHER, classInstance.getTeacher());
        contentValues.put(CLASS_INSTANCE_COMMENTS, classInstance.getComments());

        int rowsAffected = db.update(TABLE_CLASS_INSTANCES, contentValues, CLASS_INSTANCE_ID + " = ?",
                new String[]{String.valueOf(classInstance.getId())});

        updateFirestore(firestoreId, classInstance);

        return rowsAffected > 0;
    }

    private void updateFirestore(String firestoreId, ClassInstance classInstance) {
        FirebaseFirestore dbFirestore = FirebaseFirestore.getInstance();

        Log.d("Firestore", "Checking firestoreId: " + firestoreId);

        if (firestoreId == null) {
            Log.e("Firestore", "firestoreId cannot be null!");
            return;
        }

        dbFirestore.collection("classInstances")
                .document(firestoreId)
                .set(classInstance)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Successfully updated ClassInstance for ID: " + firestoreId);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to update ClassInstance for ID: " + firestoreId, e);
                });
    }

    public long addClassToCourse(ClassInstance classInstance, String firestoreId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(CLASS_INSTANCE_COURSE_ID, classInstance.getCourseId());
        values.put("courseName", classInstance.getCourseName());
        values.put(CLASS_INSTANCE_DATE, classInstance.getDate());
        values.put(CLASS_INSTANCE_TEACHER, classInstance.getTeacher());
        values.put(CLASS_INSTANCE_COMMENTS, classInstance.getComments());
        values.put(CLASS_INSTANCE_FIRESTORE_ID, firestoreId);

        long result = -1;

        try {
            result = db.insert(TABLE_CLASS_INSTANCES, null, values);

            if (result == -1) {
                Log.e("DatabaseHelper", "Error adding class instance to SQLite");
            } else {
                if (firestoreId != null) {
                    Log.d("DatabaseHelper", "Instance of class has been added to SQLite with Firestore ID: " + firestoreId);
                }
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Exception error while adding class instance: ", e);
        } finally {
            db.close();
        }

        return result;
    }

    public void updateFirestoreId(long id, String firestoreId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(CLASS_INSTANCE_FIRESTORE_ID, firestoreId);

        try {
            db.update(TABLE_CLASS_INSTANCES, values, "id = ?", new String[]{String.valueOf(id)});
            Log.d("DatabaseHelper", "Updated Firestore ID successfully in SQLite: " + firestoreId);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error updating Firestore ID", e);
        } finally {
            db.close();
        }
    }

    public Course getCourseById(int courseId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_COURSES, null, "id = ?", new String[]{String.valueOf(courseId)}, null, null, null);

        Log.d("Database", "Searching for course with ID: " + courseId);

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

                Log.d("Database", "Course found: ID=" + id + ", Firestore ID=" + firestoreId + ", Name=" + name + ", Day=" + dayOfWeek + ", Time=" + time);
                return new Course(id, firestoreId, name, dayOfWeek, time, capacity, duration, price, type, description);
            } else {
                Log.e("Database", "Invalid column index");
            }
        } else {
            Log.e("Database", "Cursor is empty or null for course ID: " + courseId);
        }

        if (cursor != null) {
            cursor.close();
        }
        Log.d("Database", "No course found with ID: " + courseId);
        return null;
    }

    public List<Course> searchCoursesByName(String courseName) {
        List<Course> courses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM courses WHERE courseName LIKE ?";
        Cursor cursor = null;

        try {
            Log.i("DatabaseHelper", "Executing query: " + query + " with parameter: " + courseName);
            cursor = db.rawQuery(query, new String[]{"%" + courseName + "%"});

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndex("id"));
                    String firestoreId = cursor.getString(cursor.getColumnIndex("firestoreId"));
                    String name = cursor.getString(cursor.getColumnIndex("courseName"));
                    String dayOfWeek = cursor.getString(cursor.getColumnIndex("dayOfWeek"));
                    String time = cursor.getString(cursor.getColumnIndex("time"));
                    int capacity = cursor.getInt(cursor.getColumnIndex("capacity"));
                    String duration = cursor.getString(cursor.getColumnIndex("duration"));
                    double price = cursor.getDouble(cursor.getColumnIndex("price"));
                    String type = cursor.getString(cursor.getColumnIndex("type"));
                    String description = cursor.getString(cursor.getColumnIndex("description"));

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

        Log.d("DatabaseHelper", "Update Firestore ID: " + firestoreId + " for class ID: " + id);

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
        ClassInstance classInstance = null;

        Cursor cursor = null;
        try {
            String query = "SELECT * FROM class_instances WHERE id = ?";
            cursor = db.rawQuery(query, new String[]{String.valueOf(classId)});

            if (cursor != null && cursor.moveToFirst()) {
                classInstance = new ClassInstance();
                classInstance.setId(cursor.getInt(cursor.getColumnIndex("id")));
                classInstance.setCourseId(cursor.getInt(cursor.getColumnIndexOrThrow("course_id")));
                classInstance.setDate(cursor.getString(cursor.getColumnIndex("date")));
                classInstance.setTeacher(cursor.getString(cursor.getColumnIndex("teacher")));
                classInstance.setComments(cursor.getString(cursor.getColumnIndex("comments")));

                int firestoreIdIndex = cursor.getColumnIndex("firestoreId");
                if (firestoreIdIndex != -1) {
                    classInstance.setFirestoreId(cursor.getString(firestoreIdIndex));
                    Log.d("DatabaseHelper", "Firestore ID: " + classInstance.getFirestoreId());
                } else {
                    Log.e("DatabaseHelper", "The firestoreId column was not found in the query result.");
                }

                Log.d("DatabaseHelper", "Class ID: " + classInstance.getId());
                Log.d("DatabaseHelper", "Course ID: " + classInstance.getCourseId());
                Log.d("DatabaseHelper", "Date: " + classInstance.getDate());
                Log.d("DatabaseHelper", "Teacher: " + classInstance.getTeacher());
                Log.d("DatabaseHelper", "Comments: " + classInstance.getComments());
            } else {
                Log.e("DatabaseHelper", "Could not find class with ID: " + classId);
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error while trying to get class by ID", e);
        } finally {
            // Đảm bảo đóng cursor
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return classInstance;
    }

    public List<Course> getUnsyncedCourses() {
        List<Course> unsyncedCourses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("courses", null, "firestore_id IS NULL", null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex("id"));
                String name = cursor.getString(cursor.getColumnIndex("name"));
                Course course = new Course(id, name);
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
        if (oldVersion < newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_COURSES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLASS_INSTANCES);
            onCreate(db);
        }
    }
}
