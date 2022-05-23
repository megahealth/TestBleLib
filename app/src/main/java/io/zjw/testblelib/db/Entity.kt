package io.zjw.testblelib.db

import com.raizlabs.android.dbflow.annotation.Column
import com.raizlabs.android.dbflow.annotation.PrimaryKey
import com.raizlabs.android.dbflow.annotation.Table
import com.raizlabs.android.dbflow.structure.BaseModel

@Table(database = AppDatabase::class)
class DataEntity : BaseModel() {
    @PrimaryKey(autoincrement = true)
    var _id: Int = 0

    @Column(typeConverter = ByteArrayConverter::class)
    var data: ByteArray? = null

    @Column
    var date: Long = -1

    @Column
    var dataType = 0

    @Column
    var stopType = 0

    // result of parse bp data(Diastolic Blood Pressure)
    @Column
    var DBP = 0.0

    // result of parse bp data(Systolic Blood Pressure)
    @Column
    var SBP = 0.0

    @Column
    var configSBP = -1F

    @Column
    var configDBP = -1F

    @Column
    var pr = -1

    @Column(typeConverter = FloatArrayConverter::class)
    var chEcg: FloatArray? = null

    override fun toString(): String {
        return "DataEntity(_id=$_id, data.len=${data?.size}, date=$date, dataType=$dataType, stopType=$stopType, configSBP=$configSBP, configDBP=$configDBP, SBP=$SBP, DBP=$DBP, pr=$pr, chEcg.len=${chEcg?.size})"
    }
}