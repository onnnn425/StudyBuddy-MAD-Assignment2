package com.studybuddy.app.database;

import androidx.room.TypeConverter; // 1. Ensure this import is correct
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.studybuddy.app.models.Subtask;
import java.lang.reflect.Type;
import java.util.List;

public class Converters {

    @TypeConverter //this must be present above every converter method
    public static List<Subtask> fromString(String value) {
        Type listType = new TypeToken<List<Subtask>>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter //this must be present here too
    public static String fromList(List<Subtask> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }
}