#!/bin/bash

# Start the java project
start() {
  # Check if the java process is running
  PID=\$(ps -ef | grep "\-Dapp.name=${appName}\s" | grep -v grep | awk '{print \$2}')
  if [ -n "\$PID" ]; then
    # The process is already running, do nothing
    echo "The app is already running, pid: \$PID"
    return 0
  fi
  # Start the java project in the background and redirect the output to a log file
  nohup ${javaPath} -Dapp.name=${appName} ${runOptions} -jar ${archiveName} ${runArgs}  >/dev/null 2>&1 &
  # Get the pid of the java process
  PID=\$!
  # Echo the pid to the console
  echo "The app is started, pid: \$PID"
}

# Stop the java project
stop() {
  PIDS=\$(ps -ef | grep "\-Dapp.name=${appName}s" | grep -v grep | awk '{print \$2}')
  if [ -n "\$PIDS" ]; then
    for PID in \$PIDS; do
      # Kill the process with SIGTERM signal
      kill \$PID
      # Wait for the process to exit
      wait \$PID
      # Echo a message to the console
      echo "The app is stopped, pid: \$PID"
    done
    return 0
  fi
  echo "The app is stopped"
}

# Restart the java project
restart() {
  # Stop the java project first
  stop
  # Start the java project again
  start
}

# Check the command line argument and call the corresponding function
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
echo "Usage: \$0 {start|stop|restart}"
exit 1
;;
esac

