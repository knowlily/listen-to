# 简单浏览器 (Simple Browser)

一个使用Android System WebView的小型安卓浏览器，采用Material Design 3 (M3Expressive) 外观，支持动态取色和中文界面。

## 功能特性

- ✅ 使用Android System WebView渲染网页
- ✅ Material Design 3 (M3Expressive) 界面设计
- ✅ 动态取色 (Dynamic Color) - 自动适配系统主题色
- ✅ 中文界面支持
- ✅ 地址栏输入和导航
- ✅ 前进/后退/刷新按钮
- ✅ 底部导航栏
- ✅ 夜间模式支持
- ✅ 手势支持 (后退手势)

## 技术栈

- Kotlin
- Android Jetpack
- Material Design 3 Components
- Android System WebView

## 项目结构

```
app/
├── src/main/java/com/example/simplebrowser/
│   └── MainActivity.kt          # 主活动
├── src/main/res/
│   ├── layout/                  # 布局文件
│   ├── values/                  # 资源文件
│   ├── values-zh/              # 中文资源
│   ├── values-night/           # 夜间模式
│   ├── drawable/               # 矢量图标
│   ├── menu/                   # 菜单
│   └── xml/                    # 配置文件
├── build.gradle                # 模块配置
├── AndroidManifest.xml         # 应用清单
└── ...
```

## 构建和运行

### 前提条件

1. Android Studio (建议使用最新版本)
2. Android SDK 34或更高版本
3. Gradle

### 构建步骤

1. 使用Android Studio打开项目
2. 同步Gradle依赖
3. 连接Android设备或启动模拟器
4. 点击运行按钮或使用 `./gradlew installDebug`

### 命令行构建

```bash
# 清理项目
./gradlew clean

# 构建APK
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug
```

## 配置说明

### 动态取色

应用使用Material Design 3的动态取色功能，自动从壁纸提取主题色。要启用此功能：

1. 设备需要Android 12+ (API 31+)
2. 在系统设置中启用"Material You"或动态取色
3. 应用会自动适配系统主题色

### 中文支持

应用已完全中文本地化：
- 主界面按钮和标签
- 错误提示信息
- 底部导航栏

## 权限说明

应用需要以下权限：
- `INTERNET` - 访问互联网
- `ACCESS_NETWORK_STATE` - 检查网络状态

## 兼容性

- **最低Android版本**: API 24 (Android 7.0)
- **目标Android版本**: API 34 (Android 14)
- **建议Android版本**: API 31+ (Android 12+) 以获得完整的动态取色体验

## 已知限制

- 书签和历史记录功能目前仅显示占位信息
- 设置页面尚未实现完整功能
- 需要Android System WebView组件支持

## 未来改进计划

1. 实现书签管理
2. 添加历史记录功能
3. 实现标签页管理
4. 添加下载管理器
5. 实现隐私模式
6. 添加广告拦截功能
7. 支持自定义搜索引擎

## 贡献

欢迎提交Issue和Pull Request！

## 许可证

本项目采用MIT许可证。详情请见LICENSE文件。