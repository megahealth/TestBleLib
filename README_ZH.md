# Mega Ble SDK 库使用文档（原生Android端）
库名: megablelibopen

- [EN](./README.md) | 中文

## sdk文件
 - [arr库 v1.6.15](https://github.com/megahealth/TestBleLib/blob/master/megablelibopen/megablelibopen-1.6.15.aar)
 - [.so库 v11141](https://github.com/megahealth/TestBleLib/tree/master/app/src/main/jniLibs)
 - [demo v1.0.19](https://github.com/megahealth/TestBleLib)

建议克隆demo后，arr库和.so库从demo中取出使用

## 更新日志
|版本|说明|时间|
|:-:|-|:-:|
|1.6.15|1.MegaBleCallback增加解析rawdata的回调<br/>2.README新增如何获取温度的说明<br/>(该版本不牵扯算法更新，未使用请忽略该版本)|2021/10/26|
|1.6.14|修复SPO2呼吸事件解析出现异常的问题<br/>(请更新.so库，如果未使用该字段请忽略该版本)|2021/10/18|
|1.6.14|MegaSpoPrBean增加SPO2呼吸事件数据字段<br/>(请更新.so库，如果未使用该字段请忽略该版本)|2021/09/08|
|1.6.13|1.支持ZG28<br/>2.MegaDailyBean增加温度字段(temp)<br/>(如果不是ZG28指环可忽略该字段)<br/>3.更新后处理算法(V11141)|2021/08/24|
|1.6.12|增加获取crash log的API|2021/06/18|
|1.6.11|1.更新后处理算法(V10974)<br/>2.MegaSpoPrBean新增解析字段 |2021/06/09|

## 作用
提供与兆观公司智能指环蓝牙交互的功能

## 主要功能
### 1. 血氧长时监测模式（睡眠监测）
实时输出，同时戒指自身存储。方便手机与戒指断开，待监测结束后，异步收取监测数据

数据内容：血氧(SpO2)，心率(pr)，睡眠分期

### 2. 运动监测 (心率)
实时输出，同时戒指自身存储。方便手机与戒指断开，待监测结束后，异步收取监测数据

数据内容：心率(pr)

### 3. 血氧实时模式
实时输出，戒指自身不存储

### 4. 脉诊模式
实时+脉诊rawdata

### 5. 收取日常计步数据

## 推荐工作流程
[工作流程图pdf](https://file-mhn.megahealth.cn/62630b5d10f14ecce727/App%E4%B8%8E%E6%88%92%E6%8C%87%E4%BA%A4%E4%BA%92%E6%B5%81%E7%A8%8B%E5%9B%BE.pdf)


## Quick start
请向官方提供包名，来获取有效id和key
1. android studio项目引入此库
2. 通过MegaBleBuilder构造客户端对象MegaBleClient，并传入MegaBleCallback实例，作为项目和库通信接口。
3. 开发者自己负责扫描蓝牙设备(调用android系统蓝牙扫描方法)。若有必要解析广播，请调用适当的广播解析方法。
4. 扫描到想要连接的设备(名字含有MegaRing的蓝牙外设)即可停止扫描，调用库方法connect(...)进行连接
5. 由库接管连接后的蓝牙状态，并同时向用户反馈必要的蓝牙信息
6. (强制)在库初始化完蓝牙后：
  1. 非绑定设备状态下需要传入用户的userId 和 mac，等待库返回token
  2. 已绑定设备状态下需要传入用户的userId 和 token
方法为：startWithoutToken，startWithToken
7. (强制)设置用户信息：setUserInfo(...)。完成此步后，蓝牙设备和用户进入已绑定状态
8. Idle状态。用户可以开始进行操作，如：收数据、开监测(建议在此先获取下设备当前的模式信息)
9. 解析数据(需要强制联网验证)，可以输出类似兆观健康应用中的报告统计信息

## 主要API：

- final public class MegaBleBuilder
```
client = new MegaBleBuilder()
                .withSecretId(id)
                .withSecretKey(key)
                .withContext(context)
                .uploadData(true) // 上传数据帮助我们优化解析算法.(默认不上传)
                .withCallback(megaBleCallback)
                .build();
```

- public class MegaBleClient
```
client.toggleLive(true); // 开/关全局实时通道。兼容：血氧长时、运动、血氧实时
client.getV2Mode(); // 获取设备当前所处的模式
client.enableV2ModeLiveSpo(true); // 打开血氧实时模式
client.enableV2ModeDaily(true); // 关闭所有模式
client.enableV2ModeSpoMonitor(true); // 打开血氧长时模式 (睡眠血氧监测)
client.enableV2ModeSport(true); // 打开运动模式
client.enableV2ModePulse(true); // 打开脉诊模式
client.enableRawdataSpo // 打开血氧rawdata，需要打开血氧相关模式
client.enableRawdataPulse // 打开脉诊rawdata，需要打开脉诊模式
client.disableRawdata // 关闭所有rawdata
client.syncData() // 同步监测数据
client.syncDailyData() // 同步日常计步数据
client.getV2PeriodSetting() // 获取定时监测的设置信息 (MegaBleCallback.onV2PeriodSettingReceived返回设置信息)
client.enableV2PeriodMonitor(true, boolean isLoop, int monitorDuration, int timeLeft) // 打开定时监测 参数释义：true、是否重复、监测时长(s)、距离监测开启的时长(s)
client.enableV2PeriodMonitor(false, false, 0, 0) // 关闭定时监测
client.parseSpoPr(bytes, callback) // 解析血氧数据
client.parseSport(bytes, callback) // 解析运动数据
client.parseSpoPrOld(bytes, callback) // 解析血氧数据(已弃用，请使用parseSpoPr方法)
client.parseSportOld(bytes, callback) // 解析血氧数据(已弃用，请使用parseSport方法)
client.startDfu() // 进入DFU模式，onReadyToDfu()表示已进入升级模式，可向戒指发送升级包
client.getCrashLog() //获取crash log, 推荐在监测数据收取完成以后获取crash log信息.
client.parseDailyEntry(bytes) //解析日常数据
```

- public abstract class MegaBleCallback // 指环操作回调
```
void onConnectionStateChange(boolean connected); // 蓝牙连接状态变化
void onError(int code); //操作异常
void onStart(); // 开始绑定指环
void onSetUserInfo(); // 发送用户信息
void onIdle(); // 绑定完成，指环空闲
void onKnockDevice(); // 没有token或者token改变时会走这个回调，需提示用户晃动指环
void onTokenReceived(String token); // 返回当前的token，请妥善保存
void onRssiReceived(int rssi); //获取指环的信号
void onDeviceInfoReceived(MegaBleDevice device); //返回指环的信息
void onBatteryChanged(int value, int status); // 电量变换
void onReadyToDfu(); // 指环已进入DFU模式
void onSyncingDataProgress(int progress); // 数据收取进度
void onSyncMonitorDataComplete(byte[] bytes, int dataStopType, int dataType, String uid, int steps); // 监测数据收取完成
void onSyncDailyDataComplete(byte[] bytes); // 日常数据收取成功
void onSyncNoDataOfMonitor(); // 暂无监测数据/监测数据收取完成
void onSyncNoDataOfDaily(); // 暂无日常数据/日常数据收取完成
void onOperationStatus(int status); // 指令发送结果
void onHeartBeatReceived(MegaBleHeartBeat heartBeat); // 心跳包获取结果
void onV2LiveSpoMonitor(MegaV2LiveSpoMonitor live); // 睡眠监测实时值
void onV2LiveSport(MegaV2LiveSport live); // 运动监测实时值
void onV2LiveSpoLive(MegaV2LiveSpoLive live); // 实时血氧监测值
void onV2ModeReceived(MegaV2Mode mode); // 返回当前模式
void onV2PeriodSettingReceived(setting: MegaV2PeriodSetting) // 返回当前定时信息
void onCrashLogReceived(bytes: ByteArray?)//返回crash log
// rawdata数据解析
void onRawdataParsed(MegaRawData[]);
void onRawdataParsed([]);//已弃用
```

- public class ParsedSpoPrBean（已废弃，替换为MegaSpoPrBean）

    解析血氧数据：血氧、脉率、睡眠分期；其他统计信息

- public class ParsedPrBean（已废弃，替换为MegaPrBean）

    解析运动数据：脉率；其他统计信息

- public class MegaDailyParsedResult

   解析日常数据

- public class MegaDailyBean

   日常数据详情

- native库 // 数据解析相关
  - jniLibs

- dfu（戒指固件升级）依赖
```
// dfu lib. higher dfu lib may not work, use this one
// 官网地址：https://github.com/NordicSemiconductor/Android-DFU-Library
// If you use proguard, add the following line to your proguard rules: -keep class no.nordicsemi.android.dfu.** { *; }
implementation 'no.nordicsemi.android:dfu:1.8.1'
```

- 广播解析
    - MegaAdvParse.parse (三代戒指)
    - MegaBleClient.parseScanRecord (二代戒指)

- 获取算法解析版本
    - MegaBleClient.megaParseVersion()

## 字段说明
| MegaSpoPrBean |说明|
| :-:|:-:|
|startAt|开始时间戳(s)|
|endAt|结束时间戳(s)|
|startPos|氧、心率开始（偏移）|
|endPos|血氧、心率结束（偏移）|
|duration|监测时长(s)|
|maxPr|最大脉率(bpm)|
|avgPr|平均脉率(bpm)|
|minPr|最小脉率(bpm)|
|minO2|最小血氧|
|avgO2|平均血氧|
|prArr|解析后得到的心率数组。间隔时间(s))|
|handOffArr|离手的时间戳(成对)，方便显示用户何时离手|
|o2Arr|解析后得到的血氧数组，连续的，间隔时间(s)。|
|stageArr|睡眠分期数组 0:清醒 2:眼动 3:浅睡 4:深睡 6:离手/无效|
|maxDownDuration|最长氧减时间(s)|
|wakeMinutes|清醒期(minutes)|
|remMinutes|眼动期(minutes)|
|lightMinutes|浅睡期(minutes)|
|deepMinutes|深睡期(minutes)|
|wakeInSMinutes|入睡后觉醒时长(minutes)|
|fallSMinutes|入睡等待时长(minutes)|
|downIndex|氧减指数|
|downTimes|氧减次数|
|downIndexW|氧减指数 对整晚数据，不做分期的统计|
|secondsUnder60|血氧饱和度 <60% 的时间(s)|
|secondsUnder65|血氧饱和度 <65% 的时间(s)|
|secondsUnder70|血氧饱和度 <70% 的时间(s)|
|secondsUnder75|血氧饱和度 <75% 的时间(s)|
|secondsUnder80|血氧饱和度 <80% 的时间(s)|
|secondsUnder85|血氧饱和度 <85% 的时间(s)|
|secondsUnder90|血氧饱和度 <90% 的时间(s)|
|secondsUnder95|血氧饱和度 <95% 的时间(s)|
|secondsUnder100|血氧饱和度 <100% 的时间(s)|
|shareUnder60|血氧饱和度 <60% 的时间占比(*100转换为%)|
|shareUnder65|血氧饱和度 <65% 的时间占比(*100转换为%)|
|shareUnder70|血氧饱和度 <70% 的时间占比(*100转换为%)|
|shareUnder75|血氧饱和度 <75% 的时间占比(*100转换为%)|
|shareUnder80|血氧饱和度 <80% 的时间占比(*100转换为%)|
|shareUnder85|血氧饱和度 <85% 的时间占比(*100转换为%)|
|shareUnder90|血氧饱和度 <90% 的时间占比(*100转换为%)|
|shareUnder95|血氧饱和度 <95% 的时间占比(*100转换为%)|
|shareUnder100|血氧饱和度 <100% 的时间占比(*100转换为%)|
|ODI3Less100Cnt|血氧低于100高于95的事件个数|
|ODI3Less95Cnt|血氧低于95高于90的事件个数|
|ODI3Less90Cnt|血氧低于90高于85的事件个数|
|ODI3Less85Cnt|血氧低于85高于80的事件个数|
|ODI3Less80Cnt|血氧低于80高于75的事件个数|
|ODI3Less75Cnt|血氧低于75高于70的事件个数|
|ODI3Less70Cnt|血氧低于70高于65的事件个数|
|ODI3Less65Cnt|血氧低于65高于60的事件个数|
|ODI3Less60Cnt|血氧低于60的事件个数|
|ODI3Less100Percent|血氧低于100高于95的事件占比|
|ODI3Less95Percent|血氧低于95高于90的事件占比|
|ODI3Less90Percent|血氧低于90高于85的事件占比|
|ODI3Less85Percent|血氧低于85高于80的事件占比|
|ODI3Less80Percent|血氧低于80高于75的事件占比|
|ODI3Less75Percent|血氧低于75高于70的事件占比|
|ODI3Less70Percent|血氧低于70高于65的事件占比|
|ODI3Less65Percent|血氧低于65高于60的事件占比|
|ODI3Less60Percent|血氧低于60的事件占比|
|ODI3Less10sCnt|时间少于10秒的事件个数|
|ODI3Less20sCnt|时间少于20秒大于10秒的事件个数|
|ODI3Less30sCnt|时间少于30秒大于20秒的事件个数|
|ODI3Less40sCnt|时间少于40秒大于30秒的事件个数|
|ODI3Less50sCnt|时间少于50秒大于40秒的事件个数|
|ODI3Less60sCnt|时间少于60秒大于50秒的事件个数|
|ODI3Longer60sCnt|时间大于60秒的事件个数|
|ODI3Less10sPercent|时间少于10秒的事件占比|
|ODI3Less20sPercent|时间少于20秒大于10秒的事件占比|
|ODI3Less30sPercent|时间少于30秒大于20秒的事件占比|
|ODI3Less40sPercent|时间少于40秒大于30秒的事件占比|
|ODI3Less50sPercent|时间少于50秒大于40秒的事件占比|
|ODI3Less60sPercent|时间少于60秒大于50秒的事件占比|
|ODI3Longer60sPercent|时间大于60秒的事件占比|
|downTimes4|氧减次数|
|downIndex4|氧减指数|
|downIndexW4|氧减指数 对整晚数据，不做分期的统计|
|maxDownDuration4|最长氧减时间|
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
|Spo2EvtVect3|Spo2呼吸事件数据, 数据成对出现. [时间戳, 时长(s), 时间戳, 时长(s), ...] ||
|Spo2EvtVect4|Spo2呼吸事件数据, 数据成对出现. [时间戳, 时长(s), 时间戳, 时长(s), ...] ||

|MegaPrBean|说明|
| :-:|:-:|
|startAt|开始时间戳(s)|
|endAt|结束时间戳(s)|
|duration|监测时长(s)|
|maxPr|最大脉率(bpm)|
|avgPr|平均脉率(bpm)|
|minPr|最小脉率(bpm)|
|prArr|解析后得到的心率数组。间隔时间(s))|
|handOffArr|离手的时间戳(成对)，方便显示用户何时离手|

|MegaV2PeriodSetting|说明|
|:-:|:-:|
|status|定时器监控状态 <0x00-无效，0x01-空闲，0x02-运行，0x04-挂起>|
|periodType|1-周期定时;0-单次定时|
|h|小时|
|m|分钟|
|s|秒|
|maxTime|监测时长(s)|

|MegaV2LiveSpoLive|说明|
|:-:|:-:|
|status|  0--->值有效 <br>1--->准备中 <br>2--->值无效|
|hr|心率|
|spo2|血氧(%)|
|accX|acc|
|accY|acc|
|accZ|acc|

|MegaV2LiveSpoMonitor|说明|
|:-:|:-:|
|status|  0--->值有效 <br>1--->准备中 <br>2--->值无效|
|hr|心率|
|spo2|血氧(%)|

|MegaBleBattery|说明|
|:-:|:-:|
|normal|(0, "normal")|
|charging|(1, "charging")|
|full|full(2, "full")|
|lowPower|(3, "lowPower")|

|MegaBleHeartBeat|说明|
|:-:|:-:|
|version|version|
|battPercent|电量(%)|
|deviceStatus||
|mode|工作模式|
|recordStatus|是否正在记录数据|

|MegaBleDevice|说明|
|:-:|:-:|
|name|设备名称|
|mac|mac|
|sn|sn|
|hwVer|硬件版本|
|fwVer|固件版本|
|blVer|BootLoader版本|

|MegaDailyParsedResult|解析返回的日常数据集合 |
|:-------------------:|:-------------------------------------------------------:|
|dailyUnit|计算每一条日常数据的开始时间(单位是分钟) |
|dailyBeans|日常数据集合|

|MegaDailyBean|日常数据详情信息 |
|:-----------:|:------------------------------------------:|
|time|结束时间(单位是时间戳) |
|stepsDiff|时间段内步数|
|temp|时间段内温度|

|MegaRawData|RawData解析结果 |
|:-----------:|:------------------------------------------:|
|red|红光|
|infrared|红外|
|ambient|环境光|

操作返回码

|返回码|说明|
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

## 完整Java Doc
  - [在线文档](https://wangkelei.github.io/megadoc/)

## 佩戴检测
    1. 切换至实现血氧模式
    2. onV2LiveSpoLive()返回包含acc值的实时血氧对象
    3. 引导用户摆出指定手势，若用户正确佩戴指环：四指向下时，accY = 0；手心向上时，accZ = 0

## 日常数据计算说明
    1.日常数据的开始时间需要开发者自行计算. start = MegaDailyBean.time-dailyUnit*60
    2.温度的单位是 ℃ ,temp/10 即可拿到温度.
    3.日常数据的同步时间需要开发者自行控制.

## 如何获取监测中的温度数据
    1.结束监测
    2.收取日常数据
    3.解析日常数据
    4.收取监测数据
    5.解析监测数据,将第2步收取的包含温度的日常数据过滤后合入监测数据中（过滤条件：日常数据的开始时间、结束时间需落在监测数据解析后的startAt、endAt区间内，时间和温度如何存储请自行定义数据格式）

## 数据说明
- 每监测 82 秒产生 256 字节的数据;
- 结束监测时指环里会保存这次监测的数据, 其中不足 256 字节的部分会被舍去;
- 数据在被收取后自动删除;
- 指环内部空间能存储 12 小时的睡眠监测, 建议在开始新的监测前检查收取数据;

## 注
- 导包方法：android studio， file -> new -> new module... -> import .jar/.aar package
- 导入native库
- 设备闲时，可`开启实时`、`开启长时检测`（血氧、运动）、`收检测数据`、`查询定时设置信息`、`开启/关闭定时监测`。
长时监控数据会被记录到戒指内部，实时数据不会。
长时监控开启后，可断开蓝牙连接，不必一直连着，戒指将自动保存心率血氧数据，以便后续手机连上收取。
- 每次连上戒指，若不在监测中，建议都要尝试收取监测数据。若戒指内部监测数据满了，会导致无法开启监测
- 一般需要监测1小时以上数据才有效
- MegaBleCallback.onOperationStatus回调会返回相关操作的结果
- 当您切换至新的解析函数时，请仔细阅读返回的的字段信息。
- 数据和日志的路径--->sdcard/megaBle (client.setDebugEnable(true))
- 开发者需要连续获取10秒-20秒的实时acc值来判断用户的佩戴方向,若用户正确佩戴指环：四指向下时，accY = 0；手心向上时，accZ= 0
- 其他
  - 权限要求：
  蓝牙、写文件、网络、定位
  - minSdkVersion 19
- 建议参考demo源码，并运行体验



# Demo使用说明
请结合android studio控制台查看输出信息

## assets资源、解析血氧，解析运动，解析日常计步数据
目录下有三份参考数据：血氧监测数据(mock_spo2.bin)、运动监测数据(mock_sport.bin)、日常计步数据(mock_daily.bin)。
对应按钮：解析血氧，解析运动，解析日常数据

## 选择文件、开始dfu
这两个按钮是后续戒指固件升级(dfu)时使用的，前期可不用关心。
demo为了方便使用本地选择文件，后续应将升级文件(.zip包)放在服务器，判断戒指固件版本号来升级

## 开全局live监听
允许戒指各个模式工作模式下的实时数据通知。可重复调用

## 其他按钮
- 开实时血氧 - 开启血氧实时监测模式
- 开血氧 - 开启血氧长时监测模式（睡眠监测）
- 开运动 - 开启运动监测模式
- 开脉诊 - 开启脉诊监测模式
- 关监控 - 关闭所有监测模式
- 收数据 - 收取【血氧长时监测】【运动监测】结束后产生的数据
- 收日常数据 - 收取日常计步数据
- 解析血氧 - 收取到的血氧数据，调用api联网验证并解析
- 解析运动 - 收取到的运动数据，调用api联网验证并解析
- 解析日常数据 - 收取到的日常计步数据，调用api联网验证并解析
- 开rawdata脉诊 - 获取脉诊模式时的rawdata数据
- 关rawdata
- 查看当前模式 - 查看戒指当前处于的模式
- 查看定时监测设置 - 获取定时监测的设置信息
- 选择开始时间 - 设置定时监测的开始时间
- 打开定时监测
- 关闭定时监测
- 选择监测时长
## 示例
1. 获取脉诊rawdata
    步骤：开脉诊、开rawdata脉诊


请在demo源码中搜索button名字，查看响应事件，详细api请参考在线java doc