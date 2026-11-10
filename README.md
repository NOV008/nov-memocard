# Nov记忆卡

一款纯本地的 Android 间隔重复卡片应用，使用 **FSRS-6** 调度，Jetpack Compose + Room 构建。

> 本仓库不包含任何牌组内容。三个内置背诵牌组已移除；请导入你自己的 apkg / JSON / CSV。

## 功能

- FSRS-6 调度，支持期望保留率、每日新卡上限、每日复习上限
- 卡片翻面（3D + 跟手光效），重来 / 困难 / 良好 / 简单四档评分
- 撤销上一次评分
- 卡片标签、暂停 / 埋卡
- 图片卡（正反面配图）
- apkg / JSON / CSV 导入，JSON 导出备份（含 FSRS 状态）
- OLED 纯黑暗色主题、字号自适应、振动反馈
- Android 16 / ColorOS 16 流体云（Live Updates）：切后台显示当前问题与本次进度

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- Room（本地数据库）
- FSRS-6（间隔重复调度）
- 无网络权限：不联网、无账号、无云同步、无统计 SDK

## 构建

环境要求：JDK 17、Android SDK 36。

```bash
./gradlew assembleDebug
```

APK 输出：

```text
app/build/outputs/apk/debug/app-debug.apk
```

如果 Gradle 发行包下载慢，可将 `gradle/wrapper/gradle-wrapper.properties` 中的 `distributionUrl` 换成可用镜像。

发布自己的版本前，请修改 `app/build.gradle.kts` 里的 `applicationId`，并配置你自己的签名。

## 导入格式

### JSON

```json
{
  "name": "牌组名",
  "cards": [
    { "front": "问题或术语", "back": "答案或要点" }
  ]
}
```

也支持直接导入 `[{ "front": "...", "back": "..." }]` 数组。

### CSV

每行一张卡，第一个英文逗号前是 front，后面是 back：

```text
front,back
```

不支持带引号的逗号转义，字段内不要包含英文逗号。

### apkg

读取 Anki 包内 SQLite 的 `notes.flds`，取前两个字段作为 front / back。
暂不支持 `anki21b`（zstd 压缩）格式，也不会导入 apkg 内的媒体文件。

## 流体云（可选）

需要 Android 16 + ColorOS 16 的设备（如 OPPO Find N5）。

1. 应用已声明 `POST_PROMOTED_NOTIFICATIONS`（signature 权限）。
2. 安装后进入系统「通知与中心 → 流体云 → 更多支持的应用」，找到「Nov记忆卡」。
3. 打开「流体云显示实时活动」。

说明：该权限无法通过 adb 代授，只能走系统开关；展开卡片是系统模板，不支持自定义排版。

## 隐私

- Manifest 中没有 `INTERNET` 权限。
- 牌组、卡片、复习记录、设置全部保存在本机。
- 换机或重装前，请在「我的 → 导出备份（JSON）」手动备份。

## 项目结构

```text
app/src/main/java/com/nov/memocard/
├── data/          Room 实体、DAO、仓库、设置
├── fsrs/          FSRS-6 算法与调度
├── liveupdate/    流体云 / Live Updates
├── ui/            Compose 页面与组件
└── util/          apkg 导入、图片存储、崩溃处理
```

## 贡献

欢迎 Issue 和 PR。提交前请确认：

- 不提交任何有版权的牌组、教材或题库内容；
- 保持纯本地、无网络权限的定位；
- `./gradlew assembleDebug` 能通过。

## 许可

[MIT](LICENSE)

第三方组件：AndroidX、Jetpack Compose、Material 3、Room、Kotlin。
FSRS 为公开的间隔重复算法；本项目的实现为独立编写。

## 免责声明

本项目仅用于个人学习与间隔重复工具研究，不提供、不托管、不分发任何学习内容。
导入内容的版权与合规性由使用者自行负责。本项目与 Anki、OPPO / ColorOS 无隶属关系。
