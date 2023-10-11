package io.zjw.testblelib.utils

import android.content.res.Resources
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit


class EcgChatManager {
    private val TAG = "EcgChatManager"

    private var chart: LineChart? = null
    private var xAxis: XAxis? = null
    private var yAxis: YAxis? = null
    private var showCount = 400
    private var disposable: Disposable? = null

    private val dataSet = LineDataSet(arrayListOf(), "")
    private var dataList = arrayListOf<Float>()

    fun bindView(chart: LineChart) {
        this.chart = chart
        this.chart?.let {
            it.setTouchEnabled(false)
            it.description.isEnabled = false
            it.legend.isEnabled = false
            it.axisRight.isEnabled = false
            it.setViewPortOffsets(
                    0F,
                    dp2px(10F).toFloat(),
                    0F,
                    dp2px(10F).toFloat()
            )

            xAxis = it.xAxis
            xAxis?.position = XAxis.XAxisPosition.BOTTOM
            xAxis?.setDrawAxisLine(false)
            xAxis?.setDrawLabels(false)
            xAxis?.setDrawGridLines(false)
            xAxis?.axisMinimum = 0F
            xAxis?.axisMaximum = (showCount - 1).toFloat()

            yAxis = it.axisLeft
            yAxis?.setDrawAxisLine(false)
            yAxis?.setDrawLabels(false)
            yAxis?.setDrawGridLines(false)

            dataSet.color = Color.RED
            dataSet.lineWidth = 1F
            dataSet.setDrawCircles(false)
            dataSet.setDrawValues(false)

            it.isAutoScaleMinMaxEnabled = true
            it.data = LineData(dataSet)
        }
    }

    fun addDataToList(dataArray: FloatArray?) {
        if (dataArray == null) return
        for (item in dataArray) {
            dataList.add(item)
        }
        if (chart!!.data!!.getDataSetByIndex(0) == null)
            chart?.data?.addDataSet(dataSet)
        if (disposable == null)
            startChartWork()
    }

    private fun startChartWork() {
        stopChartWork()
        if (dataList.isEmpty()) return
        disposable = Observable
            .interval(0, 10, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it >= dataList.size) {
                    println("$TAG index is greater than size")
                    return@subscribe
                }
                println("$TAG startChartWork() $it")
                try {
                    chart?.data?.addEntry(
                            Entry(
                                    it.toFloat(),
                                    dataList[it.toInt()]
                            ),
                            0
                    )
                    println("$TAG startChartWork() add data:${dataList[it.toInt()]}")
                    calculateAXisMaxMinByIndex(it)
                    chart?.notifyDataSetChanged()
                    chart?.invalidate()
                } catch (e: ArrayIndexOutOfBoundsException) {
                    Log.e("EcgChatManager", "$e")
                }
            }, {
                println("--->$it")
            })
    }

    fun stopChartWork() {
        println("$TAG stopChartWork()")
        disposable?.let {
            if (!it.isDisposed)
                it.dispose()
            disposable = null
        }

    }

    fun resetData() {
        dataList.clear()
        xAxis?.axisMinimum = 0F
        xAxis?.axisMaximum = (showCount - 1).toFloat()
        yAxis?.resetAxisMaximum()
        yAxis?.resetAxisMinimum()
        dataSet.clear()
        chart?.clearValues()
    }

    private fun calculateAXisMaxMinByIndex(index: Long) {
        if (index >= showCount) {
            xAxis?.axisMinimum = (index.toFloat() - showCount + 1)
            xAxis?.axisMaximum = index.toFloat()
        }
    }

    private fun dp2px(dpVal: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal, Resources.getSystem().displayMetrics).toInt()
    }

    companion object {
        val instance: EcgChatManager by lazy {
            EcgChatManager()
        }
    }
}