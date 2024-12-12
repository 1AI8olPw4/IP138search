# IP查询工具

一个基于Java Swing开发的IP地址查询工具，支持批量查询IP地址的地理位置信息。使用IP138的API服务，提供高效的并发查询功能。

## 主要功能

- ✨ 单个/批量IP地址查询
- 🚀 多线程并发处理
- 📊 表格化展示结果
- 💾 导出CSV格式报告
- 🔑 自定义API Token配置
- 🎨 美观的用户界面

## 使用前配置

⚠️ **重要**: 使用前需要配置IP138的API Token

1. 获取Token
   - 访问 [IP138官网](https://www.ip138.com/api/)
   - 注册并获取API Token

2. 配置Token
   - 运行程序后点击"设置"按钮
   - 输入获取到的Token
   - 点击"测试Token"验证
   - 确认有效后保存

## 使用方法

### 方式一：直接运行（开发模式）
```bash
./run.sh
```

### 方式二：使用打包版本

运行程序：
   - Windows：
     - 双击 `IP138search.jar` 文件
     - 或在命令行中运行：`java -jar IP138search.jar`
   
   - Linux/Mac：
```bash
java -jar IP138search.jar
```

### 使用说明
1. 在输入框中输入IP地址：
   - 单个IP：`8.8.8.8`
   - 多个IP：`8.8.8.8, 8.8.4.4`

2. 点击"查询IP"按钮开始查询

3. 查看结果后可点击"导出结果"保存为CSV文件

## 系统要求
- JDK 1.8+
- 网络连接
- IP138 API Token（必需）

## 注意事项
- 确保Java环境正确安装
- 运行jar文件时确保lib目录在同级目录下
- 首次运行需要配置API Token
- 导出的CSV文件支持Excel直接打开
