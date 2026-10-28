# Evaluating agreements: printed fingerings, our trails, string/pitch patterns

Code: `src/computingmusic/eval/` (NoteByNote, Evaluate). Run: `make eval FILE=score.musicxml` (tools/sf/eval/run_eval.sh), report in `~/.string_fingering/eval/NAME.report.txt`. No window, no JJ if the dump is cached.

## Ground rules
- Stretches start at ONE item and are extended note by note (the app's Extend step). The grouped arpeggio start of `TrailRun.run` is NOT used (EH has no rule that arpeggios are chords); it appears only in M0 for comparison.
- "Live trails" = all trails alive at the end of a stretch. A note's *candidates* = its (string, height) over all live trails; *determined* = exactly one. No trail is chosen as "the fingering".
- Strings: G D A E = 0..3. Height = semitones above the open string. "Bowing" here means only which string.
- The engine has NO finger numbers. A printed digit can be compared with a trail only through (open vs stopped) and (order of fingers by height). Nothing finer is measurable without a finger-to-height model (open question 1).

## Measures (n = notes, T = live trails)
**M0 Ambiguity.** Live trails per stretch under each start rule. Input: stretches. `O(stretches)`. Tells: how much any later statement is a statement about "the" fingering. Does not tell: which trail is right.

**M1 Printed digit vs trails, per note.** Printed 0 means height 0; printed 1-4 means height > 0. Per note: every candidate agrees / some / none. Input: printed single digits, candidates. `O(n T)`. Tells: whether a disagreement is forced (none) or avoidable (some). Does not tell: anything about which finger.

**M2 Printed digits vs trails, per stretch.** Number of live trails satisfying OPEN (M1) and ORDER at all printed notes of the stretch (ORDER, from PrintedCheck, unvalidated: a higher digit is not lower than a lower digit; 1 semitone slack across instants, none within one). `O(T p^2)` per stretch, p = printed notes. Tells: whether the printed fingering is among the engine's possibilities. Does not tell: the rules are mine; "none passes" may mean ORDER is wrong.

**M3 String for the same pitch.** For each pitch: strings used by its determined notes; whether one string lies in all occurrences' candidates. `O(n log n)`. Tells: how context-dependent string choice is. Does not tell: whether that is right; a spread is normal when positions change.

**M4 Recurring runs.** Runs of W (=4) consecutive single-note items with identical pitches, grouped. (printed) at positions printed in both occurrences: same digit? (ours) when all notes determined: same string sequence? `O(n W)`. Tells: whether the printed fingering and our strings repeat when the notes repeat. Does not tell: anything about runs with transposition (not done), or chords.

**M5 Kind of disagreement.** For notes with one candidate: printed 0 but we stop it (and on which string), printed 1-4 but we play it open. `O(n)`. Tells: it is the open-string decision, and which string. Does not tell: string/height disagreement for stopped notes (needs question 1).

Candidates not built: string-sequence patterns in the SCORE (strings are ours, not the score's, so such a pattern is ours only; M4 uses pitch runs instead); semitone-transposed runs; a "position offset" (height minus digit) spread per stretch (needs question 1).

## Results: Caprice No. 1 (caprice01_ly2mxml), 1,242 notes, 1,086 items; start rule: NOTE BY NOTE
| measure | result |
|---|---|
| M0 | note by note: 130 stretches, 48 with one trail (the other 82: 2-5 trails 56, 6-30 24, 31-100 2); none impossible, none at cap. Grouped start: 138 stretches, 94 with one trail |
| M1 | 249 printed single digits (+3 two-number, not read): every candidate agrees 225, some 14, none 10 |
| M2 | 88 stretches have printed digits: all live trails pass 48, some pass 23, none 17; trails passing 182 of 341 |
| M3 | 45 pitches; 23 have determined notes on more than one string (e.g. E: G x37, D x20; e: A x14, E x70); 824 of 1,242 notes determined |
| M4 | 178 recurring 4-note runs. Printed: 52 occurrence pairs share printed positions; same digit 85, different 3 (E B ^g e': 1 vs 2 in m7/m56/m60; _g _e' _e' _g: 3 vs 1 in m43/m59). Ours: of 67 runs with >=2 determined occurrences, 65 same string sequence, 2 differ; 111 runs have <2 determined occurrences |
| M5 | printed 0, ours stopped: 3 (A x2: ours D at 7; e x1: ours A at 7). Printed 1-4, ours open: 7 (A printed 3 x2; e printed 3 x3, 1 x1, 4 x1) |

## Depends on / not verified
- The 48 one-trail stretches match the app's Extend count EH was given, but TopologyAnchor (computed and unused in Extend) is omitted, and the 2-30 trail stretches are not independently checked against the app.
- M2 "none passes" (17 stretches) mixes real disagreement with the unvalidated ORDER rule. M1/M4/M5 use OPEN only and are the safer numbers.
- M4 determined-run counts depend on the start rule (more determined notes with the grouped start, which prunes trails).
- Reflection is used on FingerTrail (package-private constructor, Trailpoint fields); a public accessor would remove that.

## Open questions for EH
1. May a finger-to-height model be proposed (e.g. in first position finger n sits n or n+1 semitones above the open string, shifted by position)? Without it printed 1-4 cannot be compared with heights.
2. Is the OPEN rule (printed 0 = height 0) the only rule you accept? Is ORDER acceptable, with 1 semitone slack?
3. Should the 3 + 7 open-string disagreements be read as ours wrong, printed wrong, or a position shift?
4. Is a run of identical pitches the right unit for M4, and what W? Should transposed runs count?
5. When a stretch has several trails, should a measure report the range over trails (as here), or be restricted to determined notes?
6. Other fingering sources to bring in (other editions, a player's)? Same measures apply per source.

## 2026-10-28 The trail search requires the finger a trill needs
A trill is held by two fingers: the trilled note's and the one a tone above it on the same string. Before, only the post-hoc finger assignment (PieceSolver) reserved that height; the search could accept a fingering whose frame could not also hold it. Now `FingerTrail.makeCopyWithNewFingering(f, trilledPitches)` adds, for each note of f whose pitch is trilled (`JJNote.trill()`, carried per item in `TrailRun.Items.trilled`, passed through `NoteByNote.run(ins, chords, trilled)`), an auxiliary stopped height h+2 (open string: 2) to the trail (`auxHeightSet`, copied by `copy()`, no note added). `frameFits`, the 4-distinct-heights rule (`bad()`) and `Constraints.HAND` all see stopped heights plus auxiliary ones (`frameHeights()`); the extension is refused if they fail, or if h+2 is above the string's highest height. The comfort cost in `within` also sees them. `PieceSolver.solve(ins, items)` is unchanged.

Regression check: `computingmusic.eval.TrillCheck` (headless; Caprice 3): (a) item 91 (measure 24, a trilled unison double stop E4 on G and on D) starts 2 trails without the trill's finger and 0 with it, and no trail of the item before extends; (b) measure 2's chord still gets lower finger 1, trilled top finger 3. Both pass.

| Caprice | | stretches | items with no fingering | forced stretches | whole-piece cost | our finger agrees with printed (of single digits) |
|---|---|---|---|---|---|---|
| 1 | before | 126 | 0 | 40 | 336.4 | 119 of 249 |
| 1 | after | 126 | 0 | 40 | 336.4 | 119 of 249 (no trilled note) |
| 2 | before | 125 | 0 | 12 | 270.7 | 130 of 401 |
| 2 | after | 125 | 0 | 12 | 274.1 | 137 of 401 |
| 3 | before | 108 | 0 | 11 | 240.3 | 85 of 195 |
| 3 | after | 116 | 1 (item 91) | 18 | 254.9 | 90 of 193 (2 notes unplaced) |

Not verified: the unison double trill of Caprice 3 measure 24 is now unplayable in the model (four distinct heights spread over G and D); whether it is played so in the score was not checked. Caprice 3's cost is not comparable before/after (a different number of stretches and one item unfingered). A trill's finger is assumed a tone up (JJ does not say tone or semitone).
