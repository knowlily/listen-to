# 简单浏览器 (Simple Browser)

一个轻量级的 Android 浏览器，基于 System WebView，采用 Material Design 2 外观，支持动态取色和中文界面。

## 功能特性

- 网页浏览：基于 Android System WebView，支持 JavaScript、DOM Storage
- 导航控制：前进、后退、刷新、地址栏输入
- 书签管理：添加、查看、清除书签，按时间排序
- 历史记录：浏览历史查看，支持清除
- 悬浮底栏：圆角卡片式底部导航，滚动时自动隐藏/显示
- 主题切换：浅色 / 深色 / 跟随系统
- 自定义主题色：7 种预设颜色可选（紫、蓝、青、红、橙、绿、粉）
- 浏览器标识切换：电脑模式 / 手机模式 UA
- 弹窗支持：window.open 在覆盖层中显示
- 动态取色：Android 12+ 自动适配系统壁纸主题色
- 缓存清除：WebView 缓存和历史记录清除
- 加载进度条：线性进度指示器
- 中文界面

## 技术栈

- Kotlin
- Android Jetpack (AppCompat, ViewBinding)
- Material Design 2 Components
- Android System WebView
- SharedPreferences 持久化

## 项目结构

```
app/
├── src/main/java/com/example/simplebrowser/
│   ├── MainActivity.kt            # 主浏览器界面
│   ├── SettingsActivity.kt        # 设置页面
│   ├── HistoryActivity.kt         # 历史记录
│   └── BookmarksActivity.kt       # 书签管理
├── src/main/res/
│   ├── layout/                    # 布局文件
│   │   ├── activity_main.xml      # 主界面
│   │   ├── activity_settings.xml  # 设置界面
│   │   ├── activity_history.xml   # 历史记录界面
│   │   ├── activity_bookmarks.xml # 书签界面
│   │   └── item_bookmark.xml      # 书签列表项
│   ├── values/                    # 资源文件 (颜色、字符串、样式)
│   ├── drawable/                  # 矢量图标
│   ├── menu/                      # 菜单 (bottom_nav_menu)
│   └── xml/                       # 网络配置
├── build.gradle                   # 模块配置
└── AndroidManifest.xml            # 应用清单
```

## 构建和运行

### 前提条件

1. Android Studio (最新版)
2. Android SDK 34
3. JDK 17

### 命令行构建

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK（需要签名密钥）
./gradlew assembleRelease

# 安装到设备
./gradlew installDebug
```

## 权限

- `INTERNET` — 访问互联网
- `ACCESS_NETWORK_STATE` — 检查网络连接状态

## 兼容性

- **最低版本**: Android 7.0 (API 24)
- **目标版本**: Android 14 (API 34)
- **动态取色**: Android 12+ (API 31+)

## 许可证

MIT License
