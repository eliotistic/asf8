# The chord app, built with the SDK's own tools (no Gradle): make -C android apk   ->   android/build/chordapp.apk
# The solver is the repository's own source (src/computingmusic/chord, hand, instrument), not a copy.
SDK ?= $(HOME)/android-sdk
BT  ?= $(SDK)/build-tools/33.0.2
JAR ?= $(SDK)/platforms/android-33/android.jar
CORE := ../src/computingmusic/chord/ChordBoard.java ../src/computingmusic/hand/Geometry.java ../src/computingmusic/hand/HandParams.java \
        ../src/computingmusic/hand/FrameTest.java ../src/computingmusic/instrument/InstrumentSpec.java ../src/computingmusic/AbcPitch.java
APP  := $(wildcard src/computingmusic/chordapp/*.java)
KEY  := $(HOME)/.android/debug.keystore

.PHONY: apk clean
apk: build/chordapp.apk

build/classes.stamp: $(APP) $(CORE)
	mkdir -p build/classes
	javac --release 8 -cp $(JAR) -d build/classes $(APP) $(CORE)
	touch $@

build/classes.dex: build/classes.stamp
	mkdir -p build
	$(BT)/d8 --release --min-api 26 --lib $(JAR) --output build $$(find build/classes -name '*.class')

build/base.apk: AndroidManifest.xml
	mkdir -p build
	$(BT)/aapt2 link -o $@ --manifest AndroidManifest.xml -I $(JAR) --min-sdk-version 26 --target-sdk-version 33

build/chordapp.apk: build/base.apk build/classes.dex
	cp build/base.apk build/unaligned.apk
	cd build && zip -q -j unaligned.apk classes.dex
	$(BT)/zipalign -f 4 build/unaligned.apk build/aligned.apk
	@test -f $(KEY) || { mkdir -p $(dir $(KEY)); keytool -genkeypair -keystore $(KEY) -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"; }
	$(BT)/apksigner sign --ks $(KEY) --ks-pass pass:android --key-pass pass:android --out $@ build/aligned.apk
	@echo built $@

clean:
	@echo "remove android/build yourself (nothing here deletes it)"
