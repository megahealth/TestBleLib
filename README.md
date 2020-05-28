# Mega Ble SDK 库使用文档（原生Android端）
库名: megablelibopen

## sdk文件
 - [arr库 v1.5.8](https://github.com/megahealth/TestBleLib/blob/master/megablelibopen/megablelibopen.aar)
 - [.so库 v9463](https://github.com/megahealth/TestBleLib/tree/master/app/src/main/jniLibs)
 - [demo v1.0.10](https://github.com/megahealth/TestBleLib)

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
8. Idle状态。用户可以开始进行操作，如：收数据、开监测
9. 解析数据(需要强制联网验证)，可以输出类似兆观健康应用中的报告统计信息

## 主要API：

- final public class MegaBleBuilder
```
client = new MegaBleBuilder()
                .withSecretId(id)
                .withSecretKey(key)
                .withContext(context)
                .withCallback(megaBleCallback)
                .build();
```

- public class MegaBleClient
```
client.toggleLive(true); // 开/关全局实时通道。兼容：血氧长时、运动、血氧实时
client.enableV2ModeLiveSpo(true); // 打开血氧实时模式
client.enableV2ModeDaily(true); // 关闭所有模式
client.enableV2ModeSpoMonitor(true); // 打开血氧长时模式 (睡眠血氧监测)
client.enableV2ModeSport(true); // 打开运动模式
client.enableV2ModePulse(true); // 打开脉诊模式
client.enableRawdataSpo // 打开血氧rawdata，需要打开血氧相关模式
client.enableRawdataPulse // 打开脉诊rawdata，需要打开脉诊模式
client.disableRawdata // 关闭所有rawdata
```

- public abstract class MegaBleCallback

- public class ParsedSpoPrBean

    解析血氧数据：血氧、脉率、睡眠分期；其他统计信息

- public class ParsedPrBean

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

## 完整Java Doc
  - [在线文档](https://ble-sdk-doc.now.sh/)

## 注
- 导包方法：android studio， file -> new -> new module... -> import .jar/.aar package
- 导入native库
- 设备闲时，可`开启实时`、`开启长时检测`（血氧、运动）、`收检测数据`。
长时监控数据会被记录到戒指内部，实时数据不会。
长时监控开启后，可断开蓝牙连接，不必一直连着，戒指将自动保存心率血氧数据，以便后续手机连上收取。
- 每次连上戒指，若不在监测中，建议都要尝试收取监测数据。若戒指内部监测数据满了，会导致无法开启监测
- 一般需要监测1小时以上数据才有效
- 其他
  - 权限要求：
  蓝牙、写文件、网络、定位
  - minSdkVersion 19
- 建议参考demo源码，并运行体验



# Demo使用说明
请结合android studio控制台查看输出信息

## assets资源、解析血氧，解析运动
目录下有两份参考数据：血氧监测数据(mock_spo2.bin)、运动监测数据(mock_sport.bin)。
对应按钮：解析血氧，解析运动

## 选择文件、开始dfu
这两个按钮是后续戒指固件升级(dfu)时使用的，前期可不用关心。
demo为了方便使用本地选择文件，后续应将升级文件(.zip包)放在服务器，判断戒指固件版本号来升级

## 开全局live监听
允许戒指各个模式工作模式下的实时数据通知。可重复调用

## 其他按钮
- 开实时血氧 - 开启血氧实时监测模式
- 开血氧 - 开启血氧长时监测模式（睡眠监测）
- 开运动 - 开启运动监测模式
- 关监控 - 关闭所有监测模式
- 收数据 - 收取【血氧长时监测】【运动监测】结束后产生的数据
- 解析血氧 - 收取到的血氧数据，调用api联网验证并解析
- 解析运动 - 收取到的运动数据，调用api联网验证并解析
- 开rawdata脉诊 - 获取脉诊模式时的rawdata数据
- 关rawdata

请在demo源码中搜索button名字，查看响应事件，详细api请参考在线java doc
