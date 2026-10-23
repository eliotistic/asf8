# Audiveris 5.x editing crib sheet (from the manual, read 2026-10-21)

Manual: https://audiveris.github.io/audiveris/ — pages cited in brackets.

## Views
- Sheet tabs: **Binary** (thresholded ink, never affected by display mode) and **Data**.
- **F12** cycles the Data display mode: Physical (pale blue vertical / pale pink horizontal
  pixel sections + inters), Combined (inters translucent over pixels — best for spotting
  discrepancies), Logical (inters only). [display_modes]
- **F11** cycles selection mode: glyph+inter (default), inter-only, section-only. [selection]
- **F9** (View > Show score errors): pink measure background = rhythm problem. [inspection]
- **F7** (View > Show jumbo inters): augmentation dots drawn as big red dots — missing or false
  dots are the most frequent cause of rhythm errors. [inspection, tips]
- View > Show chord IDs; right-click a measure > Dump measure voices; right-click a time slot.

## Selecting  [selection]
- Left-click selects (replaces). Left-click on empty space clears. Right-click opens the pop-up.
- Overlapping glyphs: the smallest wins. Note inside a chord: the note wins; Inter board
  "To Ensemble" (or Inters pop-up) selects the chord.
- **Shift+drag** = lasso (only entities fully inside). **Ctrl+click** adds/removes.
- Alt+arrows nudge the location point one pixel. Select by ID in the Glyph/Inter board spinner.
- Double-click an inter = select + edit mode.

## Deleting  [remove_inter]
- **Delete / Backspace removes selected INTERS only** (a selected bare glyph is ignored).
- Or Inter board **Deassign**, or pop-up "Inters" > delete. Removing an inter removes its
  relations; removing the last member of an ensemble removes the ensemble.

## Adding / replacing  [add_inter, tips]
- Shape palette (right side of sheet): open a shape set, **drag** onto a staff (staff is chosen
  by hovering; drop only works with a staff target). Compound notes drop as head+stem.
- From a glyph: select glyph(s), then double-click a shape in the palette, or pop-up
  Glyphs > shape set, or classifier top-5 buttons.
- **Select all fragments of one symbol with a Shift-lasso, then assign** (a natural sign split in
  two glyphs). Don't catch neighbours (augmentation dots).
- **Delete the wrong interpretation first**: only an *identical* glyph's old inter is replaced
  automatically; glyphs differing by one pixel leave conflicting inters in the output.
- Manual inters are flagged MANUAL and treated as certain.
- Shortcuts: set key then shape key (a,f/n/s accidentals; h,b black head; r,1/2/4/8 rests;
  b,f/h/3 beams...). Repetitive input: Ctrl+Shift+N.
- Relations: **drag from one inter to another** (e.g. slur end -> head). Auto-relations need
  geometry (accidental close, to the right of the head).

## Editing  [edit_inter]
- Double-click (or Edit checkbox in Inter board) shows handles. Drag handle, or Alt+arrows.
  Enter or click elsewhere commits. Undo Ctrl+Z.

## Chords / rhythm  [chords]
- Pop-up > Chords: Split (long stem built one chord from two), Merge (often needed for whole
  notes), Next in Voice, Separate Voices, Same/Separate Time Slots.
- Pop-up Measure > Reprocess rhythm after fixing.

## Text  [text]
- Words are edited per word (type, Enter). Sentence role can be changed (Lyrics, Direction, ...);
  **Fingering is not a text role**.

## Fingering digits  [guides/specific/fingering_plucking]
- Detection is OFF by default. GUI: Book > Set book parameters (fingerings switch).
  Batch: `-constant org.audiveris.omr.sheet.ProcessingSwitches.fingerings=true`.
- Manual: Shape board sets **Fingerings** / **Pluckings** (works regardless of the switch).
- Exported to MusicXML. Digits 0-4 only. Designed for guitar.
- Measured on Popper Op.73 No.17 (300 ppi): switch on found 54 fingerings (38 ones, 9 twos,
  5 threes, 1 four, 1 zero); our own blob reader found ~308 digit-shaped marks.

## Other
- Pop-up Extraction (Binary tab) saves a rectangle of the binary image.
- Octave shift: ClefsAndShifts set; removing one physical shift removes the whole logical one.
- CLI constants: `-constant KEY=VALUE` (Tools > Constants in the GUI).
