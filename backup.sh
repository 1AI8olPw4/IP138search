#!/bin/bash

# 获取当前时间作为备份文件名的一部分
timestamp=$(date +"%Y%m%d_%H%M%S")
backup_dir="backup_${timestamp}"

echo "正在创建备份..."

# 创建备份目录
mkdir -p "${backup_dir}"

# 复制所有源文件和配置文件
cp SimpleGUI.java "${backup_dir}/"
cp pom.xml "${backup_dir}/" 2>/dev/null
cp build.sh "${backup_dir}/" 2>/dev/null
cp run.sh "${backup_dir}/" 2>/dev/null
cp clean.sh "${backup_dir}/" 2>/dev/null

# 创建zip文件
zip -r "${backup_dir}.zip" "${backup_dir}"

# 清理临时目录
rm -rf "${backup_dir}"

echo "备份完成！文件保存为: ${backup_dir}.zip" 