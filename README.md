# KnowlilyBrowser

基于 Android System WebView 的轻量级浏览器，Material Design 2 外观，支持动态取色、插件系统和多标签页浏览。

## 功能特性

- **核心浏览** — System WebView，支持 JavaScript、DOM Storage，拦截混合内容
- **多标签页** — 标签栏添加/关闭/切换，单 WebView 实例 + LRU 状态缓存（上限 5 个）
- **隐私模式** — 按标签页独立隐身：不留历史、不存缓存、切换时 Cookie 隔离
- **书签管理** — 添加/查看/清除/导入/导出（JSON），Room 数据库 + Flow 实时刷新
- **历史记录** — 浏览历史查看与搜索，Room 数据库 + Flow
- **地址联想** — 输入时防抖搜索，匹配历史记录和书签
- **页内查找** — 内置查找栏，显示匹配计数，支持上/下一个导航
- **下拉刷新** — SwipeRefreshLayout 全局支持
- **HTTPS-Only 模式** — 自动将 HTTP 升级为 HTTPS
- **主题系统** — 浅色 / 深色 / 跟随系统，7 种预设主题色 + Material You 动态取色（Android 12+）
- **浏览器标识切换** — 电脑模式 / 手机模式 UA
- **悬浮底栏** — 圆角卡片式底部导航，滚动时自动隐藏/显示
- **文件下载** — 系统 DownloadManager，下载完成通知
- **插件系统** — `BrowserPlugin` 接口 + 生命周期钩子，可扩展
  - 内置：广告拦截、夜间模式（CSS 注入）
  - 用户安装：从 URL 或本地 JSON 文件加载 JS/CSS/AdBlock 插件
- **错误处理** — 自定义错误页（可重试），SSL 证书错误默认拦截
- **弹窗支持** — `window.open` 在覆盖层中展示，WebView 安全加固

## 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Kotlin 1.9.22 |
| 构建 | Gradle 8.5, AGP 8.3.2, KSP |
| 依赖注入 | Hilt 2.50 |
| 数据库 | Room 2.6.1 + Kotlin Flow |
| 架构 | MVVM + Repository，单 Activity 多 Fragment |
| UI | Material Design 2 (1.13.0), ViewBinding, Navigation |
| 最低/目标 SDK | API 24 (Android 7.0) / API 34 (Android 14) |

## 下载

从 [GitHub Releases](https://github.com/knowlily/listen-to/releases) 下载最新 APK。

## 项目结构

```
app/
├── src/main/java/com/knowlily/browser/
│   ├── MainActivity.kt              # 单一 Activity，管理 Fragment 切换 + 底部导航
│   ├── KnowlilyApplication.kt       # @HiltAndroidApp 入口
│   ├── di/
│   │   ├── AppModule.kt             # Hilt 单例组件模块
│   │   ├── DatabaseModule.kt        # Room 数据库 + DAO 提供者
│   │   └── SettingsRepositoryEntryPoint.kt  # EntryPoint，支持 onCreate 前注入
│   ├── data/
│   │   ├── AppDatabase.kt           # Room 数据库
│   │   ├── HistoryDao.kt            # 历史记录 DAO
│   │   └── BookmarksDao.kt          # 书签 DAO
│   ├── model/
│   │   ├── HistoryItem.kt           # 历史记录实体
│   │   ├── BookmarkItem.kt          # 书签实体
│   │   └── TabItem.kt               # 标签页数据
│   ├── ui/
│   │   ├── BrowserFragment.kt       # 浏览器主界面（标签栏 + WebView + 查找栏）
│   │   ├── HistoryFragment.kt       # 历史记录页
│   │   ├── BookmarksFragment.kt     # 书签管理页
│   │   └── SettingsFragment.kt      # 设置页
│   ├── viewmodel/
│   │   ├── BrowserViewModel.kt      # 标签管理、导航、地址联想
│   │   ├── HistoryViewModel.kt      # 历史记录状态
│   │   ├── BookmarksViewModel.kt    # 书签状态
│   │   └── SettingsViewModel.kt     # 主题、插件、缓存、安装
│   ├── repository/
│   │   ├── SettingsRepository.kt    # SharedPreferences，@Singleton @Inject
│   │   ├── HistoryRepository.kt     # 历史记录持久化
│   │   └── BookmarksRepository.kt   # 书签持久化
│   ├── adapter/
│   │   ├── HistoryAdapter.kt        # 历史记录列表适配器
│   │   ├── BookmarksAdapter.kt      # 书签列表适配器
│   │   └── TabAdapter.kt            # 标签栏适配器
│   └── plugin/
│       ├── BrowserPlugin.kt         # 插件统一接口
│       ├── PluginManager.kt         # @Singleton @Inject，生命周期 + JS 注入
│       ├── AdBlockerPlugin.kt       # 广告拦截插件
│       ├── DarkModePlugin.kt        # 夜间模式插件
│       ├── UserPlugin.kt            # 用户自定义插件
│       └── UserPluginRepository.kt  # 用户插件持久化
├── src/main/res/
│   ├── layout/                      # 布局文件
│   ├── values/                      # strings.xml（中文默认）、颜色、样式
│   ├── drawable/                    # 矢量图标
│   ├── menu/                        # 工具栏 + 底部导航菜单
│   └── xml/                         # network_security_config, backup_rules
├── build.gradle                     # minifyEnabled true, R8 + ProGuard
└── AndroidManifest.xml
```

## 构建

### 前提条件

- JDK 17
- Android SDK 34 + build-tools 34.0.0
- （可选）Release 构建需在项目根目录放置 `signing.properties`：
  ```properties
  storeFile=keystore.jks
  storePassword=你的密码
  keyAlias=你的别名
  keyPassword=你的密码
  ```

### 命令

```bash
./gradlew assembleDebug      # 构建 Debug APK
./gradlew assembleRelease    # 构建 Release APK（R8 混淆，需 signing.properties）
./gradlew installDebug       # 安装到已连接设备
```

## 权限

| 权限 | 用途 |
|------|------|
| `INTERNET` | 访问互联网 |
| `ACCESS_NETWORK_STATE` | 加载前检查网络连接 |
| `WRITE_EXTERNAL_STORAGE` (API < 29) | 文件下载 |

## 兼容性

- **最低版本**: Android 7.0 (API 24)
- **目标版本**: Android 14 (API 34)
- **动态取色**: Android 12+ (API 31+)

## 许可证

MIT License
