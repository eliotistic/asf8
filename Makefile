ABCMAINE_JAR ?= $(HOME)/abc/abcMaine.jar
BUILD_DIR := build/classes
MAIN_CLASS := computingmusic.BigGui
SOURCES := $(shell find src -name '*.java' -print)

.PHONY: build run clean notation run-control server hand

build:
	@test -f "$(ABCMAINE_JAR)" || { \
		echo "Missing abcMaine.jar at $(ABCMAINE_JAR)" >&2; \
		echo "Set ABCMAINE_JAR=/path/to/abcMaine.jar and try again." >&2; \
		exit 1; \
	}
	mkdir -p "$(BUILD_DIR)"
	javac -cp "$(ABCMAINE_JAR)" -d "$(BUILD_DIR)" $(SOURCES)
	cp -R src/resource "$(BUILD_DIR)/"

run: build
	java -cp "$(BUILD_DIR):$(ABCMAINE_JAR)" $(MAIN_CLASS)

clean:
	rm -rf build

# The score as Verovio draws it, with the data of each note.  make notation FILE=score.musicxml
notation: build
	java -cp "$(BUILD_DIR):$(ABCMAINE_JAR)" computingmusic.jj.NotationFrame $(FILE)

# The window, with a control port that another program can use (tools/sf/sfctl.py, see GuiControl.java).
run-control: build
	java -Xmx3g -cp "$(BUILD_DIR):$(ABCMAINE_JAR)" $(MAIN_CLASS) --control

# String Fingering with no window, JSON lines on standard input and output (see SFServer.java).
server: build
	java -Xmx3g -cp "$(BUILD_DIR):$(ABCMAINE_JAR)" computingmusic.SFServer

# Agreements between printed fingerings, trails and string patterns, no window (doc/EVALUATION.md).
# make eval FILE=score.musicxml   -> ~/.string_fingering/eval/NAME.report.txt
eval: build
	sh tools/sf/eval/run_eval.sh "$(FILE)"

# The hand model: place the fingers on a board to scale and set the freedoms of the hand (doc/HAND_MODEL.md).
hand: build
	java -cp "$(BUILD_DIR):$(ABCMAINE_JAR)" computingmusic.hand.HandFrame
