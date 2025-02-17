package io.zjw.testblelib.reports

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import io.zjw.testblelib.databinding.ActivityReportDetailBpBinding
import io.zjw.testblelib.db.DBInstance
import java.util.*

/**
 * Copyright © 2019 All Rights Reserved By MegaHealth.
 * Author: wangkelei
 * Date: 2022/5/23 11:01 上午
 * Description:
 */
class ReportDetailBpActivity : AppCompatActivity() {
    private lateinit var yAxis: YAxis
    private lateinit var xAxis: XAxis

    private lateinit var binding: ActivityReportDetailBpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportDetailBpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        val _id = intent.getIntExtra("_id", -1)
        Log.i("ReportDetailBpActivity", "_id:--->$_id")
        val entity = DBInstance.INSTANCE.loadReportById(_id)
        binding.bpChart.apply {
            setTouchEnabled(false)
            axisRight.setDrawLabels(false)
            axisRight.setDrawGridLines(false)
            legend.isEnabled = false
            description.isEnabled = false
        }
        yAxis = binding.bpChart.axisLeft
        xAxis = binding.bpChart.xAxis
        yAxis.apply {
            setDrawLabels(false)
            setLabelCount(7, true)
        }

        xAxis.apply {
            setDrawLabels(false)
            position = XAxis.XAxisPosition.BOTH_SIDED
        }

        val lineDataSet = LineDataSet(arrayListOf(), "")
        lineDataSet.setDrawCircles(false)
        lineDataSet.setDrawValues(false)
        lineDataSet.color = Color.RED
        entity?.chEcg?.let {
            /**
             * Recommend to take out the last 400 points(4 seconds data) in array to draw ECG chart.
             */
            val dest = FloatArray(400)
            System.arraycopy(it, it.size - 400, dest, 0, 400)
            for (index in dest.indices) {
                lineDataSet.addEntry(Entry(index.toFloat(), dest[index]))
            }
        }
        entity?.let {
            binding.tvSbpValue.text = String.format("%.1f", it.SBP)
            binding.tvDbpValue.text = String.format("%.1f", it.DBP)
            binding.tvPrValue.text = "${it.pr}"
        }
        val lineData = LineData(lineDataSet)
        binding.bpChart.data = lineData
    }
}