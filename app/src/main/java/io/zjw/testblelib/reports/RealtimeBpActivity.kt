package io.zjw.testblelib.reports

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import io.mega.megableparse.ParsedBPBean
import io.zjw.testblelib.R
import io.zjw.testblelib.bean.BpDataEvent
import io.zjw.testblelib.utils.EcgChatManager
import kotlinx.android.synthetic.main.activity_realtime_bp.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.SimpleDateFormat
import java.util.*


class RealtimeBpActivity : AppCompatActivity() {


    private var parsedBPBean: ParsedBPBean? = null
    private var isParsing = false

    private val handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                0x01 -> {
                    tv_start_time.text = String.format(
                            getString(R.string.bp_test_start_time),
                            formatTimestampToEnHm(System.currentTimeMillis())
                    )
                    tv_start_time.visibility = View.VISIBLE
                    ll_prepare.visibility = View.GONE
                    tv_finish.visibility = View.GONE
                    ll_res.visibility = View.GONE
                    ll_live_data.visibility = View.VISIBLE
                    chart_view.visibility = View.VISIBLE
                    tv_duration.text = "00:00"
                    tv_duration.visibility = View.VISIBLE
                    tv_test_tip.visibility = View.VISIBLE
                }
                0x02 -> {
                    ll_live_data.visibility = View.GONE
                    tv_duration.visibility = View.GONE
                    tv_test_tip.visibility = View.GONE
                    tv_finish.visibility = View.VISIBLE
                    ll_res.visibility = View.VISIBLE
                    if (parsedBPBean != null && parsedBPBean!!.flag == 1) {
                        tv_sbp_value.text =
                                String.format(Locale.getDefault(), "%.1f", parsedBPBean!!.SBP)
                        tv_dbp_value.text =
                                String.format(Locale.getDefault(), "%.1f", parsedBPBean!!.DBP)
                        tv_res_pr_value.text = "${parsedBPBean?.pr}"
                    } else {
                        tv_sbp_value.text = "--"
                        tv_dbp_value.text = "--"
                        tv_res_pr_value.text = "--"
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_realtime_bp)
        initViews()
        setListener()
        startTest()
    }

    private fun initViews() {
        if (!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this)
        }
        EcgChatManager.instance.bindView(chart_view)
    }

    private fun setListener() {
        tv_next.setOnClickListener {
            finish()
        }
    }

    fun formatTimestampToEnHm(timestamps: Long): String? {
        return SimpleDateFormat("HH:mm").format(Date(timestamps))
    }

    private fun startTest() {
        tv_sbp_value.text = ""
        tv_dbp_value.text = ""
        tv_pr_value.text = ""
        tv_duration.text = ""
        tv_start_time.visibility = View.INVISIBLE
        tv_finish.visibility = View.GONE
        chart_view.visibility = View.GONE
        ll_res.visibility = View.GONE
        ll_live_data.visibility = View.GONE
        tv_duration.visibility = View.GONE
        tv_test_tip.visibility = View.GONE
        ll_prepare.visibility = View.VISIBLE
        EcgChatManager.instance.resetData()
        handler.sendEmptyMessageDelayed(0x01, 10000)
        isParsing = true
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBpDataEventReceived(bpDataEvent: BpDataEvent){
        Log.i("RealtimeBpActivity", bpDataEvent.parsedBPBean.toString())
        parsedBPBean = bpDataEvent.parsedBPBean
        tv_duration.text = formatDurationToMmSs(bpDataEvent.duration.toLong())
        if (chart_view.visibility == View.VISIBLE){
            EcgChatManager.instance.addDataToList(parsedBPBean!!.chEcg)
        }
        if(parsedBPBean!!.flag == 1){
            EcgChatManager.instance.stopChartWork()
            handler.sendEmptyMessage(0x02)
        }
    }

    private fun formatDurationToMmSs(duration: Long): String {
        val minute = duration % 3600 / 60
        val second = duration % 60
        return String.format("%02d:%02d", minute, second)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return false
    }

    override fun onDestroy() {
        if (EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this)
        }
        handler.removeCallbacksAndMessages(null)
        EcgChatManager.instance.stopChartWork()
        EcgChatManager.instance.resetData()
        super.onDestroy()
    }
}