package io.zjw.testblelib;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.leon.lfilepickerlibrary.LFilePicker;
import com.leon.lfilepickerlibrary.utils.Constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.mega.megablelib.MegaAdvParse;
import io.mega.megablelib.MegaAuth;
import io.mega.megablelib.MegaBleBuilder;
import io.mega.megablelib.MegaBleCallback;
import io.mega.megablelib.MegaBleClient;
import io.mega.megablelib.MegaBleConst;
import io.mega.megablelib.enums.MegaBleBattery;
import io.mega.megablelib.model.MegaBleDevice;
import io.mega.megablelib.model.bean.MegaAdvertisingParsedEntity;
import io.mega.megablelib.model.bean.MegaBleHeartBeat;
import io.mega.megablelib.model.bean.MegaV2LiveSpoLive;
import io.mega.megablelib.model.bean.MegaV2LiveSpoMonitor;
import io.mega.megablelib.model.bean.MegaV2LiveSport;
import io.mega.megablelib.model.bean.MegaV2Mode;
import io.mega.megableparse.ParsedPrBean;
import io.mega.megableparse.ParsedSpoPrBean;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.zjw.testblelib.dfu.DfuService;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private static final long SCAN_PERIOD = 5;
    private static final int EVENT_CONNECTED = 10000;
    private static final int EVENT_DISCONNECTED = 10001;
    private static final int REQUEST_ENABLE_BT = 10002;

    private static final int REQUESTCODE_CHOOSE_FILE = 20000;
    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    private static final int REQUEST_ACCESS_COARSE_LOCATION = 2;

    /**
     * 固件升级 dfu 状态监听
     */
    DfuProgressListener mDfuProgressListener = new DfuProgressListenerAdapter() {
        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            super.onProgressChanged(deviceAddress, percent, speed, avgSpeed, currentPart, partsTotal);
            tvDfuProgress.setText(String.valueOf(percent));
        }

        @Override
        public void onDfuCompleted(String deviceAddress) {
            Log.d(TAG, "dfu listener, onDfuCompleted");
            // dfu 完成
            // 根据实际情况处理dfu完成后的业务。例如：重连
        }

        @Override
        public void onDfuProcessStarted(String deviceAddress) {
            Log.d(TAG, "dfu listener, onDfuProcessStarted");
        }

        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            Log.d(TAG, "dfu listener, onDfuError");
        }

        @Override
        public void onDeviceConnected(String deviceAddress) {
            Log.d(TAG, "dfu listener, onDeviceConnected");
        }

        @Override
        public void onDeviceDisconnected(String deviceAddress) {
            Log.d(TAG, "dfu listener, onDeviceDisconnected");
        }
    };

    private Handler handler = new Handler(msg -> {
        switch (msg.what) {
            case EVENT_CONNECTED:
                Bundle bundle = msg.getData();
                MegaBleDevice device = (MegaBleDevice) bundle.getSerializable("device");
                setMegaDeviceInfo(device);
                break;
            case EVENT_DISCONNECTED:
                clearMegaDeviceInfo();
                break;
            default:
                break;
        }

        return true;
    });

    // ui
    private Button btnScan;
    private RecyclerView mRecyclerView;
    private ScannedAdapter mScannedAdapter;
    private LinearLayout mainContent;
    private TextView tvStatus;
    private TextView tvName;
    private TextView tvMac;
    private TextView tvSN;
    private TextView tvVersion;
    private TextView tvOtherInfo;
    private Button btnChooseFile;
    private TextView tvDfuPath;
    private Button btnStartDfu;
    private TextView tvBatt;
    private TextView tvBattStatus;
    private TextView tvRssi;
    private TextView tvAppVersionName;
    private Button btnMonitorOn;
    private Button btnSportOn;
    private Button btnMonitorOff; // 血氧监测，运动监测，实时监测的关闭都是同一个
    private Button btnSyncData;
    private TextView tvSpo;
    private TextView tvHr;
    private TextView tvLiveDesc;
    private Button btnLiveOn;
    private Button btnOpenGlobalLive;
    private TextView tvSyncProgress;
    private TextView tvSmile;
    private EditText etToken;
    private TextView tvClear;
    private TextView tvV2Live;
    private Button btnParse;
    private Button btnParseSport;
    private TextView tvDfuProgress;

    // permission checker
    private RequestPermissionHandler mRequestPermissionHandler;


    // ble
    private BluetoothAdapter mBluetoothAdapter;
    private List<ScannedDevice> mScannedDevices = new ArrayList<>();

    // sdk ble
    private String mDfuPath;
    private MegaBleClient megaBleClient;
    private MegaBleDevice megaBleDevice;
    private BluetoothAdapter.LeScanCallback mLeScanCallback = (device, rssi, scanRecord) -> {
        if (device.getName() == null) return;
        MegaAdvertisingParsedEntity advEntity = MegaBleClient.parseScanRecord(device, scanRecord);
        if (advEntity != null) Log.d(TAG, advEntity.toString());
        ScannedDevice scannedDevice = new ScannedDevice(device.getName(), device.getAddress(), rssi);
        Log.d(TAG, "device: " + scannedDevice.toString());
        int i = mScannedDevices.indexOf(scannedDevice);
        if (i == -1) {
            mScannedDevices.add(scannedDevice);
        } else {
            mScannedDevices.get(i).setRssi(rssi);
        }
    };

    private MegaBleCallback megaBleCallback = new MegaBleCallback() {
        @Override
        public void onConnectionStateChange(boolean connected, MegaBleDevice device) {
            Log.d(TAG, "onConnectionStateChange: " + connected);

            if (connected) {
                megaBleDevice = device;
                sendToHandler(device);
            } else {
                handler.sendEmptyMessage(EVENT_DISCONNECTED);
            }
        }

        @Override
        public void onDfuBleConnectionChange(boolean connected, MegaBleDevice device) {
            Log.d(TAG, "onDfuBleConnectionChange: " + connected);
            if (connected) {
                megaBleDevice = device; // 务必别忘了这一步
            }
        }

        @Override
        public void onError(int code) {
            Log.e(TAG, "Error code: " + code);
        }

        @Override
        public void onStart() {
            String userId = "5837288dc59e0d00577c5f9a"; // 12 2 digit hex strings, total 24 length
//            String userId = "313736313031373332323131"; // 12 2 digit hex strings, total 24 length
//            String mac = "BC:E5:9F:48:81:3F";
//            megaBleClient.startWithoutToken(userId, mac);
//            String random = "129,57,79,122,227,76";
//            megaBleClient.startWithToken(userId, random);


            String token = UtilsSharedPreference.get(MainActivity.this, UtilsSharedPreference.KEY_TOKEN);
            runOnUiThread(() -> etToken.setText(token));
            if (token == null) {
                megaBleClient.startWithoutToken(userId, megaBleDevice.getMac());
            } else {
                megaBleClient.startWithToken(userId, token);
            }
        }

        @Override
        public void onDeviceInfoReceived(MegaBleDevice device) {
            Log.d(TAG, "onDeviceInfoReceived" + device.toString());
            sendToHandler(device);
        }

        // default setting
        @Override
        public void onSetUserInfo() {
            megaBleClient.setUserInfo((byte) 25, (byte) 1, (byte) 170, (byte) 60, (byte) 0);
        }

        @Override
        public void onIdle() {
            // 设备闲时，可开启实时、开启长时监控、收监控数据。
            // 长时监控数据会被记录到戒指内部，实时数据不会。
            // 长时监控开启后，可断开蓝牙连接，戒指将自动保存心率血氧数据，以便后续手机连上收取。默认每次连上会同步过来。
            // 绑定token有变动时，用户信息，监测数据将被清空。

            // 建议默认开启全局实时通道，无需关闭，重复开启无影响
            // suggested setting, repeated call is ok.
            megaBleClient.toggleLive(true);
        }

        // default setting
        @Override
        public void onEnsureBindWhenTokenNotMatch() {
            megaBleClient.ensureBind(true);
        }

        @Override
        public void onV2LiveSpoLive(MegaV2LiveSpoLive live) {
            updateV2Live(live);
        }

        // 实时血氧仪模式的回调
        @Override
        public void onV2LiveSpoMonitor(MegaV2LiveSpoMonitor live) {
            updateV2Live(live);
        }

        @Override
        public void onV2LiveSport(MegaV2LiveSport live) { updateV2Live(live); }

        // 请提示用户：敲击、晃动戒指
        // show user a window to shaking, in order to bind the ring
        // if token is matched (bind success), this step will be skipped
        @Override
        public void onKnockDevice() {
            Log.d(TAG, "onKnockDevice!!!");
            runOnUiThread(() -> {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Hint");
                alertDialog.setMessage("Please shake device");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        (dialog, which) -> dialog.dismiss());
                alertDialog.show();
            });
        }

        // 绑定成功得到token, 下次用token验证匹配无需再敲击
        // get token when bind success, next time start with this token
        @Override
        public void onTokenReceived(String token) {
            Log.d(TAG, "onTokenReceived: " + token);
            runOnUiThread(() -> etToken.setText(token));
            UtilsSharedPreference.put(MainActivity.this, UtilsSharedPreference.KEY_TOKEN, token);
        }

        @Override
        public void onRssiReceived(int rssi) {
            Log.d(TAG, "onRssiReceived: " + rssi);
            runOnUiThread(() -> tvRssi.setText(String.valueOf(rssi)));
        }

        @Override
        public void onBatteryChanged(int value, int status) {
            runOnUiThread(() -> {
                // 请使用MegaBleBattery来判断电池状态，充电中可以收数据，但不能做监测相关动作
                tvBatt.setText(String.valueOf(value));
                tvBattStatus.setText(MegaBleBattery.getDescription(status));
                if (status == MegaBleBattery.charging.ordinal()) {
                    // normal(0, "normal"),
                    // charging(1, "charging"),
                    // full(2, "full"),
                    // lowPower(3, "lowPower");
                    // 在充电中，无法进行监测相关操作，但可收数据！！！请随时响应电池状态改变
                }
            });
        }

        @Override
        public void onV2ModeReceived(MegaV2Mode mode) {
            switch (mode.getMode()) {
                case MegaBleConst.MODE_MONITOR:
                    // current working mode is spo2 monitor
                    break;
                case MegaBleConst.MODE_LIVE:
                    // current working mode is live
                    break;
                case MegaBleConst.MODE_SPORT:
                    // current working mode is sport
                    break;
            }
        }

        /**
         * 固件升级
         * dfu 库使用参考
         * https://github.com/NordicSemiconductor/Android-DFU-Library/tree/release/documentation
         */
        @Override
        public void onReadyToDfu() {
            // if不写的话，在高版本的系统上可能会有问题
            // 另行定义方法，接收mac, name, path也可；发送消息给升级页面也可。这里就直接写在回调里了
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DfuServiceInitiator.createDfuNotificationChannel(MainActivity.this);
            }

            DfuServiceInitiator starter = new DfuServiceInitiator(megaBleDevice.getMac())
                    .setDeviceName(megaBleDevice.getName()) // onDfuBleConnectionChange里device赋值的原因，这里要用
                    .setKeepBond(false)
                    .setZip(mDfuPath);
            starter.setDisableNotification(true); // 禁止notification的交互
            starter.start(MainActivity.this, DfuService.class);
        }

        @Override
        public void onSyncingDataProgress(int progress) {
            Log.d(TAG, "onSyncingDataProgress: " + progress);
            runOnUiThread(() -> {
                tvSyncProgress.setText(String.valueOf(progress));
            });
        }

        @Override
        public void onSyncMonitorDataComplete(byte[] bytes, int dataStopType, int dataType) {
            Log.d(TAG, "onSyncMonitorDataComplete: " + Arrays.toString(bytes));
            if (dataType == MegaBleConst.MODE_MONITOR) {
                megaBleClient.parseSpoPr(bytes, new MegaAuth.Callback<ParsedSpoPrBean>() {
                    @Override
                    public void onFail(String s) {
                        Log.d(TAG, s);
                    }

                    @Override
                    public void onSuccess(ParsedSpoPrBean bean) {
                        Log.d(TAG, bean.toString());
                    }
                });
            } else if (dataType == MegaBleConst.MODE_SPORT) {
                megaBleClient.parseSport(bytes, new MegaAuth.Callback<ParsedPrBean>() {
                    @Override
                    public void onFail(String s) {
                        Log.d(TAG, s);
                    }

                    @Override
                    public void onSuccess(ParsedPrBean bean) {
                        Log.d(TAG, bean.toString());
                    }
                });
            }

        }

        @Override
        public void onSyncNoDataOfMonitor() {
            Log.d(TAG, "no data");
        }

        // result of client cmd
        @Override
        public void onOperationStatus(int operationType, int status) {
//            switch (operationType) {
//                case  MegaBleConfig.CMD_V2_MODE_SPO_MONITOR:
//                    //
//                break;
//                case  MegaBleConfig.CMD_V2_MODE_LIVE_SPO:
//                    //
//                break;
//                case  MegaBleConfig.CMD_V2_GET_MODE:
//                    //
//                break;
//            }

            String msg;
            if (status == MegaBleConst.STATUS_OK) {
                msg = "Success";
            } else if (status == MegaBleConst.STATUS_LOWPOWER) {
                msg = "Err, low power";
            } else if (status == MegaBleConst.STATUS_RECORDS_CTRL_ERR) {
                msg = "Err, busy or redo";
            } else if (status == MegaBleConst.STATUS_NO_DATA) {
                msg = "Err, no data";
            } else if (status == MegaBleConst.STATUS_REFUSED) {
                msg = "Err, refused";
            } else {
                msg = "Err, unknown";
            }
            runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onHeartBeatReceived(MegaBleHeartBeat heartBeat) {
            //
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRequestPermissionHandler = new RequestPermissionHandler();
        checkAppPermission();

        initView();
        initBle();

        // mock id, key，use yours
        megaBleClient = new MegaBleBuilder()
                .withSecretId("D4CE5DD515F81247")
                .withSecretKey("uedQ2MgVEFlsGIWSgofHYHNdZSyHmmJ5")
                .withContext(this)
                .withCallback(megaBleCallback)
                .build();

//        megaBleClient.setDebugEnable(true);
    }

    @Override
    protected void onDestroy() {
        if (megaBleClient != null)
            megaBleClient.disConnect();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        DfuServiceListenerHelper.registerProgressListener(this, mDfuProgressListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener);
    }

    private void initView() {
        btnScan = findViewById(R.id.btn_scan);
        mRecyclerView = findViewById(R.id.recycler_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        mainContent = findViewById(R.id.main_content);
        tvStatus = findViewById(R.id.tv_device_status);
        tvName = findViewById(R.id.tv_name);
        tvMac = findViewById(R.id.tv_mac);
        tvVersion = findViewById(R.id.tv_fw_version);
        tvSN = findViewById(R.id.tv_sn);
        tvOtherInfo = findViewById(R.id.tv_other_info);
        btnChooseFile = findViewById(R.id.btn_choose_file);
        tvDfuPath = findViewById(R.id.tv_dfu_path);
        btnStartDfu = findViewById(R.id.btn_start_dfu);
        tvBatt = findViewById(R.id.tv_batt);
        tvBattStatus = findViewById(R.id.tv_batt_status);
        tvRssi = findViewById(R.id.tv_rssi);
        tvAppVersionName = findViewById(R.id.tv_version);
        btnMonitorOn = findViewById(R.id.btn_monitor_on);
        btnSportOn = findViewById(R.id.btn_sport_on);
        btnMonitorOff = findViewById(R.id.btn_monitor_off);
        btnSyncData = findViewById(R.id.btn_sync_data);
        btnLiveOn = findViewById(R.id.btn_live_on);
        btnOpenGlobalLive = findViewById(R.id.btn_open_global_live);
        btnParse = findViewById(R.id.btn_parse);
        btnParseSport = findViewById(R.id.btn_parse_sport);

        tvSpo = findViewById(R.id.tv_spo);
        tvHr = findViewById(R.id.tv_hr);
        tvLiveDesc = findViewById(R.id.tv_live_desc);
        tvSyncProgress = findViewById(R.id.tv_sync_progress);
        tvSmile = findViewById(R.id.tv_smile);
        etToken = findViewById(R.id.et_token);
        tvClear = findViewById(R.id.tv_clear);
        tvV2Live = findViewById(R.id.tv_v2_live);
        tvDfuProgress = findViewById(R.id.tv_dfu_progress);

        try {
            String versionName = getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
            tvAppVersionName.setText("v" + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        setSupportActionBar(toolbar);

        btnScan.setOnClickListener(this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mScannedAdapter = new ScannedAdapter(mScannedDevices);
        mRecyclerView.setAdapter(mScannedAdapter);

        btnChooseFile.setOnClickListener(this);
        btnStartDfu.setOnClickListener(this);
        btnLiveOn.setOnClickListener(this);
        btnMonitorOn.setOnClickListener(this);
        btnSportOn.setOnClickListener(this);
        btnMonitorOff.setOnClickListener(this);
        btnSyncData.setOnClickListener(this);
        tvClear.setOnClickListener(this);
        btnOpenGlobalLive.setOnClickListener(this);
        btnParse.setOnClickListener(this);
        btnParseSport.setOnClickListener(this);

        // get token form shardPreference
        etToken.setText(UtilsSharedPreference.get(this, UtilsSharedPreference.KEY_TOKEN));

        etToken.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (s != null)
                    tvClear.setVisibility(View.VISIBLE);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                if (s == null) tvClear.setVisibility(View.INVISIBLE);
//                else tvClear.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() <= 0)
                    tvClear.setVisibility(View.INVISIBLE);
            }
        });

        findViewById(android.R.id.content).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MainActivity.this.getCurrentFocus() != null) {
                    tvClear.setVisibility(View.INVISIBLE);
                    etToken.clearFocus();
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(MainActivity.this.getCurrentFocus().getWindowToken(), 0);
                }
                return false;
            }
        });
    }

    private void initBle() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() != R.id.btn_choose_file
                && v.getId() != R.id.btn_scan
                && v.getId() != R.id.btn_parse
                && v.getId() != R.id.btn_parse_sport
                && R.id.tv_clear != v.getId()) {
            if (megaBleDevice == null) {
                Toast.makeText(this, "ble offline", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        switch (v.getId()) {
            case R.id.btn_scan:
                if (PermissionChecker.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ACCESS_COARSE_LOCATION);
                    return;
                }

                scanLeDevices();
                break;

            case R.id.btn_choose_file:
                new LFilePicker()
                        .withActivity(this)
                        .withTitle("选择文件")
                        .withRequestCode(REQUESTCODE_CHOOSE_FILE)
                        .withFileFilter(new String[]{".zip"})
                        .withMutilyMode(false)
                        .withNotFoundBooks("未选中")
                        .start();
                break;

            case R.id.btn_start_dfu:
                if (mDfuPath == null) {
                    Toast.makeText(this, "未选择文件", Toast.LENGTH_SHORT).show();
                    return;
                }
                megaBleClient.startDfu(megaBleDevice.getMac());
                break;

            case R.id.btn_open_global_live:
                megaBleClient.toggleLive(true);
                break;

            case R.id.btn_live_on:
                megaBleClient.enableV2ModeLiveSpo(true);
                break;

            case R.id.btn_monitor_off: // 血氧，运动，实时关闭
                megaBleClient.enableV2ModeDaily(true);
                break;

            case R.id.btn_monitor_on: // 开血氧监测
                megaBleClient.enableV2ModeSpoMonitor(true);
                break;
            case R.id.btn_sport_on: // 开运动监测
                megaBleClient.enableV2ModeSport(true);
                break;

            case R.id.btn_sync_data:
                megaBleClient.syncData();
                break;

            case R.id.tv_clear:
                etToken.setText(null);
                UtilsSharedPreference.remove(this, UtilsSharedPreference.KEY_TOKEN);
                break;
            case R.id.btn_parse:
                // mock data; 解析血氧监测数据
                Log.d(TAG, "解析版本号: " + MegaBleConst.VERSION_PARSE_C);
                byte[] bytes = new byte[]{1, 2, 3, 4, 5};
                megaBleClient.parseSpoPr(bytes, new MegaAuth.Callback<ParsedSpoPrBean>() {
                    @Override
                    public void onFail(String s) {
                        Log.d(TAG, s);
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "auth failed", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onSuccess(ParsedSpoPrBean bean) {
                        Log.d(TAG, bean.toString());
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "parse success", Toast.LENGTH_SHORT).show());
                    }
                });
                break;

            case R.id.btn_parse_sport:
                // mock data; 解析运动监测数据
                Log.d(TAG, "解析版本号: " + MegaBleConst.VERSION_PARSE_C);
                byte[] bytes1 = new byte[]{1, 2, 3, 4, 5};
                megaBleClient.parseSport(bytes1, new MegaAuth.Callback<ParsedPrBean>() {
                    @Override
                    public void onFail(String s) {
                        Log.d(TAG, s);
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "auth failed", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onSuccess(ParsedPrBean bean) {
                        Log.d(TAG, bean.toString());
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "parse success", Toast.LENGTH_SHORT).show());
                    }
                });
                break;

            default:
                break;
        }
    }

    @SuppressLint("CheckResult")
    private void scanLeDevices() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "ble not support", Toast.LENGTH_SHORT).show();
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean statusOfGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!statusOfGPS) {
            Toast.makeText(this, "GPS disable, plz enable", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        mScannedDevices.clear();
        mScannedAdapter.notifyDataSetChanged();
        btnScan.setClickable(false);
        Observable.timer(SCAN_PERIOD, TimeUnit.SECONDS).subscribe(aLong -> {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            btnScan.setClickable(true);
        });
        mBluetoothAdapter.startLeScan(mLeScanCallback);

        Observable.interval(1, TimeUnit.SECONDS)
                .take(SCAN_PERIOD)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    Log.d(TAG, aLong.toString() + "scaning...");
                    mScannedAdapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUESTCODE_CHOOSE_FILE) {
                ArrayList<String> list = data.getStringArrayListExtra(Constant.RESULT_INFO);
                tvDfuPath.setText(list.get(0));
                mDfuPath = list.get(0);
            }
        } else if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                scanLeDevices();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mRequestPermissionHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    class ScannedAdapter extends RecyclerView.Adapter<ScannedViewHolder> {
        private List<ScannedDevice> mList;

        public ScannedAdapter(List<ScannedDevice> list) {
            mList = list;
        }

        @Override
        public ScannedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_scan_result, parent, false);
            return new ScannedViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ScannedViewHolder holder, int position) {
            ScannedDevice device = mList.get(position);
            holder.bindView(device);
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }
    }

    class ScannedViewHolder extends RecyclerView.ViewHolder {
        private CardView v;
        private TextView tvName;
        private TextView tvAddress;
        private TextView tvRssi;

        public ScannedViewHolder(View itemView) {
            super(itemView);
            v = (CardView) itemView;
            tvName = v.findViewById(R.id.tv_name);
            tvAddress = v.findViewById(R.id.tv_mac);
            tvRssi = v.findViewById(R.id.tv_rssi);
        }

        public void bindView(ScannedDevice device) {
            tvName.setText(device.getName());
            tvAddress.setText(device.getAddress());
            tvRssi.setText(String.valueOf(device.getRssi()));

            v.setOnClickListener(v -> {
                Toast.makeText(MainActivity.this, "Connecting -> " + device.getName() + " " + device.getAddress(), Toast.LENGTH_SHORT).show();
                megaBleClient.connect(device.getAddress(), device.getName());
            });
        }
    }

    /**
     * set UI
     */
    private void setMegaDeviceInfo(MegaBleDevice device) {
        mainContent.setVisibility(View.VISIBLE);
        tvName.setText(device.getName());
        tvMac.setText(device.getMac());
        tvVersion.setText(device.getFwVer());
        tvSN.setText(device.getSn());
        tvOtherInfo.setText(device.getOtherInfo());
        tvStatus.setText("already connected");
    }

    private void clearMegaDeviceInfo() {
        tvName.setText(null);
        tvMac.setText(null);
        tvVersion.setText(null);
        tvSN.setText(null);
        tvStatus.setText("offline");

        tvOtherInfo.setText(null);
        tvBatt.setText(null);
        tvBattStatus.setText(null);
        tvRssi.setText(null);

        tvSpo.setText(null);
        tvHr.setText(null);
        tvLiveDesc.setText(null);
        tvSyncProgress.setText(null);
        etToken.setText(null);

        megaBleDevice = null;
    }

    private void sendToHandler(MegaBleDevice device) {
        Message msg = Message.obtain();
        msg.what = EVENT_CONNECTED;
        Bundle bundle = new Bundle();
        bundle.putSerializable("device", device);
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    private <T> void updateV2Live(T live) {
        runOnUiThread(() -> {
            tvV2Live.setText(live.toString());
            // 使用AnimationUtils装载动画配置文件
            Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.alpha);
            // 启动动画
            tvSmile.startAnimation(animation);
        });
        megaBleClient.getV2Mode();
    }

    private void checkAppPermission() {
        mRequestPermissionHandler.requestPermission(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS, new RequestPermissionHandler.RequestPermissionListener() {
            @Override
            public void onSuccess() {
//                Toast.makeText(MainActivity.this, "request permission success", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailed() {
                Toast.makeText(MainActivity.this, "request permission failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
