#!/bin/sh
# A private copy of the String Fingering window on a virtual display, controllable with sfctl.py, and
# invisible on the real desktop.  Usage:  private_window.sh start | stop | status
# It records the process ids it starts and stops exactly those (never by pattern).
STATE="$HOME/.string_fingering/private"
APP="$HOME/shape/fingering"
ABC="$HOME/abc/abcMaine.jar"
DISP=":77"
mkdir -p "$STATE"
case "$1" in
start)
  [ -f "$STATE/java.pid" ] && kill -0 "$(cat "$STATE/java.pid")" 2>/dev/null && { echo "already running"; exit 0; }
  rm -f "$STATE/control.port"
  Xvfb "$DISP" -screen 0 1700x1000x24 >"$STATE/xvfb.log" 2>&1 &
  echo $! > "$STATE/xvfb.pid"
  sleep 1
  cd "$APP" && DISPLAY="$DISP" java -Xmx3g -Dsf.control.file="$STATE/control.port" -Dsf.recent.file="$STATE/recent.txt" \
    -cp "build/classes:$ABC" computingmusic.BigGui --control >"$STATE/gui.log" 2>&1 &
  echo $! > "$STATE/java.pid"
  for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do
    [ -s "$STATE/control.port" ] && break
    sleep 1
  done
  [ -s "$STATE/control.port" ] && echo "window up; export SF_CONTROL_FILE=$STATE/control.port" || { echo "window did not start; see $STATE/gui.log"; exit 1; }
  ;;
stop)
  for f in java xvfb; do
    [ -f "$STATE/$f.pid" ] && kill "$(cat "$STATE/$f.pid")" 2>/dev/null
    rm -f "$STATE/$f.pid"
  done
  rm -f "$STATE/control.port"
  echo stopped
  ;;
status)
  for f in java xvfb; do
    if [ -f "$STATE/$f.pid" ] && kill -0 "$(cat "$STATE/$f.pid")" 2>/dev/null; then echo "$f running (pid $(cat "$STATE/$f.pid"))"; else echo "$f not running"; fi
  done
  ;;
*) echo "usage: $0 start|stop|status"; exit 2 ;;
esac
