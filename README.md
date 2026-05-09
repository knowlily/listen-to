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
- 浏览器插件系统：内置广告拦截 + 夜间模式，支持用户安装 JS/CSS/AdBlock 插件
- 插件安装：支持从 URL 下载或本地文件加载 JSON 配置
- ViewModel + Repository 架构：数据持久化与 UI 分离，旋转屏幕不丢状态
- 单 Activity 多 Fragment：流畅的 Tab 切换，WebView 实例共享

## 技术栈

- Kotlin
- Android Jetpack (ViewModel, LiveData, Fragment)
- Material Design 2 Components
- Android System WebView
- SharedPreferences 持久化
- Repository 架构模式

## 项目结构

```
app/
├── src/main/java/com/knowlily/browser/
│   ├── MainActivity.kt              # 单一 Activity，管理 Fragment 切换
│   ├── model/
│   │   ├── HistoryItem.kt           # 历史记录数据类
│   │   └── BookmarkItem.kt          # 书签数据类
│   ├── ui/
│   │   ├── BrowserFragment.kt       # 浏览器主界面 (WebView)
│   │   ├── HistoryFragment.kt       # 历史记录页
│   │   ├── BookmarksFragment.kt     # 书签管理页
│   │   └── SettingsFragment.kt      # 设置页
│   ├── viewmodel/
│   │   ├── BrowserViewModel.kt      # 浏览器状态管理
│   │   ├── HistoryViewModel.kt      # 历史记录状态
│   │   ├── BookmarksViewModel.kt    # 书签状态
│   │   └── SettingsViewModel.kt     # 设置状态
│   ├── repository/
│   │   ├── SettingsRepository.kt    # 设置数据持久化
│   │   ├── HistoryRepository.kt     # 历史记录持久化
│   │   └── BookmarksRepository.kt   # 书签持久化
│   ├── adapter/
│   │   ├── HistoryAdapter.kt        # 历史记录列表适配器
│   │   └── BookmarksAdapter.kt      # 书签列表适配器
│   └── plugin/
│       ├── BrowserPlugin.kt         # 插件统一接口
│       ├── PluginManager.kt         # 插件管理器
│       ├── AdBlockerPlugin.kt       # 广告拦截插件
│       ├── DarkModePlugin.kt        # 夜间模式插件
│       ├── UserPlugin.kt            # 用户自定义插件
│       └── UserPluginRepository.kt  # 用户插件持久化
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
