ABCMAINE_JAR ?= $(HOME)/abc/abcMaine.jar
BUILD_DIR := build/classes
MAIN_CLASS := computingmusic.BigGui
SOURCES := $(shell find src -name '*.java' -print)

.PHONY: build run clean

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
