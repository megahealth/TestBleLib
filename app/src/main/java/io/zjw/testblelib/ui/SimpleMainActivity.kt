package io.zjw.testblelib.ui

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
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
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
import io.mega.megablelib.*
import io.mega.megablelib.enums.MegaBleBattery
import io.mega.megablelib.model.MegaBleDevice
import io.mega.megablelib.model.bean.*
import io.mega.megableparse.MegaSpoPrBean
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.zjw.testblelib.R
import io.zjw.testblelib.RequestPermissionHandler
import io.zjw.testblelib.ScannedDevice
import io.zjw.testblelib.UtilsSharedPreference
import io.zjw.testblelib.databinding.ActivityMainSimpleBinding
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class SimpleMainActivity : AppCompatActivity(), View.OnClickListener {
    companion object {
        private const val TAG = "SimpleMainActivity"
        private const val SCAN_PERIOD: Long = 5
        private const val EVENT_CONNECTED = 10000
        private const val EVENT_DISCONNECTED = 10001
        private const val REQUEST_ENABLE_BT = 10002
        private const val REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124
        private const val REQUEST_ACCESS_COARSE_LOCATION = 2
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

    private var megaBleClient: MegaBleClient? = null
    private var megaBleDevice: MegaBleDevice? = null
    private lateinit var infoDialog: AlertDialog
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
            var advOnly = MegaAdvParse.parse(scanRecord)

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

        override fun onStart() {
            val userId = "5837288dc59e0d00577c5f9a" // 12 2 digit hex strings, total 24 length
//            String userId = "313736313031373332323131"; // 12 2 digit hex strings, total 24 length
//            String mac = "BC:E5:9F:48:81:3F";
//            megaBleClient.startWithoutToken(userId, mac);
//            String random = "129,57,79,122,227,76";
//            megaBleClient.startWithToken(userId, random);

            val token =
                UtilsSharedPreference.get(this@SimpleMainActivity, UtilsSharedPreference.KEY_TOKEN)
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

        // 请提示用户：敲击、晃动戒指
        // show user a window to shaking, in order to bind the ring
        // if token is matched (bind success), this step will be skipped
        override fun onKnockDevice() {
            Log.d(TAG, "onKnockDevice!!!")
            runOnUiThread {
                val alertDialog = AlertDialog.Builder(this@SimpleMainActivity).create()
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
            UtilsSharedPreference.put(this@SimpleMainActivity, UtilsSharedPreference.KEY_TOKEN, token)
        }

        override fun onEnsoModeReceived(enable: Boolean) {
            super.onEnsoModeReceived(enable)
            Log.d(TAG, "onEnsoModeReceived:$enable")
            runOnUiThread{
                Toast.makeText(this@SimpleMainActivity, "Enso Mode:${if(enable) "on" else "off"}", Toast.LENGTH_LONG).show()
            }
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
                Toast.makeText(this@SimpleMainActivity, mode.toString(), Toast.LENGTH_LONG).show()
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
            }
        }

        //progress's range is [1-100].
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
            }
        }

        override fun onSyncNoDataOfMonitor() {
            Log.d(TAG, "no data")
        }

        override fun onRawDataComplete(path: String, length: Int) {
            Log.d(TAG, "onRawDataComplete: $path")
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
            runOnUiThread { Toast.makeText(this@SimpleMainActivity, msg, Toast.LENGTH_SHORT).show() }
        }

        override fun onError(code: Int) {
            Log.e(TAG, "Error code: $code")
        }

        override fun onHeartBeatReceived(heartBeat: MegaBleHeartBeat) {
            Log.i(TAG, "$heartBeat")
        }

    }

    private lateinit var binding: ActivityMainSimpleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainSimpleBinding.inflate(layoutInflater)
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
        if (megaBleClient != null) megaBleClient!!.disConnect()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
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
        binding.btnMonitorOn.setOnClickListener(this)
        binding.btnMonitorOff.setOnClickListener(this)
        binding.btnSyncData.setOnClickListener(this)
        binding.tvClear.setOnClickListener(this)
        binding.btnOpenGlobalLive.setOnClickListener(this)
        binding.btnParse.setOnClickListener(this)
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
            if (this@SimpleMainActivity.currentFocus != null) {
                binding.tvClear.visibility = View.INVISIBLE
                binding.etToken.clearFocus()
                val inputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(
                    this@SimpleMainActivity.currentFocus!!.windowToken,
                    0
                )
            }
            false
        }
        binding.btnGetMode.setOnClickListener(this)
        binding.btnEnableEnsoMode.setOnClickListener(this)
        binding.btnDisableEnsoMode.setOnClickListener(this)
        binding.btnGetEnsoMode.setOnClickListener(this)
        binding.btnGetRawdata.setOnClickListener(this)
    }

    private fun initBle() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
    }

    override fun onClick(v: View) {
        if (v.id != R.id.btn_scan
            && v.id != R.id.btn_parse
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
            // 开全局live监听
            R.id.btn_open_global_live -> megaBleClient!!.toggleLive(true)
            // 关监控
            R.id.btn_monitor_off -> megaBleClient!!.enableV2ModeDaily(true)
            // 开血氧
            R.id.btn_monitor_on -> megaBleClient!!.enableV2ModeSpoMonitor(true)
            // 收数据
            R.id.btn_sync_data -> {
                megaBleClient!!.syncData()
            }
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
            R.id.btn_get_mode -> megaBleClient?.getV2Mode()
            R.id.btn_enable_enso_mode -> {
                megaBleClient?.enableEnsoMode(true)
            }
            R.id.btn_disable_enso_mode -> {
                megaBleClient?.enableEnsoMode(false)
            }
            R.id.btn_get_enso_mode -> {
                megaBleClient?.getEnsoMode()
            }
            else -> {
            }
        }
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
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                scanLeDevices()
            }
        }
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
            val view = LayoutInflater.from(this@SimpleMainActivity)
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
                    this@SimpleMainActivity,
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

    private fun checkAppPermission() {
        mRequestPermissionHandler!!.requestPermission(this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS,
            object : RequestPermissionHandler.RequestPermissionListener {
                override fun onSuccess() {
                    // Toast.makeText(SimpleMainActivity.this, "request permission success", Toast.LENGTH_SHORT).show();
                }

                override fun onFailed() {
                    Toast.makeText(
                        this@SimpleMainActivity,
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
                Toast.makeText(this@SimpleMainActivity, "parse success", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onFail(p0: String) {
            Log.d(TAG, p0)
            runOnUiThread {
                Toast.makeText(this@SimpleMainActivity, "auth failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

}

interface OnChooseTimeListener {
    fun chooseTime(hour: Int, minute: Int)
}