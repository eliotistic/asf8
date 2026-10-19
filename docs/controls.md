# String Fingering controls

This guide consolidates the surviving 2011 README, `jbjournal.txt`,
`jbdocumentation.txt`, and the behavior implemented by the current event
handlers. The handoff document mentions a `schema.png`, but that image is not
present in the repository or its Git history.

## Opening a score

Drop one `.abc` file anywhere on the application window. The program draws the
score, computes fingerings, and shows the filename in the window title.

The File menu provides two alternatives:

- **Analyze ABC file** draws the score and computes its fingerings.
- **Draw ABC, no analysis** draws the notation without running the fingering
  analysis.

## Manual chord controls

The upper-left entry panel analyzes a chord independently of the open score.

- **Pitch field** accepts up to four space-separated pitch names, such as
  `g5 bf5 d6`. Press Enter or click **finger** to analyze them.
- **finger** finds and displays the possible fingerings.
- **Previous/next arrows** browse the alternative fingering solutions.
- **Random** generates and analyzes a chord. The Random menu chooses an
  ordinary chord or a four-note stop.
- **Clear** clears the manual input and its displayed results.

## Score navigation

The four arrows below the score mean beginning, previous event, next event,
and end. Right-clicking the next arrow jumps forward by the length of the
current extended passage.

After clicking either arrow group, the keyboard Left and Right Arrow keys
control that group. A black border shows which group has focus.

## Fingering trails

A trail is a physically compatible sequence of fingerings through consecutive
score events.

- **Extend** adds the next event to the current trail. It keeps the current
  fingering where possible; otherwise it selects a compatible trail and adapts
  the earlier fingerboard panels.
- **Shrink** removes the final event from the current extended trail.
- **Solve** extends all compatible trails forward as far as possible and marks
  their spans on the score.
- **Clear solutions** removes those blue annotations and returns to the current
  anchor.
- **Positions** displays the hand-position sequences available at the current
  score event. Its small arrows browse those sequences.

## Fingerboard panels

The panels show the possible placements on the instrument strings. Multiple
panels represent a trail through a passage.

- Double-click a panel to remove it and every panel to its right.
- **Boards -> Reset Score and Panels** returns to the first score event and
  clears the fingerboards, blue Solve annotations, and position highlighting.
- **Glue** locks the displayed fingering and removes alternatives that are
  incompatible with it.
- **Unglue** removes that lock and rebuilds the alternatives.
- **Show glue** is a legacy diagnostic that prints the locked score locations
  to the terminal.

## Menus

- **Instrument** selects violin, viola, or cello tuning and range.
- **Topology -> Filter bad topologies** rejects fingerboard shapes that violate
  the configured topology rules.
- **Positions -> Manual** enables manual position browsing. **Auto Max cursor**
  follows the maximum available position as the score cursor moves.
- **Arpeggiation -> Longest here** shows the longest compatible arpeggiated
  passage beginning at the cursor. **Jump to new longest** changes navigation
  to jump between changes in that passage.
- **Anchors -> On** preserves compatible trails while navigating and extending.
- **Properties -> Reload Properties** reloads `SFProperties.xml`. **Reset
  Properties** replaces it with the built-in defaults before reloading.

## Legacy and unfinished controls

- **Freeze** captures the current extension in memory and advances, but saving
  that capture is unfinished.
- **Load** is not implemented.
- The Solve menu is not connected; use the **Solve** button below the score.
- Several experimental extension checkboxes exist in the source but are not
  currently displayed.
