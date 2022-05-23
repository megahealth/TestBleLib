package io.zjw.testblelib.reports

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.zjw.testblelib.R
import io.zjw.testblelib.db.DBInstance
import io.zjw.testblelib.db.DataEntity
import kotlinx.android.synthetic.main.activity_report_list.*

/**
 * Copyright © 2019 All Rights Reserved By MegaHealth.
 * Author: wangkelei
 * Date: 2022/5/22 10:35 下午
 * Description:
 */
class ReportListActivity : AppCompatActivity() {
    private lateinit var adapter: ReportAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_list)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        adapter = ReportAdapter()
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
        val d = DBInstance.INSTANCE.getLatestReport()
        if (d.isNullOrEmpty()) {
            Toast.makeText(this, "No data", Toast.LENGTH_SHORT).show()
        } else {
            adapter.setDataList(d)
        }
    }

    internal inner class ReportAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val data = arrayListOf<DataEntity>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return BPItemHolder(
                LayoutInflater.from(this@ReportListActivity)
                    .inflate(R.layout.view_item_bp, parent, false)
            )
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is BPItemHolder) {
                holder.bindView(data[position])
            }
        }

        override fun getItemCount(): Int {
            return data.size
        }

        fun setDataList(dataList: List<DataEntity>) {
            data.clear()
            data.addAll(dataList)
        }
    }

    internal inner class BPItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var tv_sbp_value: TextView
        private var tv_dbp_value: TextView

        init {
            tv_sbp_value = itemView.findViewById(R.id.tv_sbp_value)
            tv_dbp_value = itemView.findViewById(R.id.tv_dbp_value)
        }

        fun bindView(dataEntity: DataEntity) {
            tv_sbp_value.text = String.format("%.1f", dataEntity.SBP)
            tv_dbp_value.text = String.format("%.1f", dataEntity.DBP)
            itemView.setOnClickListener {
                val intent = Intent(this@ReportListActivity, ReportDetailBpActivity::class.java)
                intent.putExtra("_id", dataEntity._id)
                startActivity(intent)
            }
        }
    }
}