# Hand model (open, parameterised)

`make hand` opens the window; `java -cp build/classes:$HOME/abc/abcMaine.jar computingmusic.hand.HandFrame --png FILE` draws the example without a window.

## What it is
Pure geometry in millimetres, no tuning to any piece (`src/computingmusic/hand/`):
- `Geometry` — the board: stop length L, string spacing at nut and bridge (interpolated linearly), open pitches.
  A finger `h` semitones up the string is `L (1 - 2^(-h/12))` from the nut, so semitones close up towards the bridge
  (first semitone about 18.2 mm on L = 325 mm; each next one is shorter by 2^(-1/12)).
- `HandParams` — the freedoms: for each finger pair the largest tip-to-tip distance that is *comfortable* and *absolute*,
  a hand-size factor that scales them, the closest two fingers may be on one string, and whether fingers may cross.
- `HandModel.check` — for fingers placed as (finger, string, height): the tip distance (along the board and across the
  strings) for every pair, the verdict for each (comfortable / possible, beyond comfort / impossible) and the worst of them.

## Status of the numbers
Every limit is a CHOSEN placeholder. The survey (see the session report, 2026-10-27) found no published measurements
of violin finger extension in mm; only weak or non-violin figures (finger splay from a patent, pianists' spans,
enslaving force data). Geometry for the violin comes from luthier/forum sources (stop 325 mm; outer strings 16-16.5 mm
at the nut, 33-34 mm at the bridge); viola, cello and bass geometry are chosen and unverified; their reach limits are per instrument since phase 3 (INSTRUMENTS.md).

## Not done yet
- Not wired into the trail engine (`FingerTrail` still uses the fixed 6-semitone frame); doing so replaces that frame.
- Finger numbers are placed by hand in the window, not deduced.
- No shift or preparation timing; no coupling cost between fingers; no sizes from a hand-size database (ANSUR II).
- SLIDING with one finger (portamento, a finger gliding from one pitch to another without lifting): a technique the
  model does not know. Probably it matters mostly as a way of CHOOSING hand position (a slide is a shift that keeps
  a finger down), so it belongs with the shift-cost work, not with the frame test. Noted 2026-10-27, EH: "important
  technique ... probably about choosing hand position, so perhaps not so important now".

