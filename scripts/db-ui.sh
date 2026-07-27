#!/usr/bin/env bash
#
# Copyright 2026 Enrico Olivelli
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Start/stop the DB UI web application in the background.
#
# Usage:
#   ./scripts/db-ui.sh start [-- <extra java args>]   start in background
#   ./scripts/db-ui.sh stop                           stop the running app
#   ./scripts/db-ui.sh restart                         stop then start
#   ./scripts/db-ui.sh status                          show whether it is running
#   ./scripts/db-ui.sh logs                            tail -f the app log
#
# Environment overrides:
#   PORT   HTTP port the app listens on (default 8080)
#   JAR    path to the runnable jar (default dbui/target/db-ui.jar)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAR="${JAR:-$ROOT/dbui/target/db-ui.jar}"
PORT="${PORT:-8080}"
RUN_DIR="$ROOT/.run"
PID_FILE="$RUN_DIR/db-ui.pid"
LOG_FILE="$RUN_DIR/db-ui.log"

# Prints the PID if the app is running, otherwise nothing.
running_pid() {
  [ -f "$PID_FILE" ] || return 0
  local pid
  pid="$(cat "$PID_FILE" 2>/dev/null || true)"
  if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
    echo "$pid"
  fi
}

start() {
  local pid
  pid="$(running_pid)"
  if [ -n "$pid" ]; then
    echo "DB UI is already running (pid $pid) on http://localhost:$PORT"
    return 0
  fi

  if [ ! -f "$JAR" ]; then
    echo "Jar not found: $JAR" >&2
    echo "Build it first with:  mvn package -pl dbui -am  (add -DskipTests to go faster)" >&2
    exit 1
  fi

  mkdir -p "$RUN_DIR"
  echo "Starting DB UI (port $PORT)..."
  nohup java -jar "$JAR" --server.port="$PORT" "$@" >"$LOG_FILE" 2>&1 &
  local new_pid=$!
  echo "$new_pid" >"$PID_FILE"

  # Wait for the HTTP port to answer, or fail fast if the process dies.
  for _ in $(seq 1 30); do
    if ! kill -0 "$new_pid" 2>/dev/null; then
      echo "DB UI failed to start. Recent log:" >&2
      tail -n 20 "$LOG_FILE" >&2
      rm -f "$PID_FILE"
      exit 1
    fi
    if curl -sf -o /dev/null "http://localhost:$PORT/"; then
      echo "DB UI is up (pid $new_pid) → http://localhost:$PORT"
      echo "Logs: $LOG_FILE"
      return 0
    fi
    sleep 1
  done

  echo "DB UI started (pid $new_pid) but did not answer on port $PORT yet." >&2
  echo "Check the log:  $LOG_FILE" >&2
}

stop() {
  local pid
  pid="$(running_pid)"
  if [ -z "$pid" ]; then
    echo "DB UI is not running."
    rm -f "$PID_FILE"
    return 0
  fi

  echo "Stopping DB UI (pid $pid)..."
  kill "$pid" 2>/dev/null || true
  for _ in $(seq 1 20); do
    kill -0 "$pid" 2>/dev/null || break
    sleep 0.5
  done
  if kill -0 "$pid" 2>/dev/null; then
    echo "Still alive, forcing..."
    kill -9 "$pid" 2>/dev/null || true
  fi
  rm -f "$PID_FILE"
  echo "Stopped."
}

status() {
  local pid
  pid="$(running_pid)"
  if [ -n "$pid" ]; then
    echo "DB UI is RUNNING (pid $pid) on http://localhost:$PORT"
  else
    echo "DB UI is STOPPED."
  fi
}

logs() {
  if [ ! -f "$LOG_FILE" ]; then
    echo "No log file yet: $LOG_FILE" >&2
    exit 1
  fi
  tail -f "$LOG_FILE"
}

cmd="${1:-}"
[ $# -gt 0 ] && shift || true
# Allow "start -- <java args>": drop a leading "--" separator.
[ "${1:-}" = "--" ] && shift || true

case "$cmd" in
  start)   start "$@" ;;
  stop)    stop ;;
  restart) stop; start "$@" ;;
  status)  status ;;
  logs)    logs ;;
  *)
    echo "Usage: $0 {start|stop|restart|status|logs} [-- <extra java args>]" >&2
    exit 2
    ;;
esac
