#!/bin/sh
# Run computingmusic.eval.Evaluate on a MusicXML file with no window; the report goes to ~/.string_fingering/eval/.
# Usage: run_eval.sh FILE.musicxml [WINDOW]   (run from anywhere; JJ errors cannot jump the editor: nojump is first on PATH)
APP="$HOME/shape/fingering"
ABC="${ABCMAINE_JAR:-$HOME/abc/abcMaine.jar}"
OUT="$HOME/.string_fingering/eval"
mkdir -p "$OUT"
FILE="${1:-$HOME/libmusic/Paganini/mutopia_caprice01/caprice01_ly2mxml.musicxml}"
NAME=$(basename "$FILE" | sed 's/\.[^.]*$//')
cd "$APP" && PATH="$APP/tools/jj/nojump:$PATH" java -Xmx3g -Djava.awt.headless=true \
  -cp "build/classes:$ABC" computingmusic.eval.Evaluate "$FILE" ${2:+"$2"} > "$OUT/$NAME.report.txt" 2>"$OUT/$NAME.err.txt"
echo "exit $?  report: $OUT/$NAME.report.txt"
