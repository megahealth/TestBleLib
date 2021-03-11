# Mega Ble SDK 库使用文档（原生Android端）
库名: megablelibopen

- [EN](./README.md) | 中文

## sdk文件
 - [arr库 v1.6.7](https://github.com/megahealth/TestBleLib/blob/master/megablelibopen/megablelibopen-1.6.7.aar)
 - [.so库 v10830](https://github.com/megahealth/TestBleLib/tree/master/app/src/main/jniLibs)
 - [demo v1.0.16](https://github.com/megahealth/TestBleLib)

建议克隆demo后，arr库和.so库从demo中取出使用

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
client.syncDailyData() // 同步日常计步数据
client.getV2PeriodSetting() // 获取定时监测的设置信息 (MegaBleCallback.onV2PeriodSettingReceived返回设置信息)
client.enableV2PeriodMonitor(true, boolean isLoop, int monitorDuration, int timeLeft) // 打开定时监测 参数释义：true、是否重复、监测时长(s)、距离监测开启的时长(s)
client.enableV2PeriodMonitor(false, false, 0, 0) // 关闭定时监测
client.parseSpoPr(bytes, callback) // 解析血氧数据
client.parseSport(bytes, callback) // 解析运动数据
client.parseSpoPrOld(bytes, callback) // 解析血氧数据(已弃用，请使用parseSpoPr方法)
client.parseSportOld(bytes, callback) // 解析血氧数据(已弃用，请使用parseSport方法)
```

- public abstract class MegaBleCallback
```
// 参数：[[通道1value, 通道2value], [通道1value, 通道2value]]。长度不固定，可能1组或2组；
// 血氧、脉诊都是此通道
onRawdataParsed([])
```

- public class ParsedSpoPrBean（已废弃，替换为MegaSpoPrBean）

    解析血氧数据：血氧、脉率、睡眠分期；其他统计信息

- public class ParsedPrBean（已废弃，替换为MegaPrBean）

    解析运动数据：脉率；其他统计信息

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
|offhandMinutes|离手时长(minutes)|
|wakeMinutes|清醒期(minutes)|
|remMinutes|眼动期(minutes)|
|lightMinutes|浅睡期(minutes)|
|deepMinutes|深睡期(minutes)|
|downIndex|氧减指数|
|downTimes|氧减次数|
|secondsUnder60|血氧饱和度 <60% 的时间(s)|
|secondsUnder70|血氧饱和度 <70% 的时间(s)|
|secondsUnder80|血氧饱和度 <80% 的时间(s)|
|secondsUnder85|血氧饱和度 <85% 的时间(s)|
|secondsUnder90|血氧饱和度 <90% 的时间(s)|
|secondsUnder95|血氧饱和度 <95% 的时间(s)|
|shareUnder60|血氧饱和度 <60% 的时间占比(*100转换为%)|
|shareUnder70|血氧饱和度 <70% 的时间占比(*100转换为%)|
|shareUnder80|血氧饱和度 <80% 的时间占比(*100转换为%)|
|shareUnder85|血氧饱和度 <85% 的时间占比(*100转换为%)|
|shareUnder90|血氧饱和度 <90% 的时间占比(*100转换为%)|
|shareUnder95|血氧饱和度 <95% 的时间占比(*100转换为%)|

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
- 查看定时监测设置 - 获取定时监测的设置信息
- 选择开始时间 - 设置定时监测的开始时间
- 打开定时监测
- 关闭定时监测
- 选择监测时长
## 示例
1. 获取脉诊rawdata
    步骤：开脉诊、开rawdata脉诊


请在demo源码中搜索button名字，查看响应事件，详细api请参考在线java doc