# Mega Ble SDK Document（Android）
name: megablelibopen

- EN | [中文](./README_ZH.md)

## Files
 - [arr v1.6.20](https://github.com/megahealth/TestBleLib/blob/master/megablelibopen/megablelibopen-1.6.20.aar)
 - [.so v11449](https://github.com/megahealth/TestBleLib/tree/master/app/src/main/jniLibs)
 - [demo v1.0.22](https://github.com/megahealth/TestBleLib)

## Changelog
|Version|Description|Date|
|:-:|-|:-:|
|1.6.20|Support parse rawdata for pulse|2023/03/15|
|1.6.19|Support rings of C11H, P11G, P11H.|2023/02/27|
|1.6.18|Add an example to show how to draw ECG diagram.|2022/05/23|
|1.6.18|Fix the problem that the firmware cannot be upgraded on Android 9 and above(Please check 'Tips of upgrading firmware')|2022/03/04|
|1.6.18|Fix the problem that can't save ble log on Android Q or above|2022/01/17|
|1.6.17|1.Fix the problem of unresponsive API calls for a short time (in milliseconds)<br/>2.Demo update mock_daily.bin|2021/12/02|
|1.6.16|1.Upgrade parse algorithm(V11449)<br/>2.Support for collecting blood pressure data<br/>3.Add parsing blood pressure data function<br/>4.Support for syncing hrv data<br/>5.Add parsing HRV data function<br/>|2021/11/26|
|1.6.15|1.MegaBleCallback add callback of parsing rawdata<br/>2.README add how to get temperature data|2021/10/26|
|1.6.14|Fix parsing Spo2 events problem<br/>(Please remember update .so libary)|2021/10/18|
|1.6.14|MegaSpoPrBean add Spo2 events array<br/>(Please remember update .so libary)|2021/09/08|
|1.6.13|1.Support for ZG28<br/>2.MegaDailyBean add temperature<br/>3.Upgrade parse algorithm(V11141)|2021/08/24|
|1.6.12|Add get crash log api|2021/06/18|
|1.6.11|1.Upgrade parse algorithm(V10974)<br/>2.MegaSpoPrBean add parsing fields |2021/06/09|


## Quick start
1. Android studio import .arr and .so.
2. Create MegaBleClient by MegaBleBuilder with context, megaBleCallback.
3. Scan for a target by Android standard BLE api.
4. client.connect(target) // client will take care of the interaction with target.
5. Sdk will control BLE state and feedback BLE INFO to user
6. (must)After sdk initialize BLE client：
  1. If no token, use startWithoutToken(userId, mac), to wait for a token which ring will send after shaking.
  2. if there is a token, use startWithToken(userId, token)
7. (must) setUserInfo(...)。The CONNECTION will be stable after this step.
8. Idle. // It's time to control the device, eg. enable/disable monitoring, syncing monitor data
9. Parse data

## API：

- final public class MegaBleBuilder
```
client = new MegaBleBuilder()
                .withSecretId(id)
                .withSecretKey(key)
                .withContext(context)
                .uploadData(true) //enable upload data to optimize algorithm.(default is false), Pls store data by yourself, if disabled.
                .withCallback(megaBleCallback)
                .build();
```

- public class MegaBleClient
```
client.toggleLive(true); // Turn on/off the global real-time channel.Compatible：liveSPO2/sport/SPO2Monitor/pulse
client.getV2Mode(); // Get current mode.
client.enableV2ModeLiveSpo(true); // Turn on liveSPO2 mode
client.enableV2ModeDaily(true); // Turn off liveSPO2/sport/SPO2Monitor
client.enableV2ModeSpoMonitor(true); // Turn on SPO2Monitor(Sleep SPO2Monitor) mode
client.enableV2ModeSport(true); // Turn on sport mode
client.enableV2ModePulse(true); // Turn on pulse mode
client.enableRawdataSpo // Turn on SPO2 rawdata(need turn on liveSPO2/sport/SPO2Monitor/pulse)
client.enableRawdataPulse // Turn on pulse rawdata(need turn on pulse mode)
client.disableRawdata // Turn off rawdata
client.syncData() // Sync monitor data
client.syncDailyData() // Sync daily step data
client.syncHrvData() // Sync HRV data
client.getV2PeriodSetting() // Get period monitor setting (MegaBleCallback.onV2PeriodSettingReceived show setting info)
client.enableV2PeriodMonitor(true, boolean isLoop, int monitorDuration, int timeLeft) // open period monitor params：true、isLoop、duration(s)、timeLeft(s)
client.enableV2PeriodMonitor(false, false, 0, 0) // close period monitor
client.parseSpoPr(bytes, callback) // parse SPO2Monitor data
client.parseSport(bytes, callback) // parse Sport data
client.parseSpoPrOld(bytes, callback) // parse SPO2Monitor data(Deprecated, use parseSpoPr())
client.parseSportOld(bytes, callback) // parse Sport data(Deprecated, use parseSport())
client.startDfu() // enter to dfu mode to upgrade firmware.
client.getCrashLog() // get crash log, recommend to get crash log after sync data.
client.parseDailyEntry(bytes) //pasre daily data
client.enableV2ModeEcgBp(true/false, megaRawdataConfig) // turn on or turn off blood pressure
client.parseBpData(bytes, timeHHmm, caliSBP, caliDBP) // parse blood pressure data. Params example:bytes=[], timeHHmm= 831, caliSBP=134.0, caliDBP=80.0
client.parseHrvData(bytes, callback) //parse HRV data
```

- public abstract class MegaBleCallback
```
void onConnectionStateChange(boolean connected);
void onError(int code)
void onStart() // call startWithoutToken/startWithToken
void onSetUserInfo() // call setUserInfo
void onIdle() // ready to do work
void onKnockDevice() // only happened when there is no token or token changed
void onTokenReceived(String token) // user should save
void onRssiReceived(int rssi)
void onDeviceInfoReceived(MegaBleDevice device)
void onBatteryChanged(int value, int status)
void onReadyToDfu()
void onSyncingDataProgress(int progress)
void onSyncMonitorDataComplete(byte[] bytes, int dataStopType, int dataType, String uid, int steps)
void onSyncDailyDataComplete(byte[] bytes)
void onSyncNoDataOfMonitor()
void onSyncNoDataOfDaily()
void onSyncNoDataOfHrv()
void onOperationStatus(int status)
void onHeartBeatReceived(MegaBleHeartBeat heartBeat)
void onV2LiveSpoMonitor(MegaV2LiveSpoMonitor live); // SPO2/Sleep live data
void onV2LiveSport(MegaV2LiveSport live); // Sport live data
void onV2LiveSpoLive(MegaV2LiveSpoLive live); // Live SPO2 data
void onV2ModeReceived(MegaV2Mode mode) // get current mode
void onV2PeriodSettingReceived(setting: MegaV2PeriodSetting) // get current periodic monitor setting
void onCrashLogReceived(bytes: ByteArray?)// return crash log
//Parse rawdata
void onRawdataParsed(MegaRawData[]);
void onRawdataParsed([]);//Deprecated
void onTotalBpDataReceived(data, duration) // return total blood pressure data and duration
```

- public class ParsedSpoPrBean（Deprecated, use MegaSpoPrBean）

    Parse SPO2 data：SPO2、PR、sleep area etc.

- public class ParsedPrBean（Deprecated, use MegaPrBean）

    Parse sport data：PR etc.

- public class MegaDailyParsedResult

    Parse daily data

- public class MegaDailyBean

    daily data detail

- public class ParsedBPBean

   blood pressure data detail

- public class ParsedHRVBean

   HRV data detail

- native library
  - jniLibs

- dfu（firmware upgrade）dependencies
```
// dfu lib. higher dfu lib may not work, use this one
// href：https://github.com/NordicSemiconductor/Android-DFU-Library
// If you use proguard, add the following line to your proguard rules: -keep class no.nordicsemi.android.dfu.** { *; }
TargetSdk < 31
implementation 'no.nordicsemi.android:dfu:1.8.1'
TargetSdk >= 31
implementation 'no.nordicsemi.android:dfu:2.0.2'
```

- Parse MegaAdvertising
    - MegaAdvParse.parse (MegaRing V3)
    - MegaBleClient.parseScanRecord (MegaRing V2)

- Algorithm version
    - MegaBleClient.megaParseVersion()

## Description
| MegaSpoPrBean |Description|
| :-:|:-:|
|startAt|start time(timestamp)|
|endAt|end time(timestamp)|
|startPos|SPO2/PR offset of start|
|endPos|SPO2/PR offset of end|
|duration|duration(second)|
|maxPr|maxPr(bpm)|
|avgPr|avgPr(bpm)|
|minPr|minPr(bpm)|
|minO2|minO2|
|avgO2|avgO2|
|prArr|Pr array|
|handOffArr|handOff timestamp pair|
|o2Arr|spo2 array(second)|
|stageArr|sleep stage array：0-w，2-r，3-l，4-d，6-offhand. (Awake, REM, Light, Deep)|
|maxDownDuration|(second)|
|wakeMinutes|(minutes)|
|remMinutes|(minutes)|
|lightMinutes|(minutes))|
|deepMinutes|(minutes)|
|wakeInSMinutes|(minutes)|
|fallSMinutes|(minutes)|
|downIndex|spo2 down index|
|downTimes|spo2 down counts|
|downIndexW|spo2 down index of whole night|
|secondsUnder60|spo2 <60% seconds|
|secondsUnder65|spo2 <65% seconds|
|secondsUnder70|spo2 <70% seconds|
|secondsUnder75|spo2 <75% seconds|
|secondsUnder80|spo2 <80% seconds|
|secondsUnder85|spo2 <85% seconds|
|secondsUnder90|spo2 <90% seconds|
|secondsUnder95|spo2 <95% seconds|
|secondsUnder100|spo2 <100% seconds|
|shareUnder60|spo2 <60% time percent(%), notice:convert to percent need *100)|
|shareUnder65|spo2 <65% time percent(%), notice:convert to percent need *100)|
|shareUnder70|spo2 <70% time percent(%), notice:convert to percent need *100|
|shareUnder75|spo2 <75% time percent(%), notice:convert to percent need *100)|
|shareUnder80|spo2 <80% time percent(%), notice:convert to percent need *100|
|shareUnder85|spo2 <85% time percent(%), notice:convert to percent need *100|
|shareUnder90|spo2 <90% time percent(%), notice:convert to percent need *100|
|shareUnder95|spo2 <95% time percent(%), notice:convert to percent need *100|
|shareUnder100|spo2 <100% time percent(%), notice:convert to percent need *100)|
|ODI3Less100Cnt||
|ODI3Less95Cnt||
|ODI3Less90Cnt||
|ODI3Less85Cnt||
|ODI3Less80Cnt||
|ODI3Less75Cnt||
|ODI3Less70Cnt||
|ODI3Less65Cnt||
|ODI3Less60Cnt||
|ODI3Less100Percent||
|ODI3Less95Percent||
|ODI3Less90Percent||
|ODI3Less85Percent||
|ODI3Less80Percent||
|ODI3Less75Percent||
|ODI3Less70Percent||
|ODI3Less65Percent||
|ODI3Less60Percent||
|ODI3Less10sCnt||
|ODI3Less20sCnt||
|ODI3Less30sCnt||
|ODI3Less40sCnt||
|ODI3Less50sCnt||
|ODI3Less60sCnt||
|ODI3Longer60sCnt||
|ODI3Less10sPercent||
|ODI3Less20sPercent||
|ODI3Less30sPercent||
|ODI3Less40sPercent||
|ODI3Less50sPercent||
|ODI3Less60sPercent||
|ODI3Longer60sPercent||
|downTimes4||
|downIndex4||
|downIndexW4||
|maxDownDuration4||
|ODI4Less100Cnt||
|ODI4Less95Cnt||
|ODI4Less90Cnt||
|ODI4Less85Cnt||
|ODI4Less80Cnt||
|ODI4Less75Cnt||
|ODI4Less70Cnt||
|ODI4Less65Cnt||
|ODI4Less60Cnt||
|ODI4Less100Percent||
|ODI4Less95Percent||
|ODI4Less90Percent||
|ODI4Less85Percent||
|ODI4Less80Percent||
|ODI4Less75Percent||
|ODI4Less70Percent||
|ODI4Less65Percent||
|ODI4Less60Percent||
|ODI4Less10sCnt||
|secondsUnder100||
|ODI4Less20sCnt||
|ODI4Less30sCnt||
|ODI4Less40sCnt||
|ODI4Less50sCnt||
|ODI4Less60sCnt||
|ODI4Longer60sCnt||
|ODI4Less10sPercent||
|ODI4Less20sPercent||
|ODI4Less30sPercent||
|ODI4Less40sPercent||
|ODI4Less50sPercent||
|ODI4Less60sPercent||
|ODI4Longer60sPercent||
|Spo2EvtVect3|Spo2 events array, data appears in pairs. [timestamp, duration(s), timestamp, duration(s), ...] ||
|Spo2EvtVect4|Spo2 events array, data appears in pairs. [timestamp, duration(s), timestamp, duration(s), ...] ||

|MegaPrBean|Description|
| :-:|:-:|
|startAt|start time(timestamp)|
|endAt|end time(timestamp)|
|duration|duration(second)|
|maxPr|(bpm)|
|avgPr||
|minPr||
|prArr|Pr array(second))|
|handOffArr|handOff timestamp pair|

|MegaV2PeriodSetting|Description|
|:-:|:-:|
|status|period monitor status <0x00-disable，0x01-idle，0x02-working，0x04-suspend>|
|periodType|1-loop;0-once|
|h|hour|
|m|minute|
|s|second|
|maxTime|duration(second)|

|MegaV2LiveSpoLive|Description|
|:-:|:-:|
|status|  0--->valid value <br>1--->preparing <br>2--->invalid value|
|hr|heart rate(bpm)|
|spo2|spo2(%)|
|accX|acc|
|accY|acc|
|accZ|acc|

|MegaV2LiveSpoMonitor|Description|
|:-:|:-:|
|status|  0--->valid value <br>1--->preparing <br>2--->invalid value|
|hr|heart rate(bpm)|
|spo2|spo2(%)|

|MegaBleBattery|Description|
|:-:|:-:|
|normal|(0, "normal")|
|charging|(1, "charging")|
|full|full(2, "full")|
|lowPower|(3, "lowPower")|

|MegaBleHeartBeat|Description|
|:-:|:-:|
|version|version|
|battPercent|battery value(%)|
|deviceStatus||
|mode|working mode|
|recordStatus|is/not recording data|

|MegaBleDevice|Description|
|:-:|:-:|
|name|device name|
|mac|mac|
|sn|sn|
|hwVer|hardware version|
|fwVer|firmware version|
|blVer|BootLoader version|

|MegaDailyParsedResult|Result of parse daily data |
|:-------------------:|:-------------------------------------------------------:|
|dailyUnit|For calculate start of MegaDailyBean(Unit is minutes) |
|dailyBeans|list of MegaDailyBean|

|MegaDailyBean|Details of daily data |
|:-----------:|:------------------------------------------:|
|time|end of MegaDailyBean(The unit is timestamps) |
|stepsDiff|steps in the time period|
|temp|temperature in the time period|

|MegaRawData|Details of RawData |
|:-----------:|:------------------------------------------:|
|red|Red light|
|infrared|Infrared light|
|ambient|Ambient light|

|ParsedBPBean|Details of Blood Pressure|
|:-:|:-:|
|dataType|tpye of data|
|protocol|protocol of data|
|frameCount|count of frame|
|dataBlockSize|dataBlockSize|
|SBP|Systolic Blood Pressure|
|DBP|Diastolic Blood Pressure|
|pr|Pulse Rate|
|status|state of data(0:valid 1:ECG data negative saturation)|
|flag|result of calculate(0:invalid 1:valid 2:timeout)|
|chEcg|data of ECG|
|dataNum|length of ECG data|

|ParsedHRVBean|Details of HRV|
|:-:|:-:|
|version|version of data|
|dataType|type of data|
|timeStart|start time(timestamp)|
|duration|duration(second)|
|cnt|number of heart beats analyzed|
|meanBpm|average of HR|
|SDNN||
|SDANN||
|RMSSD||
|NN50||
|pNN50||
|triangleIdx|triangle index|
|maxRR|maximum RR interval|
|maxRRTimeStamp|occurrence time of maxRR(timestamp)|
|minBpm|minimum HR|
|minBpmTimeStamp|occurrence time of minBpm|
|maxBpm|maximum HR|
|maxBpmTimeStamp|occurrence time of maxBpm|
|fastBpmCnt|tachycardia wave number|
|fastBpmRate|proportion of tachycardia|
|slowBpmCnt|bradycardia number|
|slowBpmRate|proportion of bradycardia|
|VLFP|proportion(%)|
|LFP|proportion(%)|
|HFP|proportion(%)|
|LHFR||
|SDNNCnt|count of SDNN|
|SDNNVect|SDNN array|
|HRcnt|count of HR|
|HRVect|HR array|
|histVCnt|length of Histogram data|
|histVect|histogram data|
|freqVCnt|length of Spectrogram data|
|freqVect|spectrogram data|
|timeT||
|timeCnt||
|SD1||
|SD2||
|SDRate||

|MegaRawdataConfig|Details of rawdata config|
|:-:|:-:|
|isFileEnable|Is save data to file|
|filename|file name of saving data|
|isServerEnable|Is send data to tcp server|
|ip|tcp ip(default is null)|
|port|tcp port(default is 0)|

|Operation Code|Description|
|:-:|:-:|
|0x00|CMD_SUCCESS|
|0x02|STATUS_NO_DATA|
|0x20|SLEEPID_ERR|
|0x21|CMD_PARAM_CANNOT_RESOLVE|
|0x23|RECORDS_CTRL_ERR|
|0x24|AFE44XX_IS_MONITORING|
|0x25|AFE44XX_IS_SPORTING|
|0x9F|UNKNOWN_CMD|
|0xA1|BATTERY_IS_LOW|
|0xA3|FLASH_ERR|
|0xA4|OPERA_DISALLOWG|
|0xA5|AFE44XX_FAULTG|
|0xA6|GSENSOR_FAULT|
|0xA7|BQ25120_IS_FAULT|
|0xC1|RECORDS_NO_STOP|
|0xFF|DEVICE_UNKNOWN_ERR|

## Recommend workflow
[Workflow pdf](https://file-mhn.megahealth.cn/h565KP9NTOonfpcETStB1nmKzlFgXiKS/workflow.pdf)

## Permissions required
bluetooth, write file, internet, GPS
minSdkVersion 19
targetSdkVersion 28
- It is recommended to refer to the demo source code and run the experience

## Wearing Test
    1. Switch to liveSPO2 mode
    2. onV2LiveSpoLive() will return MegaV2LiveSpoLive data
    3. Guide user to pose the specified gestures. If the user wears the ring correctly: accY = 0 when fingers point to the ground; accZ = 0 when Palms up.

## Tips of calculate daily data
    1.Start of MegaDailyBean need calculate by yourself. start = MegaDailyBean.time-dailyUnit*60
    2.The unit of temp  is ℃ ,temp/10  to get  temperature .
    3.Please control the timing of sync daily data by yourself.
## How to get temperature in monitoring
    1.Stop monitoring
    2.Sync daily data
    3.Parse daily data
    4.Sync monitor data
    5.Parse monitor data and combine with filter daily data that synced by step 2
    (Daily data's start and end must between in monitor data's startAt and endAt.The developers can store timestamps and temperature by yourself)
## How to get blood pressure data
    1.Implement MegaBleCallback.onTotalBpDataReceived() //This function will return bp data and test duration.
    2.client.enableV2ModeEcgBp(true, megaRawdataConfig) //start blood pressure test
    3.Use data from onTotalBpDataReceived() and call client.parseBpData() to get blood pressure data
    4.client.enableV2ModeEcgBp(false, megaRawdataConfig) //stop blood pressure if ParsedBPBean.flag = 1 or test duration greater than 60s
    (Tips:Please tell user to set caliSBP and caliDBP before blood pressure test.caliSBP is user's history of Systolic Blood Pressure, caliDBP is user's history of Diastolic Blood Pressure.)

## How to get HRV data
    1.Implement MegaBleCallback.onSyncNoDataOfHrv()// Sync HRV data done.
    2.Call client.syncHrvData() to sync HRV data.
    3.Use data from onSyncMonitorDataComplete() and call client.parseHrvData() to get HRV data after HRV data synced.
    (Tips:HRV data is based on SPO2Monitor(Sleep SPO2Monitor).You can sync hrv data when monitor data synced.HRV data's type is 10.MegaBleCallback.onSyncMonitorDataComplete() will return hrv data)

## Tips of upgrading firmware
    1.Battery value should be greater than 25%
    2.Battery status should be normal/charging/full
    3.Add FOREGROUND_SERVICE permission(android.permission.FOREGROUND_SERVICE) to AndroidManifest.xml if targetSdkVersion >= 28

# Remarks
- Please view the output information with the android studio console
- Please search for the button name in the demo source code to view the response event. For detailed api, please refer to the online java doc
- The Ring can save data in itself when enabling monitoring. So it is not necessary to keep connection between ring and phone.
After monitoring started, it's ok to disconnect.
- All data will be wiped out if TOKEN is changed.
- Please check the returned fields carefully, If you change to new parse functions.
- Data and Log path (client.setDebugEnable(true)). Recommend always open both develop environment and official environment.
    * version < 1.6.18：<br/>log--->sdcard/megaBle/log<br/>data--->sdcard/megaBle/data
    * version >= 1.6.18：<br/>log--->Android/data/{applicationId}/files/megaBle/log<br/>data--->Android/data/{applicationId}/files/megaBle/data
- Developers need to continuously collect 10s-20s real-time values to judge wearing.If the user wears the ring correctly:accY = 0 when fingers point to the ground; accZ = 0 when Palms up.
- Blood Pressure and HRV is only support for ring's sn start with C11E[7|8|9].Please control the calling time by yourself
