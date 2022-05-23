package io.zjw.testblelib

import android.app.Application
import com.raizlabs.android.dbflow.config.DatabaseConfig
import com.raizlabs.android.dbflow.config.FlowConfig
import com.raizlabs.android.dbflow.config.FlowManager
import io.zjw.testblelib.db.AppDatabase

/**
 * Copyright © 2019 All Rights Reserved By MegaHealth.
 * Author: wangkelei
 * Date: 2022/5/22 5:06 下午
 * Description:
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        FlowManager.init(this)
    }
}