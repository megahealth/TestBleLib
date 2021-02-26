# Mega Ble SDK Document（Android）
name: megablelibopen

- EN | [中文](./README_ZH.md)

## Files
 - [arr v1.6.6](https://github.com/megahealth/TestBleLib/blob/master/megablelibopen/megablelibopen-1.6.6.aar)
 - [.so v10120](https://github.com/megahealth/TestBleLib/tree/master/app/src/main/jniLibs)
 - [demo v1.0.15](https://github.com/megahealth/TestBleLib)

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
                .uploadData(true) //enable upload data to optimize algorithm.(default is false)
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
client.syncDailyData() // Sync daily step data
client.getV2PeriodSetting() // Get period monitor setting (MegaBleCallback.onV2PeriodSettingReceived show setting info)
client.enableV2PeriodMonitor(true, boolean isLoop, int monitorDuration, int timeLeft) // open period monitor params：true、isLoop、duration(s)、timeLeft(s)
client.enableV2PeriodMonitor(false, false, 0, 0) // close period monitor
client.parseSpoPr(bytes, callback) // parse SPO2Monitor data
client.parseSport(bytes, callback) // parse Sport data
client.parseSpoPrOld(bytes, callback) // parse SPO2Monitor data(Deprecated, use parseSpoPr())
client.parseSportOld(bytes, callback) // parse Sport data(Deprecated, use parseSport())
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
void onSyncMonitorDataComplete(byte[] bytes)
void onSyncNoDataOfMonitor()
void onOperationStatus(int status)
void onHeartBeatReceived(MegaBleHeartBeat heartBeat)
void onV2LiveSleep(MegaV2LiveSleep live)
void onV2LiveSpoMonitor(MegaV2LiveSpoMonitor live)
void onV2ModeReceived(MegaV2Mode mode)
```

- public class ParsedSpoPrBean（Deprecated, use MegaSpoPrBean）

    Parse SPO2 data：SPO2、PR、sleep area etc.

- public class ParsedPrBean（Deprecated, use MegaPrBean）

    Parse sport data：PR etc.

- native library
  - jniLibs

- dfu（firmware upgrade）dependencies
```
// dfu lib. higher dfu lib may not work, use this one
// href：https://github.com/NordicSemiconductor/Android-DFU-Library
// If you use proguard, add the following line to your proguard rules: -keep class no.nordicsemi.android.dfu.** { *; }
implementation 'no.nordicsemi.android:dfu:1.8.1'
```

- Parse MegaAdvertising
    - MegaAdvParse.parse (MegaRing V3)
    - MegaBleClient.parseScanRecord (MegaRing V2)

- Algorithm version
    - MegaBleClient.megaParseVersion()

## Description
| MegaSpoPrBean |Description|
| :-:|:-:|
|startAt|start time(s)|
|endAt|end time(s)|
|duration|duration(s)|
|maxPr|maxPr(bpm)|
|avgPr|avgPr(bpm)|
|minPr|minPr(bpm)|
|minO2|minO2|
|avgO2|avgO2|
|prArr|Pr array|
|handOffArr|handOff timestamp pair|
|o2Arr|spo2 array(s)。|
|stageArr|sleep stage array：0-w，2-r，3-l，4-d，6-offhand. (Awake, REM, Light, Deep)|
|maxDownDuration|(s)|
|offhandMinutes|(s)|
|wakeMinutes|(s)|
|remMinutes|(s)|
|lightMinutes|(s))|
|deepMinutes|(s)|
|downIndex|spo2 down index|
|downTimes|spo2 down counts|
|secondsUnder60|spo2 <60% seconds|
|secondsUnder70|spo2 <70% seconds|
|secondsUnder80|spo2 <80% seconds|
|secondsUnder85|spo2 <85% seconds|
|secondsUnder90|spo2 <90% seconds|
|secondsUnder95|spo2 <95% seconds|
|shareUnder60|spo2 <60% time percent(%), notice:convert to percent need *100)|
|shareUnder70|spo2 <70% time percent(%), notice:convert to percent need *100|
|shareUnder80|spo2 <80% time percent(%), notice:convert to percent need *100|
|shareUnder85|spo2 <85% time percent(%), notice:convert to percent need *100|
|shareUnder90|spo2 <90% time percent(%), notice:convert to percent need *100|
|shareUnder95|spo2 <95% time percent(%), notice:convert to percent need *100|

|MegaPrBean|Description|
| :-:|:-:|
|startAt|start time(s)|
|endAt|end time(s)|
|duration|duration(s)|
|maxPr|(bpm)|
|avgPr||
|minPr||
|prArr|Pr array(s))|
|handOffArr|handOff timestamp pair|

|MegaV2PeriodSetting|Description|
|:-:|:-:|
|status|period monitor status <0x00-disable，0x01-idle，0x02-working，0x04-suspend>|
|periodType|1-loop;0-once|
|h|hour|
|m|minute|
|s|second|
|maxTime|duration(s)|

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

## Permissions required
bluetooth, write file, internet, GPS
minSdkVersion 19
targetSdkVersion 28
- It is recommended to refer to the demo source code and run the experience

## Wearing Test
 1. Switch to liveSPO2 mode
 2. onV2LiveSpoLive() will return MegaV2LiveSpoLive data
 3. Guide user to pose the specified gestures. If the user wears the ring correctly: accY = 0 when fingers point to the ground; accZ = 0 when Palms up.

# Remarks
- Please view the output information with the android studio console
- Please search for the button name in the demo source code to view the response event. For detailed api, please refer to the online java doc
- The Ring can save data in itself when enabling monitoring. So it is not necessary to keep connection between ring and phone.
After monitoring started, it's ok to disconnect.
- All data will be wiped out if TOKEN is changed.
- Please check the returned fields carefully, If you change to new parse functions.
- Data and Log path--->sdcard/megaBle (client.setDebugEnable(true))
- Developers need to continuously collect 10s-20s real-time values to judge wearing.If the user wears the ring correctly:accY = 0 when fingers point to the ground; accZ = 0 when Palms up.
