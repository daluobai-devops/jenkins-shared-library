#!/bin/bash

# 定义java服务的路径和名称
SHELL_PATH=\$(readlink -f "\$0")
SHELL_DIR=\$(dirname "\$path")
SERVICE_DIR=\$SHELL_DIR
SERVICE_NAME=${appName}

# 定义pid文件的路径和名称
PID_FILE=\$SERVICE_DIR/service.pid

# 定义启动、停止和重启的函数
start() {
  # 检查pid文件是否存在
  if [ -f \$PID_FILE ]; then
    # 读取pid文件中的进程号
    pid=\$(cat \$PID_FILE)
    # 检查进程是否存在
    if kill -0 \$pid >/dev/null 2>&1; then
      echo "\$SERVICE_NAME is already running (pid: \$pid)"
      return
    else
      # 如果进程不存在，删除pid文件
      rm -f \$PID_FILE
    fi
  fi
  # 启动服务，并将进程号写入pid文件
  echo "Starting \$SERVICE_NAME ..."
  nohup /usr/local/bin/java -Dapp.name=${appName} ${runOptions} -jar ${SERVICE_DIR}/${archiveName} ${runArgs}  >/dev/null 2>&1 &
  echo \$! > \$PID_FILE
  echo "\$SERVICE_NAME started"
}

stop() {
  # 检查pid文件是否存在
  if [ -f \$PID_FILE ]; then
    # 读取pid文件中的进程号
    pid=\$(cat \$PID_FILE)
    # 检查进程是否存在
    if kill -0 \$pid >/dev/null 2>&1; then
      # 停止服务，并删除pid文件
      echo "Stopping \$SERVICE_NAME ..."
      kill -9 \$pid
      rm -f \$PID_FILE
      echo "\$SERVICE_NAME stopped"
    else
      # 如果进程不存在，删除pid文件，并提示服务没有运行
      rm -f \$PID_FILE
      echo "\$SERVICE_NAME is not running"
    fi
  else
    # 如果pid文件不存在，提示服务没有运行
    echo "\$SERVICE_NAME is not running"
  fi
}

restart() {
  # 重启服务，相当于先停止再启动
  stop
  start
}

# 根据传入的参数执行相应的函数
case "\$1" in
  start)
    start
    ;;
  stop)
    stop
    ;;
  restart)
    restart
    ;;
  *)
    # 如果没有传入参数或者参数不合法，打印帮助信息
    echo "Usage: \$0 {start|stop|restart}"
esac

exit 0

