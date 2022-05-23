package io.zjw.testblelib.db

import com.raizlabs.android.dbflow.sql.language.SQLite

class DBInstance {
    fun addDataEntity(dataEntity: DataEntity): Boolean {
        return dataEntity.save()
    }

    fun getLatestReport(): List<DataEntity> {
        return SQLite
            .select(
                DataEntity_Table._id,
                DataEntity_Table.dataType,
                DataEntity_Table.DBP,
                DataEntity_Table.SBP
            )
            .from(DataEntity::class.java)
            .limit(100)
            .orderBy(DataEntity_Table._id, false)
            .queryList()
    }

    fun loadReportById(_id: Int): DataEntity? {
        return SQLite.select()
            .from(DataEntity::class.java)
            .where(DataEntity_Table._id.eq(_id))
            .querySingle()
    }

    companion object {
        val INSTANCE: DBInstance by lazy {
            DBInstance()
        }
    }
}