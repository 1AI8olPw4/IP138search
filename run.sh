#!/bin/bash

# 创建lib目录（如果不存在）
mkdir -p lib

# 下载JSON库（如果不存在）
if [ ! -f "lib/json-20231013.jar" ]; then
    echo "下载JSON依赖..."
    curl -L "https://repo1.maven.org/maven2/org/json/json/20231013/json-20231013.jar" -o "lib/json-20231013.jar"
fi

# 编译Java文件（包含外部jar包）
echo "编译源代码..."
javac -encoding UTF-8 -cp "lib/*" SimpleGUI.java

# 检查编译是否成功
if [ $? -eq 0 ]; then
    echo "编译成功，正在运行程序..."
    # 运行时也需要包含外部jar包
    java -cp ".:lib/*" SimpleGUI
else
    echo "编译失败，请检查错误信息"
fi