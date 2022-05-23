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
class ByteArrayConverter :
    com.raizlabs.android.dbflow.converter.TypeConverter<String, ByteArray>() {
    override fun getDBValue(model: ByteArray?): String {
        if (model == null) return ""
        return Gson().toJson(model)
    }

    override fun getModelValue(data: String?): ByteArray {
        if (TextUtils.isEmpty(data))
            return byteArrayOf()
        return Gson().fromJson(data, object : TypeToken<ByteArray>() {}.type)
    }
}