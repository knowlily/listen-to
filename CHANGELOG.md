# 更新日志

所有版本都遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

GitHub Releases: https://github.com/knowlily/listen-to/releases

## 当前版本
- **最新版本**: 2.0 (versionCode 12)
- **构建日期**: 2026-05-10

## [2.0.0] - 2026-05-10

### 新增
- **Room 数据库**: 历史记录和书签改用 Room 持久化，Kotlin Flow 自动刷新列表
- **多标签页**: 标签栏横向滚动，新建/关闭/切换标签，长按 "+" 新建隐私标签
- **隐私模式**: 按标签页独立隐身，不留历史、禁用 Cookie、不缓存
- **文件下载**: WebView 下载监听，接入系统 DownloadManager 通知栏下载
- **SSL 证书处理**: 不受信任/过期/域名不匹配证书弹窗警告
- **Hilt 依赖注入**: ViewModel/Repository 层依赖注入解耦

### 改进
- SharedPreferences 历史/书签数据自动迁移至 Room（首次启动）
- 硬件加速显式启用 (LAYER_TYPE_HARDWARE)
- 后退/前进按钮状态联动 WebView 导航历史
- README 更新：技术栈（Hilt/Room/Flow）、项目结构、权限说明
- .gitignore 补充 Room DB 运行时文件

### 修复
- 页面频闪：分离 navigateUrl 与 currentUrl LiveData，消除反馈循环
- 主题色同步：SettingsRepository 单例化，跨 ViewModel LiveData 共享
- 网址输入跳转外部浏览器：修复 URL 加载反馈环路

## [1.10.0] - 2026-05-10

### 重大重构
- **包名变更**: `com.example.simplebrowser` → `com.knowlily.browser`
- **架构分层**: 引入 ViewModel + Repository 架构，SharedPreferences 操作抽离至 Repository 层，旋转屏幕不丢数据
- **单 Activity 架构**: 4 个 Activity 合并为 1 个 Activity + 4 个 Fragment，Tab 切换更流畅，WebView 实例共享不销毁

### 新增
- SettingsRepository、HistoryRepository、BookmarksRepository — 数据持久化层
- BrowserViewModel、HistoryViewModel、BookmarksViewModel、SettingsViewModel — 状态管理层
- BrowserFragment、HistoryFragment、BookmarksFragment、SettingsFragment — UI 层

### 改进
- 底部导航栏 show/hide 通过 ViewModel 联动，Fragment 滚动带动 MainActivity 底部栏动画
- Fragment 间通过 shared ViewModel (activityViewModels) 通讯，无需 Intent 传参
- WebView 配置变更时保存/恢复状态 (saveState/restoreState)
- 项目结构模块化：model/ui/viewmodel/repository/adapter/plugin 分层清晰
- README 更新至新架构说明

## [1.9.0] - 2026-05-08

### 新增
- 用户可自行安装插件：支持从 URL 下载或本地文件加载 JSON 插件配置
- 三种常见插件类型：JavaScript 用户脚本、CSS 用户样式、AdBlock 广告拦截规则
- 插件 URL 匹配模式（glob 通配符），可限制插件仅在特定网站生效
- 插件卸载功能：用户安装的插件可随时卸载，内置插件受保护

### 改进
- PluginManager 新增 installPlugin/uninstallUserPlugin/loadUserPlugins 方法
- 设置页插件列表支持动态刷新，安装/卸载后即时更新
- 插件安装对话框：URL 输入 + 后台下载，本地文件选取器

## [1.8.0] - 2026-05-08
- **构建日期**: 2026-05-08

## [1.8.0] - 2026-05-08

### 新增
- 浏览器插件系统，统一的 BrowserPlugin 接口标准
- 广告拦截插件 (AdBlockerPlugin)，域名黑名单 + 元素隐藏
- 暗色模式插件 (DarkModePlugin)，CSS filter 全网页暗色
- 设置页插件管理界面，SwitchMaterial 开关切换

### 改进
- 底栏加宽：wrap_content → match_parent + 24dp 水平边距
- 搜索栏/底栏隐藏时 WebView 正确填充全屏
- 主题色选择重新设计：2 行网格 (4+3)，圆形按钮增大至 52dp
- 合并"关于"+"开发者信息"为一张可点击卡片，点击跳转 GitHub
- 设置卡片圆角统一为 12dp

## [1.7.0] - 2026-05-08

### 新增
- 悬浮底栏设计，圆角卡片式底部导航栏
- 滚动时自动隐藏/显示搜索栏和底栏，扩大阅读空间
- 自定义主题色功能，提供 7 种预设颜色（紫、蓝、青、红、橙、绿、粉）
- 设置页面主题色选择器，圆形颜色按钮带选中状态反馈
- 浏览器标识切换添加按钮选中状态视觉反馈
- 主题切换按钮添加选中状态视觉反馈
- 设置页面开发者信息 GitHub 链接可点击跳转

### 修复
- 修复 v1.7 悬浮底栏导致闪退的问题（Material 3 属性与 Material 2 主题不兼容）
- 修复 CoordinatorLayout 多子 View 滚动行为冲突
- 移除对 `bottomNavigationView.setBackgroundColor()` 的不安全调用

### 改进
- 工具栏高度统一为 44dp，图标按钮缩小至 40dp
- 默认搜索引擎改为 Bing
- 设置页、历史记录页、书签页应用自定义主题色
- CI 工作流优化：移除无效 Java home 配置、跳过 release 签名构建

## [1.6.0] - 2026-05-07

### 新增
- 添加书签管理功能（BookmarksActivity），支持添加、查看、清除书签
- 添加用户代理（UA）切换功能，支持电脑模式和手机模式
- 添加滚动时自动隐藏/显示导航栏，提供更大阅读空间
- 添加弹窗窗口支持（window.open），在覆盖层中显示弹窗内容
- 添加历史记录清除按钮

### 修复
- 修复地址栏按回车键时重复加载 URL 的问题（移除冗余 KeyListener）
- 修复地址栏失去焦点时意外触发页面跳转的问题
- 修复 WebView 混合内容安全漏洞（MIXED_CONTENT_ALWAYS_ALLOW → NEVER_ALLOW）
- 修复历史记录/书签列表 RecyclerView 在 NestedScrollView 中滚动性能问题
- 修复设置页面版本号（1.4）与 build.gradle（1.6）不一致的问题
- 修复 CI 工作流中 gradlew 无执行权限导致 exit code 126 的问题
- 禁止 WebView 访问本地文件（file:// 协议），增强安全性

### 改进
- 优化地址栏 URL 加载后光标定位到末尾
- 优化历史记录时间戳解析，增加异常容错
- 优化用户代理字符串，PC 模式使用完整桌面端 Chrome UA

## [1.1.0] - 2026-03-20

### 新增
- 添加页面加载进度条显示
- 添加WebChromeClient支持，显示网页标题
- 添加网络连接状态检查
- 添加加载中旋转动画图标
- 改进错误处理，支持新旧API版本
- 添加完整的历史记录功能 (HistoryActivity)
- 添加明暗主题切换功能 (浅色/深色/跟随系统)
- 添加设置页面，包含缓存清除功能
- 添加Material Design动态取色功能 (Android 12+)
- 添加APK签名配置和密钥库生成

### 改进
- 优化showLoading函数，加载时显示旋转图标
- 增强网络错误处理，显示具体错误信息
- 改进页面加载状态管理
- 优化进度条显示逻辑
- 改进URL处理，支持更多网址格式 (http://, https://, www., IP地址, localhost)
- 增强WebView配置，添加缓存和用户代理设置
- 改进WebView错误回调日志
- 添加Gradle wrapper脚本和配置
- 更新Android Gradle插件至8.3.2
- 更新Material依赖至1.13.0

### 修复
- 修复页面加载时按钮状态更新问题
- 修复错误发生时进度条不隐藏的问题
- 改进URL处理逻辑
- 修复标题栏重复显示问题
- 修复Drawable资源加载错误 (动画图标问题)
- 修复网页无法打开问题 (WebView配置问题)
- 修复设置页面大小异常
- 修复闪退错误和无法跳转网页问题
- 修复启动器图标引用问题
- 添加网络安全配置，允许明文流量

## [1.0.0] - 2026-03-20

### 初始版本
- Material Design 3 (M3Expressive) 界面设计
- 动态取色 (Dynamic Color) 支持
- Android System WebView浏览器核心
- 中文界面本地化
- 前进/后退/刷新导航
- 底部导航栏
- 夜间模式支持
- 手势返回支持