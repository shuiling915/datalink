#!/bin/bash
# ============================================================
# 第二步（可选）：安装 DataX 数据同步服务（Docker）
# 前提：已运行 setup-hive.sh
#
# 使用：
#   ./setup-datax.sh start    启动 DataX 容器
#   ./setup-datax.sh stop     停止 DataX 容器
#   ./setup-datax.sh status   查看状态
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CONF_FILE="$SCRIPT_DIR/datalink.conf"

# ---------- 颜色输出 ----------
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ---------- 加载配置 ----------
if [ ! -f "$CONF_FILE" ]; then
  error "配置文件不存在: $CONF_FILE"
  echo "  请先运行 ./setup-hive.sh"
  exit 1
fi

source "$CONF_FILE"

CONTAINER_NAME="datalink-datax"

# ---------- status ----------
if [ "$1" = "status" ]; then
  if docker ps --format '{{.Names}}' | grep -q $CONTAINER_NAME; then
    info "DataX 容器运行中"
    docker ps --filter "name=$CONTAINER_NAME" --format "table {{.Names}}\t{{.Status}}"
  elif docker ps -a --format '{{.Names}}' | grep -q $CONTAINER_NAME; then
    warn "DataX 容器已停止"
  else
    info "DataX 容器未安装"
  fi
  exit 0
fi

# ---------- stop ----------
if [ "$1" = "stop" ]; then
  if docker ps --format '{{.Names}}' | grep -q $CONTAINER_NAME; then
    docker stop $CONTAINER_NAME >/dev/null
    docker rm $CONTAINER_NAME >/dev/null
    info "DataX 已停止"
  else
    warn "DataX 未在运行"
  fi
  # 把 DATAX_MODE 改回 local
  sed -i '' "s/^DATAX_MODE=.*/DATAX_MODE=local/" "$CONF_FILE" 2>/dev/null || \
  sed -i "s/^DATAX_MODE=.*/DATAX_MODE=local/" "$CONF_FILE"
  info "DATAX_MODE 已切换为 local"
  exit 0
fi

# ---------- start ----------
if [ "$1" != "start" ]; then
  echo "用法："
  echo "  ./setup-datax.sh start    启动 DataX 容器"
  echo "  ./setup-datax.sh stop     停止 DataX 容器"
  echo "  ./setup-datax.sh status   查看状态"
  exit 1
fi

# 检查 Docker
if ! docker info &>/dev/null; then
  error "Docker 未启动"
  exit 1
fi

# 检查网络
docker network inspect $NETWORK &>/dev/null || {
  error "Docker 网络 $NETWORK 不存在，请先运行 ./setup-hive.sh"
  exit 1
}

# 如果已存在，先删掉
if docker ps -a --format '{{.Names}}' | grep -q $CONTAINER_NAME; then
  docker stop $CONTAINER_NAME >/dev/null 2>&1 || true
  docker rm $CONTAINER_NAME >/dev/null 2>&1 || true
fi

# 启动 DataX 容器
info "启动 DataX 容器..."
docker run -d \
  --name $CONTAINER_NAME \
  --network $NETWORK \
  --hostname datax \
  -v /tmp/datax_jobs:/tmp/datax_jobs \
  --restart unless-stopped \
  datadocker1018/datalink-base:1.0 \
  tail -f /dev/null

# 等待容器就绪
sleep 2
if docker ps --format '{{.Names}}' | grep -q $CONTAINER_NAME; then
  info "DataX 容器已启动"
else
  error "DataX 容器启动失败"
  exit 1
fi

# 验证 DataX 可用
docker exec $CONTAINER_NAME python /opt/datax/bin/datax.py -h >/dev/null 2>&1
if [ $? -eq 0 ]; then
  info "DataX 验证通过"
else
  warn "DataX 验证失败，可能不影响使用"
fi

# 把 DATAX_MODE 改为 docker
sed -i '' "s/^DATAX_MODE=.*/DATAX_MODE=docker/" "$CONF_FILE" 2>/dev/null || \
sed -i "s/^DATAX_MODE=.*/DATAX_MODE=docker/" "$CONF_FILE"

# ==================== 完成 ====================
echo ""
echo "============================================"
info "DataX 已就绪！"
echo ""
echo "  容器名：$CONTAINER_NAME"
echo "  DATAX_MODE 已切换为 docker"
echo ""
echo "  注意：需要重启 DataLink 使配置生效"
echo "  ./setup-datalink.sh stop && ./setup-datalink.sh"
echo ""
echo "  停止：./setup-datax.sh stop"
echo "============================================"
