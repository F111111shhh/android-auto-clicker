# 连点器

一个个人使用的安卓连点器。它通过 Android 辅助功能服务执行用户主动配置的点击，通过悬浮窗提供开始、暂停、停止和选区控制。

## 功能

- 设置固定点击次数或无限点击。
- 设置点击间隔毫秒。
- 支持矩形范围和中心点半径两种点击范围。
- 可在指定范围内随机点击。
- 可开启随机时间浮动，避免完全相同间隔。
- 支持悬浮窗控制：开始、暂停、停止、选区、回到设置。
- 保存上次参数，下次打开自动恢复。

## 安装

1. 安装 Release 页面中的 APK。
2. 打开应用，按提示开启“悬浮窗权限”。
3. 打开“辅助功能权限”，启用“连点器点击服务”。
4. 回到应用设置点击次数、频率和范围。
5. 打开悬浮控制，在目标应用里点击“开始”。

## 权限说明

- 悬浮窗权限：用于显示控制面板和范围选择层。
- 辅助功能权限：用于执行你设置的屏幕点击。

应用不需要 root，不读取目标应用内容，不上传数据。

## 本地构建

项目使用原生 Android Views 和 Java。构建 release APK：

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME='C:\Users\FISH\AppData\Local\Android\Sdk'
.\gradlew.bat :app:assembleRelease
```

本地签名文件位于 `keystore/`，已被 `.gitignore` 排除。
