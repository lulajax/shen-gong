#!/bin/bash
# 神工项目启动脚本

set -e

echo "🚀 启动神工 Agent Runtime..."

# 检查 Java 版本
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未安装 Java"
    echo "请安装 Java 21+ 后再试"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "❌ 错误: Java 版本过低 (需要 21+,当前 $JAVA_VERSION)"
    exit 1
fi

echo "✅ Java 版本检查通过"

# 检查 Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ 错误: 未安装 Maven"
    exit 1
fi

echo "✅ Maven 检查通过"

# 检查环境变量
if [ -z "$OPENAI_API_KEY" ]; then
    echo "⚠️  警告: 未设置 OPENAI_API_KEY 环境变量"
    echo "请设置: export OPENAI_API_KEY=your_api_key"
fi

# 编译项目
echo "📦 编译项目..."
mvn clean package -DskipTests

# 启动应用
echo "🎉 启动应用..."
java -jar target/agent-runtime-1.0.0.jar

