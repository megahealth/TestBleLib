# Mega Ble SDK Document（Android）
name: megablelibopen

- EN | [中文](./SIMPLE_README_ZH.md)

## Files
 - [arr v1.6.28](https://github.com/megahealth/TestBleLib/blob/master/megablelibopen/megablelibopen-1.6.28.aar)
 - [.so v11449](https://github.com/megahealth/TestBleLib/tree/master/app/src/main/jniLibs)
 - [demo v1.0.27](https://github.com/megahealth/TestBleLib)


## Quick start

[SimpleMainActivity.kt](https://github.com/megahealth/TestBleLib/tree/master/app/src/main/java/io/zjw/testblelib/ui/SimpleMainActivity.kt): Please refer to the MainActivity file to understand the following example's code.

### 1. import SDK
Android studio import .arr and .so.

### 2. init MegaBleClient instance

```
megaBleClient = MegaBleBuilder()
    .withSecretId("D4CE5DD515F81247")
    .withSecretKey("uedQ2MgVEFlsGIWSgofHYHNdZSyHmmJ5")
    .withContext(this)
    .uploadData(true)
    .withCallback(megaBleCallback)
    .build()
```

### 3. scan and connect ring

1. Scan the ring.
```
mBluetoothAdapter!!.startLeScan(mLeScanCallback)
```
2. Select a ring from the scan list to connect the ring.The scan information contains the ring's bluetooth address and name.
```
megaBleClient!!.connect(device.address, device.name)
```
3. Use token to connect ring. The token is the credential for connecting to the ring, generated and returned by the ring.
If no token, use startWithoutToken(userId, mac), the user shakes the ring to indicate approval for the connection,
and the ring will generate and return a token. For the next to connect the ring, use this token to maintain the connection without requiring the user to shake the ring.

​		If no token, when the user shakes the ring, new token will be returned:
```
megaBleClient!!.startWithToken(userId, "0,0,0,0,0,0")

override fun onKnockDevice() {
    //Remind the user to shake the ring.
}

override fun onTokenReceived(token: String) {
   //save the token, for the next to connect the ring.
  Log.d(TAG, "onTokenReceived: $token")
}
```

​		If has token:
```
megaBleClient!!.startWithToken(userId, token)

override fun onTokenReceived(token: String) {
  Log.d(TAG, "onTokenReceived: $token")
}
```

4. set user information.
```
megaBleClient!!.setUserInfo(
    25.toByte(),
    1.toByte(),
    170.toByte(),
    60.toByte(),
    0.toByte()
)
```
5. onIdle() is called, which indicates that the connection is completed. 

### 3. set enso mode

```
megaBleClient?.enableEnsoMode(true)
```

### 4. start monitoring

```
client.enableV2ModeSpoMonitor(true);
```

### 5. stop monitoring

```
client.enableV2ModeDaily(true);
```

### 6. sync monitor data

call syncData() to sync data

```
megaBleClient!!.syncData()
```

- onSyncingDataProgress returns the progress of sync data.
- onSyncMonitorDataComplete indicates that the sync data has finished and returns the data.
- onSyncNoDataOfMonitor indicates that the ring has no data, and you can processed with the next operation.

### 7. parse monitor data

```
megaBleClient!!.parseSpoPr(bytes, parseSpoPrResult)
```

### 8. get raw data
call getRawData to get raw data
```
megaBleClient!!.getRawData()
```
- onSyncingDataProgress returns the progress of sync raw data.
- onRawDataComplete indicates that the raw data sync has finished and returns the path.


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
client.enableV2ModeSpoMonitor(true); // Turn on SPO2Monitor(Sleep SPO2Monitor) mode
client.enableV2ModeDaily(true); // Turn off liveSPO2/sport/SPO2Monitor
client.syncData() // Sync monitor data
client.parseSpoPr(bytes, callback) // parse SPO2Monitor data
client.toggleLive(true); // Turn on/off the global real-time channel.Compatible：liveSPO2/sport/SPO2Monitor/pulse
client.getV2Mode(); // Get current mode.
client.getRawData() //Get rawdata
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
void onDeviceInfoReceived(MegaBleDevice device)
void onBatteryChanged(int value, int status)
void onSyncingDataProgress(int progress)
void onSyncMonitorDataComplete(byte[] bytes, int dataStopType, int dataType, String uid, int steps)
void onSyncNoDataOfMonitor()
void onOperationStatus(int status)
void onHeartBeatReceived(MegaBleHeartBeat heartBeat)
void onV2LiveSpoMonitor(MegaV2LiveSpoMonitor live); // SPO2/Sleep live data
void onV2ModeReceived(MegaV2Mode mode) // get current mode
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
|maxDownDuration|The longest Oxygen Desaturation 3 Event|
|wakeMinutes|(minutes)|
|remMinutes|(minutes)|
|lightMinutes|(minutes))|
|deepMinutes|(minutes)|
|wakeInSMinutes|(minutes)|
|fallSMinutes|(minutes)|
|downIndex|Oxygen Desaturation 3 Index|
|downTimes|Oxygen Desaturation Event 3 Count|
|downIndexW|spo2 Desaturation index 3 of whole night|
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
|downTimes4|Oxygen Desaturation Event 4  Count |
|downIndex4|Oxygen Desaturation 4 Index|
|downIndexW4|spo2 Desaturation index 4 of whole night|
|maxDownDuration4|The longest Oxygen Desaturation 4 Event|
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
|accX|acc|
|accY|acc|
|accZ|acc|

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
