package io.zjw.testblelib.db

import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.raizlabs.android.dbflow.annotation.TypeConverter

/**
 * Copyright © 2019 All Rights Reserved By MegaHealth.
 * Author: wangkelei
 * Date: 2022/5/22 3:46 下午
 * Description:
 */
@TypeConverter
class FloatArrayConverter :
    com.raizlabs.android.dbflow.converter.TypeConverter<String, FloatArray>() {
    override fun getDBValue(model: FloatArray?): String {
        if (model == null) return ""
        return Gson().toJson(model)
    }

    override fun getModelValue(data: String?): FloatArray {
        if (TextUtils.isEmpty(data))
            return floatArrayOf()
        return Gson().fromJson(data, object : TypeToken<FloatArray>() {}.type)
    }
}