#!/usr/bin/env bash
set -euo pipefail

# 简化的 GraalVM 安装脚本
# 用法: ./install-graalvm.sh <版本号>
# 示例: ./install-graalvm.sh 23.0.2

# 参数检查
if [[ $# -ne 1 ]]; then
    echo "用法: $0 <JDK版本> (例如: 23.0.2)" >&2
    exit 1
fi

readonly JDK_VERSION="$1"
readonly INSTALL_DIR="/opt/graalvm"
readonly TEMP_FILE="/tmp/graalvm-$$.tar.gz"

# 清理函数
cleanup() {
    rm -f "$TEMP_FILE"
}
trap cleanup EXIT

# 检测系统架构
get_arch() {
    case "$(uname -m)" in
        x86_64|amd64) echo "linux-x64" ;;
        aarch64|arm64) echo "linux-aarch64" ;;
        *) echo "不支持的架构: $(uname -m)" >&2; exit 1 ;;
    esac
}

# 下载文件
download_file() {
    local url="$1"
    local output="$2"

    if command -v curl &>/dev/null; then
        curl -fsSL --connect-timeout 30 --retry 3 -o "$output" "$url"
    elif command -v wget &>/dev/null; then
        wget -qO "$output" --timeout=30 --tries=3 "$url"
    else
        echo "错误: 需要 curl 或 wget" >&2
        return 1
    fi
}

# 主流程
main() {
    local arch file_arch tar_name download_url

    # 获取架构
    file_arch=$(get_arch)

    # 补齐环境
    yum install -y zip openssl-devel

    # 构建下载URL
    tar_name="graalvm-community-jdk-${JDK_VERSION}_${file_arch}_bin.tar.gz"
    download_url="https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-${JDK_VERSION}/${tar_name}"

    echo "正在下载 GraalVM ${JDK_VERSION}..."
    if ! download_file "$download_url" "$TEMP_FILE"; then
        echo "下载失败" >&2
        exit 1
    fi

    echo "正在安装到 ${INSTALL_DIR}..."

    # 清理旧安装
    rm -rf "$INSTALL_DIR"
    mkdir -p "$INSTALL_DIR"

    # 解压文件（直接解压内容，跳过顶层目录）
    tar -xzf "$TEMP_FILE" -C "$INSTALL_DIR" --strip-components=1

    # 验证安装
    if [[ ! -f "${INSTALL_DIR}/bin/java" ]]; then
        echo "安装验证失败: 找不到 java 可执行文件" >&2
        exit 1
    fi

    # 设置环境变量（仅用于当前会话验证）
    export JAVA_HOME="$INSTALL_DIR"
    export PATH="${JAVA_HOME}/bin:$PATH"

    # 显示版本信息
    echo "GraalVM 安装成功！"
    echo "安装路径: $INSTALL_DIR"
    "${INSTALL_DIR}/bin/java" -version

    # 检查 native-image（GraalVM 21+ 默认包含）
    if [[ -f "${INSTALL_DIR}/bin/native-image" ]]; then
        echo "Native Image 版本:"
        "${INSTALL_DIR}/bin/native-image" --version
    fi

    # 输出环境变量配置建议
    cat <<EOF

要永久设置环境变量，请将以下内容添加到 ~/.bashrc 或 /etc/profile:
export JAVA_HOME="${INSTALL_DIR}"
export PATH="\${JAVA_HOME}/bin:\${PATH}"
EOF
}

# 执行主函数
main