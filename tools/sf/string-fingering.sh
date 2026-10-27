#!/bin/sh
# Start the String Fingering window (with its control port, see GuiControl.java).  This is what the menu
# entry and the desktop icon run.  It builds first if there is nothing built yet.
APP="$HOME/shape/fingering"
ABC="$HOME/abc/abcMaine.jar"
LOG="$HOME/.string_fingering/sf.log"
mkdir -p "$HOME/.string_fingering"
cd "$APP" || exit 1
[ -d build/classes/computingmusic ] || make build >"$LOG" 2>&1
exec java -Xmx3g -cp "build/classes:$ABC" computingmusic.BigGui --control "$@" >>"$LOG" 2>&1
