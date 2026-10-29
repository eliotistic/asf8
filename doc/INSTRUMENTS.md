# Instruments

The instrument is a parameter: one `computingmusic.instrument.InstrumentSpec` holds everything about it, and the engine
(`Instrument`), the hand model (`hand.Geometry`), the window and the tools are made from it. Registry:
`InstrumentSpec.ALL` (menu order) and `InstrumentSpec.INSTRUMENTS` (by name: `violin`, `db`, `double bass`...).
Pitches are sounding semitones, C=60; strings are numbered from the lowest (0).

| | Violin | Viola | Cello | Double bass | Guitar |
|---|---|---|---|---|---|
| strings, low to high | G D A E | C G D A | C G D A | E A D G | E A D G B e (also `guitar-dropd`: D A D G B e) |
| open pitches | 55 62 69 76 | 48 55 62 69 | 36 43 50 57 | 28 33 38 43 | 40 45 50 55 59 64 (drop D: 38 45 ...) |
| tuning | standard | standard | standard | CHOSEN as fourths, sounding | standard, sounding |
| max height per string (semitones; guitar: frets) | 24 28 32 35 (project limits, `SFProperties.xml`) | CHOSEN 24 28 32 35 | CHOSEN 12 12 12 30 (neck; thumb on A only) | CHOSEN 12 12 12 24 (neck; thumb on G only) | CHOSEN 22 x6 |
| stop length mm | 325 (sources) | 370 CHOSEN | 690 CHOSEN | 1060 CHOSEN | 650 CHOSEN |
| string spacing nut / bridge mm | 5.5 / 11.2 (sources) | 5.8 / 11.8 CHOSEN | 8.0 / 16.0 CHOSEN | 11.0 / 22.0 CHOSEN | 7.3 / 10.5 CHOSEN |
| largest chord | 4 | 4 | 4 | 4 | 6 |
| chord on neighbouring strings | yes | yes | yes | yes | no |
| fingers of the left hand | 4 | 4 | 4 | 4 | 4 |
| neck limit (height above which the thumb position is needed; reported, not modelled) | - | - | 12 (VERIFIED start of thumb position, extended to every string: CHOSEN) | 12 (same) | - |

Measured means "from a source" (see `HAND_MODEL.md` for the violin's); CHOSEN is a placeholder that is meant to be replaced, and
says so in the spec's `*Provenance` strings. Spacing is between neighbouring strings, centre to centre. `HandParams` has per-instrument limits since phase 3 (below); the violin's are unchanged. Without a hand model
(`Constraints.HAND == null`) the fixed span `Instrument.handSpan` (violin: `SFProperties.xml`; others: the same number) applies and is
not calibrated for the other instruments: use the hand model (it is what `solve.Solve` uses).

## Using an instrument

- Window: Instrument menu (built from the registry). Switching clears the boards, draws the new string count, and reads an open
  score again for the instrument. `gui_state` reports `instrument`; `gui_menu path=Instrument/Cello` selects it.
- Tools: `-Dsf.instrument=cello` for every main under `solve`, `eval`, `hand.Calibrate`, `TrailRun`; `solve.Solve` and `solve.Verify` also
  take it as the third argument (`Solve FILE 1.0 cello`). The violin's files are `NAME.solution.json`; another instrument's
  `NAME.vc.solution.json` (suffix = short name), so a cello run never overwrites a violin one.
- Server: `load` takes `"instrument"`; the command `instrument` (`"name"`) changes it for the loaded piece; `constraints` names it.
- Code: `TrailRun.violin()` (the default), `TrailRun.instrument(spec)`, `TrailRun.instrumentNamed(name)`, `new Instrument(spec)`.
  Per-instrument limits are `Instrument.maxHeights/minHeights/openAllowed/playable/handSpan`; `Constraints.VIOLIN_*` are only the
  violin's defaults read from `SFProperties.xml`.

## Adding an instrument

1. Add a `static final InstrumentSpec` to `InstrumentSpec` and put it in the `ALL` list (strings low to high, open pitches, max heights, stop length,
   spacing, chord rules, provenance strings). The enum `Instrument.Instr` needs an entry with the same short name.
2. `hand.Geometry` aliases and the Hand frame follow the registry by themselves; `FMenuBar`, the boards, `Solve`, `Verify` and the server need no change.
3. Run `make check-instruments` and `python3 -I tools/sf/test_instruments.py`.

## The guitar (phase 2)

Registry entries `Guitar` (`guitar`, short name GT) and `Guitar (drop D)` (`guitar-dropd`, GD; 6th string D2 = 38, otherwise identical;
Sor op. 1 no. 3 asks for it, op. 2 no. 2 is inferred). Solution files: `NAME.gt.solution.json`, `NAME.gd.solution.json`.
Spec flags that switch the guitar's behaviour on (the bowed instruments have them false, so their results do not change):
`chordOnNeighbouringStrings=false`, `heldNotesKeepFingers`, `barreCoversStrings`, `lowestFingersFirst`.

**Chords** (`ChordSolver`): an assignment search, notes to DISTINCT strings, each note on a string whose range reaches it (open string = height 0,
up to fret 22). Depth first over the strings; no neighbouring-strings rule, no bow window (`Instrument.inSpan` is not applied: the hand model's
frame test in the trail decides what the hand can hold). Of two notes of one pitch the lower-numbered takes the lower string. At most S!/(S-n)!
assignments, in practice a few dozen to a few hundred for 6 notes. For the bowed instruments the same search runs window by window and produces the
old permutation tables' order exactly (violin results are identical). Chords up to `maxChordSize` 6.

**Hand** (`HandParams.forInstrument(spec)`): per-instrument reach limits, mm between two fingertips, per pair 12 13 14 23 24 34, in the spec.
Violin: 34 62 85 34 60 30 comfortable; 52 95 125 50 90 46 absolute. Viola, cello, bass: see Phase 3 below. Guitar, all CHOSEN: comfortable
68 100 128 68 98 66, absolute 100 130 158 100 128 82 (fret 1-5 is 127 mm, fret 1-6 154 mm on 650 mm: the outer pair reaches four frets from fret 1
comfortably, five at the limit; neighbouring fingers two/three frets; ring-little weakest). `FrameTest` uses the instrument's. The frame is a set of
distinct stopped heights, one finger each, so a BARRE (one finger over several strings at one height) is one finger already: F major 133211 is three
heights. A barre lies flat only if no string between its strings sounds lower: otherwise (G major 320003: fret 3 on the low E and the high e, open strings
between) the parts are separate fingers, and the height appears once for each in the frame (`FingerTrail.countBarreFingers`). Among assignments of fingers
the guitar takes the lowest numbers within the comfortable limits (`lowestFingersFirst`); the bowed instruments the least strained. The stretch
cost has a small preference for low positions on the guitar (`Costs.highPosition` 0.02 per fret, CHOSEN, a tie-break). Four fingers; the THUMB IS IGNORED.

**Held notes**: a note whose onset + duration exceeds a later item's onset is still sounding then (`TrailRun.Items.sustained`, copied to
`ChordFingering.heldBefore` as rows {earlier item, pitch}; computed only when `heldNotesKeepFingers`). The trail remembers where it put every item's notes
(`FingerTrail.placed`) and refuses a later note on the held note's string at any other height (open string held: the string only). The held finger
stays down because its height is in the stretch's frame, with its number. At a change of hand position (the end of a stretch) the held stopped notes are carried:
the next stretch's trails are seeded by each distinct way the previous trails placed them (`NoteByNote.start`, `FingerTrail.seededBy`), the held heights are in the
new frame (the hand cannot shift under a held stopped note, an item that only a shift allows gets no fingering), `PieceSolver` links only compatible options, and
the held finger keeps its number (`FrameTest.assign` with pinned fingers; if the hand does not allow it the count `held fingers that could not keep their number`
rises). If the next item has no seeded start but has one without the held notes, the previous stretch is ended one item earlier and tried again (greedy cut with backtracking).
Two notes of one pitch and one onset (two voices on a unison) are ONE note for the guitar (`TrailRun.mergeUnisons`; Verify's "notes placed" is then below the score's count).

**Not modelled**: right hand (fingers p i m a, string crossing, free/rest strokes); the thumb of the fretting hand (bass notes fretted over the neck) and the
different roles of fingers in a barre (any finger may barre here); ligados (hammer-ons, pull-offs), slides, harmonics, capo; alternative tunings except drop D; written durations are
taken as sounding durations, but Sor's notated long notes are often not held in performance, so a held note that is nominal can block an item that a player would
simply play (the reason is printed by `Solve`: `[held notes: ...]`); the GUI path (`TrailRun.run`, `AnchorSequencer`, the Extend button) uses the held notes inside a stretch but
does not carry them across a stretch break; no preference for open strings; the topology shapes and the board's clef/semitone scale are still the violin's.

Tests: `make check-guitar` or `python3 -I tools/sf/test_guitar.py` (`computingmusic.eval.GuitarCheck`: C, E, Am, G, F barre, unplayable chord, six-note chord, held bass,
a held note that forbids a shift, a held finger across stretches; and the Sor files if converted).

## State

Done (phase 1): the registry and spec; geometry derived from it; per-instrument limits in `FingerTrail`, `Instrument`, `Verify`, `SFModel`;
string count in `ChordSolver`, `ChordFingering.StringMatrix`, `Bowing`, `FingerTrail`, `FingerBoard` (strings spread evenly: 6 strings
display), tooltips (finger numbers, string names, mm from the instrument's own geometry); chord size from the spec (`FingerSeq2`, `Cfinger2`).
Violin results are identical (regression on Caprices 1-3 and the Reger sonata, also after phase 2).

Done (phase 2, the guitar): above. Still open: `Topology`/`TopologyAnchor` (bar, diamond, triangle shapes) are violin-shaped and not reviewed for the other
instruments; `Instrument.getClef` for the bass and guitar (written an octave above sounding) and the board's semitone scale are the violin's.

## Phase 3: viola, cello, double bass (2026-10-29)

Reach limits, mm between two fingertips, pairs 12 13 14 23 24 34 (`InstrumentSpec.*_REACH`, provenance strings in the spec). All three are CHOSEN from the figures below.

| | comfortable | absolute |
|---|---|---|
| violin (unchanged) | 34 62 85 34 60 30 | 52 95 125 50 90 46 |
| viola: the violin's table x1.10 | 37 68 94 37 66 33 | 57 105 138 55 99 51 |
| cello | 70 100 130 70 100 36 | 80 145 175 100 140 62 |
| double bass | 60 100 110 60 60 56 | 105 150 160 105 105 100 |

Sources. VERIFIED: https://cellofun.eu/home/cello-blog-encyclopaedia/mechanics-technique/left-hand-homepage-a-musician-is-an-athlete-of-the-small-muscles/left-hand-finger-spacings/ gives, for the cello, a semitone in first position of at most 4 cm (the geometry's 690 mm stop gives 35-39 mm: agrees), a whole tone Bb-C ~7 cm (geometry 71 mm: agrees), and says the 7 cm tone is "routinely" stretched between fingers 1 and 2 in extended first position. VERIFIED (Wikipedia, https://en.wikipedia.org/wiki/Thumb_position): the cello's thumb position is "introduced on the second or half-string harmonic", the A on the A string (height 12); for the bass thumb position is used above the one-lined G (the G string's harmonic; the page is itself uncited). VERIFIED in secondary sources only (https://www.talkbass.com/threads/crab-claw-left-hand-technique.174181/, https://researchcatalogue.net/view/1053899/1183158): Simandl's half and first positions put fingers 1, 2, 4 on three consecutive semitones, the third finger skipped. Viola: only that violists report a "little wider" spacing than on the violin (https://fiddlerman.com/forum/viola-talk/string-spacing); no figure found. No measured span in mm or semitones per position was found for the cello, bass or viola (Rolland, Starker, Simandl, Rabbath not obtainable online): every number in the table is CHOSEN.
Reasoning (cello): the normal first-position hand (1-2 semitone, 2-3 tone, 3-4 semitone) covers 4 semitones from finger 1 to 4 = 127 mm at height 2 (comfortable 14 = 130); the other comfortable limits are the same semitone counts in mm at the nut end; comfortable 12 = 70 because the source calls the tone stretch routine; absolute limits are the extended hand measured from the nut (1-3: 4 semitones = 142 mm, 1-4: 5 = 173 mm). The first values (absolute 13 130, 14 165) made the Suite 3 gigue's bariolage Bb-C-D-Eb (heights 1 3 5 6, 163 mm) unplayable on the A string, and the solver took it high up the G string: raised.
Reasoning (bass): a semitone is 50-60 mm at 1060 mm; fingers 1-2-4 cover 3 consecutive semitones (1-4 = 2 semitones ~ 105 mm). Comfortable 13 (100 mm) is just below two semitones so that a frame of three consecutive heights is held by fingers 1 2 4 (the engine takes the least strained assignment; `StringsCheck` asserts it), wider frames are extensions (absolute).

Heights. Cello C G D 12, A 30 (the fingerboard is ~570 mm of 690 = ~31 semitones); bass E A D 12, G 24 (~860 of 1060 = ~28): CHOSEN. The neck stops at 12 and the thumb position is modelled on the top string only: a lower string has no height above 12. With 24-28 on the lower strings the solver took bariolages high up the D or G string (frame cost does not know that is not a neck position). The hand frame knows nothing of the thumb (a fifth finger): on the top string, heights above `InstrumentSpec.neckLimitHeight` (12) are fingered as if with four fingers and **reported** by `Solve` ("items with a note stopped above height 12 (thumb position, not modelled)"); `PieceSolver.Costs.aboveNeck` (3.0 per stretch, CHOSEN) makes a stretch with such a height cost more than one without.
Costs for the bowed instruments other than the violin: `highPosition` 0.02 per semitone of the lowest stopped height (a tie-break for low position, as the guitar), `stoppedAnOpenPitch` 0.25 for each distinct stopped place that sounds the pitch of an open string (without it the Suite 1 prelude's G2 was stopped on the C string, seven semitones up, to save two finger lifts). Both CHOSEN; the violin's and the guitar's results are identical.
`Solve` for these instruments also prints, per item with no fingering, the reason (`range: ...` a note below the lowest string, above the neck, or notes that cannot take distinct strings; `hand reach`), the number of stretches whose frame the comfortable limits do not allow (the older "beyond comfort" count is cost-based and includes lifts and bariolations), and the thumb report. `-Dsf.handlimits=violin` runs any instrument with the violin's table (to see what the instrument's own limits change). The instrument panel's range label now shows the instrument's own range in ABC ("Cello: C,, - ^d'"); it showed the violin's range with MIDI-style names. Switching instrument clears the stretches Solve all had marked.
Tests: `python3 -I tools/sf/test_cello.py` (`computingmusic.eval.StringsCheck`, the Suite 1 prelude on cello and viola, the Brey cadenza's thumb report, the violin summaries of Caprice 1 and the Reger sonata identical to the ones recorded before).

Results on real music: see the table in the report of 2026-10-29 (`~/.string_fingering/cello/table_v8.md`): cello suites 1-3 (20 movements) and the viola transpositions of 1 and 3 (13) all have a fingering for every item, with no thumb position, except Suite 2 sarabande measure 5 (a chord D2 F2 A2: D and F are only on the C string; not verified against the printed score). With the violin's hand limits the cello needed 15-66 stretches against 7-53 (frames outside the comfortable limits 15 against 3 in the Suite 1 prelude).
Not done: thumb position as a hand (thumb as a finger above height 12, the cello's thumb-position patterns); double stops above the neck; the bass's third finger is only discouraged, not forbidden; the board's semitone scale, `Topology` shapes and the clef for the bass (written an octave above sounding) are still the violin's; cello fingerings are not compared with a printed edition (none was available).
