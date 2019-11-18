package io.zjw.testblelib;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by mega on 2018/3/22.
 */

public class UtilsSharedPreference {
    private static final String FILE_NAME = "MegaBle";
    public static final String KEY_TOKEN = "token";

    public static String get(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        return sp.getString(key, null);
    }

    public static void put(Context context, String key, String value) {
        SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static void remove(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(key);
        editor.apply();
    }
}
