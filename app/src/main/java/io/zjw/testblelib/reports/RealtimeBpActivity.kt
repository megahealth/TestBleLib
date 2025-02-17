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
import io.zjw.testblelib.databinding.ActivityRealtimeBpBinding
import io.zjw.testblelib.utils.EcgChatManager
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
                    binding.tvStartTime.text = String.format(
                            getString(R.string.bp_test_start_time),
                            formatTimestampToEnHm(System.currentTimeMillis())
                    )
                    binding.tvStartTime.visibility = View.VISIBLE
                    binding.llPrepare.visibility = View.GONE
                    binding.tvFinish.visibility = View.GONE
                    binding.llRes.visibility = View.GONE
                    binding.llLiveData.visibility = View.VISIBLE
                    binding.chartView.visibility = View.VISIBLE
                    binding.tvDuration.text = "00:00"
                    binding.tvDuration.visibility = View.VISIBLE
                    binding.tvTestTip.visibility = View.VISIBLE
                }
                0x02 -> {
                    binding.llLiveData.visibility = View.GONE
                    binding.tvDuration.visibility = View.GONE
                    binding.tvTestTip.visibility = View.GONE
                    binding.tvFinish.visibility = View.VISIBLE
                    binding.llRes.visibility = View.VISIBLE
                    if (parsedBPBean != null && parsedBPBean!!.flag == 1) {
                        binding.tvSbpValue.text =
                                String.format(Locale.getDefault(), "%.1f", parsedBPBean!!.SBP)
                        binding.tvDbpValue.text =
                                String.format(Locale.getDefault(), "%.1f", parsedBPBean!!.DBP)
                        binding.tvResPrValue.text = "${parsedBPBean?.pr}"
                    } else {
                        binding.tvSbpValue.text = "--"
                        binding.tvDbpValue.text = "--"
                        binding.tvResPrValue.text = "--"
                    }
                }
            }
        }
    }

    private lateinit var binding: ActivityRealtimeBpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRealtimeBpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        setListener()
        startTest()
    }

    private fun initViews() {
        if (!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this)
        }
        EcgChatManager.instance.bindView(binding.chartView)
    }

    private fun setListener() {
        binding.tvNext.setOnClickListener {
            finish()
        }
    }

    fun formatTimestampToEnHm(timestamps: Long): String? {
        return SimpleDateFormat("HH:mm").format(Date(timestamps))
    }

    private fun startTest() {
        binding.tvSbpValue.text = ""
        binding.tvDbpValue.text = ""
        binding.tvPrValue.text = ""
        binding.tvDuration.text = ""
        binding.tvStartTime.visibility = View.INVISIBLE
        binding.tvFinish.visibility = View.GONE
        binding.chartView.visibility = View.GONE
        binding.llRes.visibility = View.GONE
        binding.llLiveData.visibility = View.GONE
        binding.tvDuration.visibility = View.GONE
        binding.tvTestTip.visibility = View.GONE
        binding.llPrepare.visibility = View.VISIBLE
        EcgChatManager.instance.resetData()
        handler.sendEmptyMessageDelayed(0x01, 10000)
        isParsing = true
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBpDataEventReceived(bpDataEvent: BpDataEvent){
        Log.i("RealtimeBpActivity", bpDataEvent.parsedBPBean.toString())
        parsedBPBean = bpDataEvent.parsedBPBean
        binding.tvDuration.text = formatDurationToMmSs(bpDataEvent.duration.toLong())
        if (binding.chartView.visibility == View.VISIBLE){
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