package io.zjw.testblelib

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.mega.megablelib.*
import io.mega.megablelib.enums.GLUMode
import io.mega.megablelib.enums.MegaBleBattery
import io.mega.megablelib.model.MegaBleDevice
import io.mega.megablelib.model.bean.*
import io.mega.megableparse.MegaPrBean
import io.mega.megableparse.MegaRawData
import io.mega.megableparse.MegaSpoPrBean
import io.mega.megableparse.ParsedHRVBean
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.zjw.testblelib.bean.BpDataEvent
import io.zjw.testblelib.databinding.ActivityMainBinding
import io.zjw.testblelib.db.DBInstance
import io.zjw.testblelib.db.DataEntity
import io.zjw.testblelib.dfu.DfuService
import io.zjw.testblelib.reports.RealtimeBpActivity
import io.zjw.testblelib.reports.ReportListActivity
import io.zjw.testblelib.ui.SimpleMainActivity
import no.nordicsemi.android.dfu.DfuProgressListener
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import org.greenrobot.eventbus.EventBus
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), View.OnClickListener, OnChooseTimeListener {
    companion object {
        private const val TAG = "MainActivity"
        private const val SCAN_PERIOD: Long = 5
        private const val EVENT_CONNECTED = 10000
        private const val EVENT_DISCONNECTED = 10001
        private const val REQUEST_ENABLE_BT = 10002
        private const val REQUESTCODE_CHOOSE_FILE = 20000
        private const val REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124
        private const val REQUEST_ACCESS_COARSE_LOCATION = 2
    }

    /**
     * 固件升级 dfu 状态监听
     */
    private var mDfuProgressListener: DfuProgressListener = object : DfuProgressListenerAdapter() {
        override fun onProgressChanged(
            deviceAddress: String,
            percent: Int,
            speed: Float,
            avgSpeed: Float,
            currentPart: Int,
            partsTotal: Int
        ) {
            super.onProgressChanged(
                deviceAddress,
                percent,
                speed,
                avgSpeed,
                currentPart,
                partsTotal
            )
            binding.tvDfuProgress!!.text = percent.toString()
        }

        override fun onDfuCompleted(deviceAddress: String) {
            Log.d(TAG, "dfu listener, onDfuCompleted")
            // dfu 完成
            // 根据实际情况处理dfu完成后的业务。例如：重连
        }

        override fun onDfuProcessStarted(deviceAddress: String) {
            Log.d(TAG, "dfu listener, onDfuProcessStarted")
        }

        override fun onError(deviceAddress: String, error: Int, errorType: Int, message: String) {
            Log.d(TAG, "dfu listener, onDfuError")
        }

        override fun onDeviceConnected(deviceAddress: String) {
            Log.d(TAG, "dfu listener, onDeviceConnected")
        }

        override fun onDeviceDisconnected(deviceAddress: String) {
            Log.d(TAG, "dfu listener, onDeviceDisconnected")
        }
    }
    private val handler = Handler(Handler.Callback { msg: Message ->
        when (msg.what) {
            EVENT_CONNECTED -> {
                val bundle = msg.data
                val device = bundle.getSerializable("device") as MegaBleDevice
                setMegaDeviceInfo(device)
            }
            EVENT_DISCONNECTED -> clearMegaDeviceInfo()
            else -> {
            }
        }
        true
    })

    private var mScannedAdapter: ScannedAdapter? = null

    private var mRequestPermissionHandler: RequestPermissionHandler? = null

    // ble
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private val mScannedDevices: MutableList<ScannedDevice> = ArrayList()
    private var isParsing = false


    // sdk ble
    private var mUri: Uri? = null
    private var megaBleClient: MegaBleClient? = null
    private var megaBleDevice: MegaBleDevice? = null
    private lateinit var infoDialog: AlertDialog
    private var periodTime = 0L
    private val mLeScanCallback =
        LeScanCallback { device: BluetoothDevice, rssi: Int, scanRecord: ByteArray ->
            if (device.name == null) return@LeScanCallback
            // 过滤掉了设备名称中不含ring的设备，请看情况使用
            if (!device.name.toLowerCase().contains("ring") && !device.name.toLowerCase()
                    .contains("mr")
            ) return@LeScanCallback
            // 戒指设备广播scanRecord的长度为62
            if (scanRecord.size < 62) return@LeScanCallback
            // 解析广播事例，先解析3代戒指广播
            val advOnly = MegaAdvParse.parse(scanRecord)
            if (advOnly == null) { // 不行了再解析2代戒指广播
                val advEntity = MegaBleClient.parseScanRecord(device, scanRecord)
                if (advEntity != null) Log.d(TAG, advEntity.toString())
            } else {
                Log.d(TAG, advOnly.toString())
            }
            val scannedDevice = ScannedDevice(device.name, device.address, rssi)
            Log.d(TAG, "device: $scannedDevice")
            val i = mScannedDevices.indexOf(scannedDevice)
            if (i == -1) {
                mScannedDevices.add(scannedDevice)
            } else {
                mScannedDevices[i].rssi = rssi
            }
            mScannedDevices.sortByDescending {
                it.rssi
            }
        }
    private val megaBleCallback: MegaBleCallback = object : MegaBleCallback() {
        override fun onConnectionStateChange(connected: Boolean, device: MegaBleDevice?) {
            Log.d(TAG, "onConnectionStateChange: $connected")
            if (connected) {
                megaBleDevice = device
                sendToHandler(device)
            } else {
                handler.sendEmptyMessage(EVENT_DISCONNECTED)
            }
        }

        override fun onDfuBleConnectionChange(connected: Boolean, device: MegaBleDevice?) {
            Log.d(TAG, "onDfuBleConnectionChange: $connected")
            if (connected) {
                megaBleDevice = device // 务必别忘了这一步
            }
        }

        override fun onError(code: Int) {
            Log.e(TAG, "Error code: $code")
        }

        override fun onStart() {
            val userId = "5837288dc59e0d00577c5f9a" // 12 2 digit hex strings, total 24 length
//            String userId = "313736313031373332323131"; // 12 2 digit hex strings, total 24 length
//            String mac = "BC:E5:9F:48:81:3F";
//            megaBleClient.startWithoutToken(userId, mac);
//            String random = "129,57,79,122,227,76";
//            megaBleClient.startWithToken(userId, random);

            val token =
                UtilsSharedPreference.get(this@MainActivity, UtilsSharedPreference.KEY_TOKEN)
            runOnUiThread { binding.etToken!!.setText(token) }
            if (TextUtils.isEmpty(token)) {
//                megaBleClient!!.startWithoutToken(userId, megaBleDevice!!.mac)
                // 下面的方法更易用
                megaBleClient!!.startWithToken(userId, "0,0,0,0,0,0")
            } else {
                megaBleClient!!.startWithToken(userId, token)
            }
        }

        override fun onDeviceInfoReceived(device: MegaBleDevice) {
            // 提前获取电池电量、监测模式；在此处调用，可以在idle之前获得
            // 方便在idle后判断该做什么事
            megaBleClient!!.getV2Batt()
            Log.d(TAG, "onDeviceInfoReceived $device")
            sendToHandler(device)
        }

        // default setting. Use this default value is ok.
        // 强制设置用户身体信息，请使用默认值即可，不用更改
        override fun onSetUserInfo() {
            megaBleClient!!.setUserInfo(
                25.toByte(),
                1.toByte(),
                170.toByte(),
                60.toByte(),
                0.toByte()
            )
        }

        override fun onIdle() {
            // 设备闲时，可开启实时、开启长时监控、收监控数据。
            // 长时监控数据会被记录到戒指内部，实时数据不会。
            // 长时监控开启后，可断开蓝牙连接，戒指将自动保存心率血氧数据，以便后续手机连上收取。默认每次连上会同步过来。
            // 绑定token有变动时，用户信息，监测数据将被清空。
            // 建议默认开启全局实时通道，无需关闭，重复开启无影响
            // suggested setting, repeated call is ok.
            Log.d(TAG, "Important: the remote device is idle.")
            megaBleClient!!.toggleLive(true)
            megaBleClient!!.getV2Mode()
            runOnUiThread {
                mScannedDevices.clear()
                mScannedAdapter?.notifyDataSetChanged()
            }
        }

        // 血氧实时模式、脉诊模式共用此回调
        override fun onV2LiveSpoLive(live: MegaV2LiveSpoLive) {
            Log.d(TAG, "accX:${live.accX} accY:${live.accY} accZ:${live.accZ}")
            when (live.status) {
                MegaBleConst.STATUS_LIVE_VALID -> {
                    updateV2Live("$live(valid)")
                }
                MegaBleConst.STATUS_LIVE_PREPARING -> {
                    updateV2Live("$live(preparing)")
                }
                MegaBleConst.STATUS_LIVE_INVALID -> {
                    updateV2Live("$live(invalid)")
                }
            }
        }

        // 实时血氧仪模式的回调
        override fun onV2LiveSpoMonitor(live: MegaV2LiveSpoMonitor) {
            when (live.status) {
                MegaBleConst.STATUS_LIVE_VALID -> {
                    updateV2Live(live)
                }
                MegaBleConst.STATUS_LIVE_PREPARING -> {
                    updateV2Live("$live(preparing)")
                }
                MegaBleConst.STATUS_LIVE_INVALID -> {
                    updateV2Live("$live(invalid)")
                }
            }
        }

        override fun onV2LiveSport(live: MegaV2LiveSport) {
            when (live.status) {
                MegaBleConst.STATUS_LIVE_VALID -> {
                    updateV2Live(live)
                }
                MegaBleConst.STATUS_LIVE_PREPARING -> {
                    updateV2Live("$live(preparing)")
                }
                MegaBleConst.STATUS_LIVE_INVALID -> {
                    updateV2Live("$live(invalid)")
                }
            }
        }

        // 请提示用户：敲击、晃动戒指
        // show user a window to shaking, in order to bind the ring
        // if token is matched (bind success), this step will be skipped
        override fun onKnockDevice() {
            Log.d(TAG, "onKnockDevice!!!")
            runOnUiThread {
                val alertDialog = AlertDialog.Builder(this@MainActivity).create()
                alertDialog.setTitle("Hint")
                alertDialog.setMessage("Please shake device")
                alertDialog.setButton(
                    AlertDialog.BUTTON_NEUTRAL, "OK"
                ) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                alertDialog.show()
            }
        }

        // 绑定成功得到token, 下次用token验证匹配无需再敲击
        // get token when bind success, next time start with this token
        override fun onTokenReceived(token: String) {
            Log.d(TAG, "onTokenReceived: $token")
            runOnUiThread { binding.etToken!!.setText(token) }
            UtilsSharedPreference.put(this@MainActivity, UtilsSharedPreference.KEY_TOKEN, token)
        }

        override fun onRssiReceived(rssi: Int) {
            Log.d(TAG, "onRssiReceived: $rssi")
            runOnUiThread { binding.tvRssi!!.text = rssi.toString() }
        }

        // 重要
        override fun onBatteryChanged(value: Int, status: Int) {
            runOnUiThread {
                // 请使用MegaBleBattery来判断电池状态，充电中可以收数据，但不能做监测相关动作
                // 请随时响应电池状态改变
                // normal(0, "normal"),
                // charging(1, "charging"),
                // full(2, "full"),
                // lowPower(3, "lowPower");
                // error(4, "error");
                // shutdown(5, "shutdown");
                binding.tvBatt!!.text = value.toString()
                binding.tvBattStatus!!.text = MegaBleBattery.getDescription(status)
                when (status) {
                    MegaBleBattery.charging.ordinal -> { /* todo */
                    }
                    MegaBleBattery.normal.ordinal -> { /* todo */
                    }
                    MegaBleBattery.lowPower.ordinal -> { /* todo */
                    }
                    MegaBleBattery.full.ordinal -> { /* todo */
                    }
                }
            }
        }

        override fun onV2ModeReceived(mode: MegaV2Mode) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, mode.toString(), Toast.LENGTH_LONG).show()
            }
            when (mode.mode) {
                MegaBleConst.MODE_DAILY -> {
                    // In daily mode, you can sync data/turn on monitor.If device is not in this mode, the operation will get error.
                    // 正处于日常模式,可以收数据,开监测,不再此模式收数据或者开监测会失败，详情请参考错误码.
                }
                MegaBleConst.MODE_MONITOR -> {
                    // In SPO2Monitor mode
                    // 正处于血氧长时监测模式
                }
                MegaBleConst.MODE_LIVE -> {
                    // In liveSPO2 mode
                    // 正处于监测实时监测模式
                }
                MegaBleConst.MODE_SPORT -> {
                    // In sport mode
                    // 正处于运动模式
                }
                MegaBleConst.MODE_PULSE -> {
                    // In pulse mode
                    // 正处于脉诊模式
                }
            }
        }

        /**
         * 固件升级
         * dfu 库使用参考
         * https://github.com/NordicSemiconductor/Android-DFU-Library/tree/release/documentation
         */
        override fun onReadyToDfu() {
            // if不写的话，在高版本的系统上可能会有问题
            // 另行定义方法，接收mac, name, path也可；发送消息给升级页面也可。这里就直接写在回调里了
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DfuServiceInitiator.createDfuNotificationChannel(this@MainActivity)
            }
            val starter = DfuServiceInitiator(megaBleDevice!!.mac)
                .setDeviceName(megaBleDevice!!.name) // onDfuBleConnectionChange里device赋值的原因，这里要用
                .setKeepBond(false)
                .setZip(mUri!!)
            starter.setDisableNotification(true) // 禁止notification的交互
            starter.start(this@MainActivity, DfuService::class.java)
        }

        override fun onSyncDailyDataComplete(bytes: ByteArray?) {
            Log.d(TAG, "onSyncDailyDataComplete: " + bytes?.contentToString())
            var result = megaBleClient!!.parseDailyEntry(bytes)
            Log.d(TAG, "$result")
            runOnUiThread {
                Toast.makeText(this@MainActivity, "parse success", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onSyncNoDataOfDaily() {
            Log.d(TAG, "no daily data")
        }

        override fun onSyncingDataProgress(progress: Int) {
            Log.d(TAG, "onSyncingDataProgress: $progress")
            runOnUiThread { binding.tvSyncProgress.text = progress.toString() }
        }

        override fun onSyncMonitorDataComplete(
            bytes: ByteArray?,
            dataStopType: Int,
            dataType: Int,
            uid: String?,
            steps: Int
        ) {
            Log.d(TAG, "onSyncMonitorDataComplete: " + bytes?.contentToString())
            Log.d(TAG, "uid: $uid")
            Log.d(TAG, "dataType: $dataType")
            Log.d(TAG, "parseVersion:${MegaBleClient.megaParseVersion()}")
            when (dataType) {
                MegaBleConst.MODE_MONITOR -> {
                    megaBleClient!!.parseSpoPr(bytes, parseSpoPrResult)
                }
                MegaBleConst.MODE_SPORT -> {
                    megaBleClient!!.parseSport(bytes, parsePrResult)
                }
                MegaBleConst.TYPE_HRV -> {
                    megaBleClient?.parseHrvData(bytes, parseHRVResult)
                }
                else -> {
                }
            }
        }

        override fun onSyncNoDataOfMonitor() {
            Log.d(TAG, "no data")
        }

        override fun onSyncNoDataOfGlu() {
            Log.d(TAG, "no glu data")
        }

        override fun onSyncNoDataOfHrv() {
            Log.d(TAG, "no hrv data")
        }

        // result of client cmd
        override fun onOperationStatus(
            operationType: Int,
            status: Int
        ) {
            val msg: String = when (status) {
                MegaBleConst.STATUS_OK -> {
                    when (operationType) {
                        MegaBleConfig.CMD_V2_MODE_DAILY -> "turn off monitor"
                        MegaBleConfig.CMD_V2_MODE_LIVE_SPO -> "Turn on live SPO2 mode success"
                        MegaBleConfig.CMD_V2_MODE_SPORT -> "Turn on sport mode success"
                        MegaBleConfig.CMD_V2_MODE_SPO_MONITOR -> "Turn on sleep mode success"
                        MegaBleConfig.CMD_V2_PERIOD_MONITOR -> "Set period monitor success"
                        MegaBleConfig.CMD_V2_GET_PERIOD_SETTING -> "Get period monitor info success"
                        else -> "Success"
                    }
                }
                MegaBleConst.STATUS_LOWPOWER -> {
                    "Err, low power"
                }
                MegaBleConst.STATUS_RECORDS_CTRL_ERR -> {
                    "Err, busy or redo"
                }
                MegaBleConst.STATUS_NO_DATA -> {
                    "Err, no data"
                }
                MegaBleConst.STATUS_REFUSED -> {
                    "Err, refused"
                }
                else -> {
                    "Err, unknown"
                }
            }
            runOnUiThread { Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show() }
        }

        override fun onHeartBeatReceived(heartBeat: MegaBleHeartBeat) {
            Log.i(TAG, "$heartBeat")
        }

        override fun onRawdataPath(path: String?) {
            super.onRawdataPath(path)
        }

        override fun onRawdataParsed(a: Array<out IntArray>?) {
            // [[red, infra], [red, infra]]
            // log -> onRawdataParsed: 48642, 49911; 48080, 49188
            Log.d(TAG, "onRawdataParsed: " + a?.joinToString("; ") { i -> i.joinToString(", ") })
        }

        override fun onRawDataComplete(path: String, length: Int) {
            Log.d(TAG, "onRawDataComplete: $path")
        }

        override fun onV2PeriodSettingReceived(setting: MegaV2PeriodSetting?) {
            Log.i(TAG, "onV2PeriodSettingReceived:$setting")
        }

        override fun onCrashLogReceived(bytes: ByteArray?) {
            Log.i(TAG, "onCrashLogReceived:${bytes?.contentToString()}")
        }

        override fun onRawdataReceived(data: ByteArray?) {
            Log.i(TAG, "onRawdataReceived() ${data?.contentToString()}")
        }

        override fun onRawdataParsed(data: Array<out MegaRawData>?) {
            Log.i(TAG, "onRawdataParsed: ${data!!.size}")
            data?.apply {
                for (item in data) {
                    println("--->${item}")
                }
            }
        }

        override fun onTotalBpDataReceived(data: ByteArray?, duration: Int) {
            Log.i(TAG, "onTotalBpDataReceived() size:${data?.size} duration:$duration")
            val calendar = Calendar.getInstance()
            val timeInMillis = calendar.timeInMillis
            val timeHHmm = calendar.get(Calendar.HOUR_OF_DAY) * 100 + calendar.get(Calendar.MINUTE)
            val result = megaBleClient?.parseBpData(data, timeHHmm, 134.0, 80.0)
            Log.i(TAG, "onTotalBpDataReceived() $result")
            if (duration >= 60) {
                isParsing = false
                megaBleClient?.enableV2ModeEcgBp(false, bpCfg)
                Log.i(TAG, "onTotalBpDataReceived() ecg bp off.")
                return
            }
            result?.let { it ->
                if(it.chEcg != null && it.dataNum >= 400 && isParsing){
                    val ecgData = FloatArray(100)
                    System.arraycopy(
                            it.chEcg,
                            it.dataNum - 100,
                            ecgData,
                            0,
                            100
                    )
                    EventBus.getDefault().post(BpDataEvent(it.apply {
                        this.chEcg = ecgData
                    }, duration))
                }
                if (it.flag == 1) {
                    isParsing = false
                    megaBleClient?.enableV2ModeEcgBp(false, bpCfg)
                    Log.i(TAG, "onTotalBpDataReceived() ecg bp off.")
                    val dataEntity = DataEntity()
                    dataEntity.apply {
                        this.dataType = 5
                        this.configDBP = 80.0F
                        this.configSBP = 134.0F
                        this.SBP = it.SBP
                        this.DBP = it.DBP
                        this.data = data
                        this.date = timeInMillis
                        this.pr = it.pr
                        this.chEcg = it.chEcg
                    }
                    DBInstance.INSTANCE.addDataEntity(dataEntity)
                }
            }
        }
    }

    private var bpCfg: MegaRawdataConfig? = null
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mRequestPermissionHandler = RequestPermissionHandler()
        checkAppPermission()
        initView()
        initBle()
        // mock id, key，use yours
        megaBleClient = MegaBleBuilder()
            .withSecretId("D4CE5DD515F81247")
            .withSecretKey("uedQ2MgVEFlsGIWSgofHYHNdZSyHmmJ5")
            .withContext(this)
            .uploadData(true)
            .withCallback(megaBleCallback)
            .build()
        // 开发测试时，可以开启debug
        megaBleClient!!.setDebugEnable(true)
    }

    override fun onDestroy() {
        if (infoDialog != null && infoDialog.isShowing)
            infoDialog.dismiss()
        if (chooseTimeDialog.isShowing)
            chooseTimeDialog.dismiss()
        if (megaBleClient != null) megaBleClient!!.disConnect()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        DfuServiceListenerHelper.registerProgressListener(this, mDfuProgressListener)
    }

    override fun onPause() {
        super.onPause()
        DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener)
    }

    private fun initView() {
        try {
            val versionName = packageManager.getPackageInfo(this.packageName, 0).versionName
            var message = String.format(
                getString(R.string.info_content),
                versionName,
                MegaBleClient.megaParseVersion()
            )
            infoDialog = AlertDialog.Builder(this)
                .setTitle(R.string.info_title)
                .setPositiveButton(R.string.ok, null)
                .setMessage(message)
                .create()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        setSupportActionBar(binding.toolbar)
        binding.btnScan.setOnClickListener(this)
        binding.recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        LinearSnapHelper().attachToRecyclerView(binding.recyclerView)
        mScannedAdapter = ScannedAdapter(mScannedDevices)
        binding.recyclerView.adapter = mScannedAdapter
        binding.btnChooseFile.setOnClickListener(this)
        binding.btnStartDfu.setOnClickListener(this)
        binding.btnLiveOn.setOnClickListener(this)
        binding.btnMonitorOn.setOnClickListener(this)
        binding.btnSportOn.setOnClickListener(this)
        binding.btnPulseOn.setOnClickListener(this)
        binding.btnMonitorOff.setOnClickListener(this)
        binding.btnSyncData.setOnClickListener(this)
        binding.btnSyncDataNotClean.setOnClickListener(this)
        binding.btnSyncDailyData.setOnClickListener(this)
        binding.tvClear.setOnClickListener(this)
        binding.btnOpenGlobalLive.setOnClickListener(this)
        binding.btnParse.setOnClickListener(this)
        binding.btnParseSport.setOnClickListener(this)
        binding.btnParseDaily.setOnClickListener(this)
        binding.btnRawdataOn.setOnClickListener(this)
        binding.btnRawdataOff.setOnClickListener(this)
        binding.btnSampleView.setOnClickListener(this)
        // get token form shardPreference
        binding.etToken.setText(UtilsSharedPreference.get(this, UtilsSharedPreference.KEY_TOKEN))
        binding.etToken.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                if (s != null) binding.tvClear.visibility = View.VISIBLE
            }

            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) { //                if (s == null) tv_clear.setVisibility(View.INVISIBLE);
//                else tv_clear.setVisibility(View.VISIBLE);
            }

            override fun afterTextChanged(s: Editable) {
                if (s.isEmpty()) binding.tvClear.visibility = View.INVISIBLE
            }
        })
        findViewById<View>(android.R.id.content).setOnTouchListener { _, _ ->
            if (this@MainActivity.currentFocus != null) {
                binding.tvClear.visibility = View.INVISIBLE
                binding.etToken.clearFocus()
                val inputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(
                    this@MainActivity.currentFocus!!.windowToken,
                    0
                )
            }
            false
        }
        binding.btnEnablePeriod.setOnClickListener(this)
        binding.btnDisablePeriod.setOnClickListener(this)
        binding.btnPeriodStartTime.setOnClickListener(this)
        binding.btnPeriodSetting.setOnClickListener(this)
        binding.btnSyncGluData.setOnClickListener(this)
        binding.btnGetMode.setOnClickListener(this)
        binding.btnTurnOnGlu.setOnClickListener(this)
        binding.btnTurnOffGlu.setOnClickListener(this)
        chooseTimeDialog = ChooseTimeDialog(this, this)
        binding.btnGetCrashLog.setOnClickListener(this)
        binding.btnBpOn.setOnClickListener(this)
        binding.btnBpOff.setOnClickListener(this)
        binding.btnParseHrv.setOnClickListener(this)
        binding.btnSyncHrv.setOnClickListener(this)
        binding.btnHrvOn.setOnClickListener(this)
        binding.btnGetRawdata.setOnClickListener(this)
    }

    private fun initBle() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
    }

    lateinit var chooseTimeDialog: ChooseTimeDialog

    override fun onClick(v: View) {
        if (v.id != R.id.btn_choose_file
            && v.id != R.id.btn_scan
            && v.id != R.id.btn_parse
            && v.id != R.id.btn_parse_sport
            && v.id != R.id.btn_parse_daily
            && v.id != R.id.btn_parse_hrv
            && v.id != R.id.btn_sample_view
            && R.id.tv_clear != v.id
        ) {
            if (megaBleDevice == null) {
                Toast.makeText(this, "ble offline", Toast.LENGTH_SHORT).show()
                return
            }
        }
        when (v.id) {
            R.id.btn_scan -> {
                if (PermissionChecker.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                        REQUEST_ACCESS_COARSE_LOCATION
                    )
                    return
                }
                scanLeDevices()
            }
            R.id.btn_choose_file -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "application/zip" // 选择 .zip 文件

                startActivityForResult(intent, REQUESTCODE_CHOOSE_FILE)
            }
            R.id.btn_start_dfu -> {
                if (mUri == null) {
                    Toast.makeText(this, R.string.not_choose_tip, Toast.LENGTH_SHORT).show()
                    return
                }
                megaBleClient!!.startDfu(megaBleDevice!!.mac)
            }
            // 开全局live监听
            R.id.btn_open_global_live -> megaBleClient!!.toggleLive(true)
            // 开实时血氧
            R.id.btn_live_on -> megaBleClient!!.enableV2ModeLiveSpo(true)
            // 关监控
            R.id.btn_monitor_off -> megaBleClient!!.enableV2ModeDaily(true)
            // 开血氧
            R.id.btn_monitor_on -> megaBleClient!!.enableV2ModeSpoMonitor(true)
            // 开运动
            R.id.btn_sport_on -> megaBleClient!!.enableV2ModeSport(true)
            // 开脉诊
            R.id.btn_pulse_on -> megaBleClient!!.enableV2ModePulse(true)
            // 开HRV，固件版本为5.*.*****且大于5.0.11804需要打开HRV开关，收取报告时才有HRV数据，关闭HRV为enableV2HRV(false)
            R.id.btn_hrv_on -> megaBleClient!!.enableV2HRV(true)
            R.id.btn_sync_data_not_clean -> {
                megaBleClient!!.syncDataWithoutClearData()
            }
            // 收数据
            R.id.btn_sync_data -> {
                megaBleClient!!.syncData()
            }
            R.id.btn_sync_daily_data -> megaBleClient!!.syncDailyData()
            R.id.btn_get_rawdata -> megaBleClient!!.getRawData()
            R.id.tv_clear -> {
                binding.etToken!!.text = null
                UtilsSharedPreference.remove(this, UtilsSharedPreference.KEY_TOKEN)
            }
            // 解析血氧
            R.id.btn_parse ->
                // mock data; 解析血氧监测数据
                // byte[] bytes = new byte[]{1, 2, 3, 4, 5}; // an invalid mock data, return null
                try {
                    val bytes = readMockFromAsset("mock_spo2.bin")
                    Log.d(TAG, "parseVersion:${MegaBleClient.megaParseVersion()}")
                    megaBleClient!!.parseSpoPr(bytes, parseSpoPrResult)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            // 解析运动
            R.id.btn_parse_sport ->
                // mock data; 解析运动监测数据
                // byte[] bytes1 = new byte[]{1, 2, 3, 4, 5}; // an invalid mock data, return null
                try {
                    val bytes1 = readMockFromAsset("mock_sport.bin")
                    Log.d(TAG, "parseVersion:${MegaBleClient.megaParseVersion()}")
                    megaBleClient!!.parseSport(bytes1, parsePrResult)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            // 解析日常
            R.id.btn_parse_daily ->
                // mock data; 解析日常计步数据
                // byte[] bytes = new byte[]{1, 2, 3, 4, 5}; // an invalid mock data, return null
                try {
                    val bytes = readMockFromAsset("mock_daily.bin")
                    Log.d(TAG, "parseVersion:${MegaBleClient.megaParseVersion()}")
                    var result = megaBleClient!!.parseDailyEntry(bytes)
                    Log.d(TAG, "$result")
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            R.string.parse_success,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            // 开rawdata
            R.id.btn_rawdata_on -> {
                val rawdataConfig = MegaRawdataConfig(true, false, "", 0)
                megaBleClient!!.enableRawdataPulse(rawdataConfig)//rawdata for pulse
//                megaBleClient!!.enableRawdataSpo(rawdataConfig)//rawdata for spo2
//                megaBleClient!!.enableRawdata(rawdataConfig)
            }
            // 关rawdata
            R.id.btn_rawdata_off -> megaBleClient!!.disableRawdata()
            R.id.btn_enable_period -> {
                if (periodTime == 0L) {
                    Toast.makeText(this, R.string.period_monitor_start_tip, Toast.LENGTH_LONG)
                        .show()
                    return
                }
                var selectDurationIndex = binding.spinnerPeriodMonitorDuration.selectedItemPosition
                var monitorDuration = when (selectDurationIndex) {
                    1 -> 3600
                    2 -> 3600 * 5
                    3 -> 3600 * 10
                    else -> 600
                }
                var currentTime = System.currentTimeMillis()
                if (periodTime < currentTime)
                    periodTime += 24 * 3600 * 1000
                var timeLeft = ((periodTime - currentTime) / 1000).toInt()
                Log.i(
                    TAG,
                    "monitorDuration:$monitorDuration isLoop:${binding.swLoop.isChecked} timeLeft:$timeLeft"
                )
                megaBleClient?.enableV2PeriodMonitor(
                    true,
                    binding.swLoop.isChecked,
                    monitorDuration,
                    timeLeft
                )
            }
            R.id.btn_disable_period -> {
                megaBleClient?.enableV2PeriodMonitor(false, false, 0, 0)
                Toast.makeText(this, R.string.period_disable_tip, Toast.LENGTH_LONG).show()
            }
            R.id.btn_period_start_time -> chooseTimeDialog.show()
            R.id.btn_period_setting -> megaBleClient?.getV2PeriodSetting()
            R.id.btn_get_mode -> megaBleClient?.getV2Mode()
            R.id.btn_turn_on_glu -> megaBleClient?.setGLUMode(GLUMode.MINUTES_15)
            R.id.btn_turn_off_glu -> megaBleClient?.setGLUMode(GLUMode.OFF)
            R.id.btn_sync_glu_data -> megaBleClient?.syncGluData()
            R.id.btn_get_crash_log -> megaBleClient?.getCrashLog()
            R.id.btn_bp_on -> {
                if (bpCfg == null) bpCfg = MegaRawdataConfig(false, false, "", 0)
                isParsing = true
                megaBleClient?.enableV2ModeEcgBp(true, bpCfg)
                startActivity(Intent(this, RealtimeBpActivity::class.java))
            }
            R.id.btn_bp_off -> {
                if (bpCfg == null) bpCfg = MegaRawdataConfig(false, false, "", 0)
                isParsing = false
                megaBleClient?.enableV2ModeEcgBp(false, bpCfg)
            }
            R.id.btn_parse_hrv -> {
                try {
                    val bytes = readMockFromAsset("mock_hrv.bin")
                    Log.d(TAG, "parseVersion:${MegaBleClient.megaParseVersion()}")
                    megaBleClient!!.parseHrvData(bytes, parseHRVResult)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            R.id.btn_sync_hrv -> {
                megaBleClient?.syncHrvData()
            }
            R.id.btn_sample_view -> {
                if(megaBleDevice != null){
                    megaBleClient?.disConnect()
                }
                startActivity(Intent(this, SimpleMainActivity::class.java))
            }
            else -> {
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item!!.itemId == R.id.menu_info) {
            infoDialog.show()
        } else if (item.itemId == R.id.menu_chart) {
            startActivity(Intent(this, ReportListActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("CheckResult")
    private fun scanLeDevices() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "ble not support", Toast.LENGTH_SHORT).show()
            return
        }
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val statusOfGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!statusOfGPS) {
            Toast.makeText(this, "GPS disable, plz enable", Toast.LENGTH_SHORT).show()
            return
        }
        if (!mBluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            return
        }
        mScannedDevices.clear()
        mScannedAdapter!!.notifyDataSetChanged()
        binding.btnScan!!.isClickable = false
        Observable.timer(SCAN_PERIOD, TimeUnit.SECONDS).subscribe { aLong: Long? ->
            mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
            binding.btnScan!!.isClickable = true
        }
        mBluetoothAdapter!!.startLeScan(mLeScanCallback)
        Observable.interval(1, TimeUnit.SECONDS)
            .take(SCAN_PERIOD)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { aLong: Long ->
                Log.d(TAG, aLong.toString() + "scaning...")
                mScannedAdapter!!.notifyDataSetChanged()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUESTCODE_CHOOSE_FILE){
                mUri = data?.data
                mUri?.run {
                    binding.tvDfuPath!!.text = "${getFileName(this)}"
                }
            }
        } else if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                scanLeDevices()
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        if (uri.scheme == "content") {
            // 查询内容提供者，获取文件名
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    fileName = cursor.getString(nameIndex)
                }
            }
        } else if (uri.scheme == "file") {
            // 如果是 file:// URI，直接获取文件名
            fileName = File(uri.path!!).name
        }
        return fileName
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mRequestPermissionHandler!!.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )
    }

    internal inner class ScannedAdapter(private val mList: List<ScannedDevice>) :
        RecyclerView.Adapter<ScannedViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScannedViewHolder {
            val view = LayoutInflater.from(this@MainActivity)
                .inflate(R.layout.item_scan_result, parent, false)
            return ScannedViewHolder(view)
        }

        override fun onBindViewHolder(holder: ScannedViewHolder, position: Int) {
            val device = mList[position]
            holder.bindView(device)
        }

        override fun getItemCount(): Int {
            return mList.size
        }

    }

    internal inner class ScannedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val v: CardView = itemView as CardView
        private val tvName: TextView
        private val tvAddress: TextView
        private val tvRssi: TextView

        init {
            tvName = v.findViewById(R.id.tv_name)
            tvAddress = v.findViewById(R.id.tv_mac)
            tvRssi = v.findViewById(R.id.tv_rssi)
        }

        fun bindView(device: ScannedDevice) {
            tvName.text = device.name
            tvAddress.text = device.address
            tvRssi.text = device.rssi.toString()
            v.setOnClickListener { v: View? ->
                Log.i(TAG, "Connecting -> " + device.name + " " + device.address)
                Toast.makeText(
                    this@MainActivity,
                    "Connecting -> " + device.name + " " + device.address,
                    Toast.LENGTH_SHORT
                ).show()
                megaBleClient!!.connect(device.address, device.name)
            }
        }
    }

    /**
     * set UI
     */
    private fun setMegaDeviceInfo(device: MegaBleDevice) {
        binding.mainContent.visibility = View.VISIBLE
        binding.tvName.text = device.name
        binding.tvMac.text = device.mac
        binding.tvFwVersion.text = device.fwVer
        binding.tvSn.text = device.sn
        binding.tvOtherInfo.text = device.otherInfo
        binding.tvDeviceStatus.text = "already connected"
    }

    private fun clearMegaDeviceInfo() {
        binding.tvName.text = null
        binding.tvMac.text = null
        binding.tvFwVersion.text = null
        binding.tvSn.text = null
        binding.tvDeviceStatus.text = "offline"
        binding.tvOtherInfo.text = null
        binding.tvBatt.text = null
        binding.tvBattStatus.text = null
        binding.tvRssi.text = null
        binding.tvSpo.text = null
        binding.tvHr.text = null
        binding.tvLiveDesc.text = null
        binding.tvSyncProgress.text = null
        binding.etToken.text = null
        megaBleDevice = null
    }

    private fun sendToHandler(device: MegaBleDevice?) {
        val msg = Message.obtain()
        msg.what = EVENT_CONNECTED
        val bundle = Bundle()
        bundle.putSerializable("device", device)
        msg.data = bundle
        handler.sendMessage(msg)
    }

    private fun <T> updateV2Live(live: T) {
        runOnUiThread {
            binding.tvV2Live!!.text = live.toString()
            // 使用AnimationUtils装载动画配置文件
            val animation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.alpha)
            // 启动动画
            binding.tvSmile!!.startAnimation(animation)
        }
    }

    private fun checkAppPermission() {
        mRequestPermissionHandler!!.requestPermission(this,
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            },
            REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS,
            object : RequestPermissionHandler.RequestPermissionListener {
                override fun onSuccess() {
                    // Toast.makeText(MainActivity.this, "request permission success", Toast.LENGTH_SHORT).show();
                }

                override fun onFailed() {
                    Toast.makeText(
                        this@MainActivity,
                        "request permission failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    @Throws(IOException::class)
    private fun readMockFromAsset(filename: String): ByteArray {
        val ins = resources.assets.open(filename)
        val bos = ByteArrayOutputStream()
        val buf = ByteArray(10 * 1024)
        var size: Int
        while (ins.read(buf).also { size = it } != -1) bos.write(buf, 0, size)
        ins.close()
        bos.flush()
        bos.close()
        return bos.toByteArray()
    }

    var parseSpoPrResult = object : MegaAuth.Callback<MegaSpoPrBean> {
        override fun onSuccess(p0: MegaSpoPrBean?) {
            Log.d(TAG, p0.toString())
            runOnUiThread {
                Toast.makeText(this@MainActivity, "parse success", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onFail(p0: String) {
            Log.d(TAG, p0)
            runOnUiThread {
                Toast.makeText(this@MainActivity, "auth failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var parsePrResult = object : MegaAuth.Callback<MegaPrBean> {
        override fun onSuccess(p0: MegaPrBean?) {
            Log.d(TAG, p0.toString())
            runOnUiThread {
                Toast.makeText(this@MainActivity, "parse success", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onFail(p0: String) {
            Log.d(TAG, p0)
            runOnUiThread {
                Toast.makeText(this@MainActivity, "auth failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var parseHRVResult = object : MegaAuth.Callback<ParsedHRVBean> {
        override fun onSuccess(p0: ParsedHRVBean?) {
            Log.d(TAG, p0.toString())
            runOnUiThread {
                Toast.makeText(this@MainActivity, "parse success", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onFail(p0: String) {
            Log.d(TAG, p0)
            runOnUiThread {
                Toast.makeText(this@MainActivity, "auth failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    class ChooseTimeDialog(context: Context, listener: OnChooseTimeListener) :
        BottomSheetDialog(context) {
        var timePicker: TimePicker? = null
        var calendar: Calendar = Calendar.getInstance()

        init {
            setContentView(R.layout.view_time_picker)
            timePicker = findViewById(R.id.time_picker)
            timePicker?.setIs24HourView(true)
            findViewById<Button>(R.id.btn_ok)?.setOnClickListener {
                listener.chooseTime(timePicker!!.currentHour, timePicker!!.currentMinute)
                dismiss()
            }
        }

        override fun show() {
            calendar.timeInMillis = System.currentTimeMillis()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                timePicker?.currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                timePicker?.currentMinute = calendar.get(Calendar.MINUTE)
            } else {
                timePicker?.hour = calendar.get(Calendar.HOUR_OF_DAY)
                timePicker?.minute = calendar.get(Calendar.MINUTE)
            }
            super.show()
        }
    }

    override fun chooseTime(hour: Int, minute: Int) {
        Log.i(TAG, "$hour:$minute")
        binding.tvTime.text = "$hour:$minute"
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        periodTime = calendar.timeInMillis
    }
}

interface OnChooseTimeListener {
    fun chooseTime(hour: Int, minute: Int)
}