#!/usr/bin/env python3 -I
"""Merge the one-part-per-system export back into a single part.

Printed measure numbers in the margin, such as "(4)", "(7)", "(10)", are read by Audiveris as
part names.  Every system then gets a part of its own, and the export holds 9 parts of 24
measures each, where all but one measure per part is a whole-measure rest.  The book
itself has ONE part along all systems ("1 part along 9 systems" in its log).

merge_parts keeps the first part and fills each of its measures from the part whose measure
holds pitched notes (the first such part; a measure that is empty in every part stays as it
is).  Divisions must agree across parts, otherwise it refuses to merge.
Complexity: O(parts * notes).  Standard library only.
"""
import copy


def pitched(m):
    return any(n.find('rest') is None for n in m.findall('note'))


def merge_parts(root, log):
    parts = root.findall('part')
    if len(parts) < 2:
        return
    divs = {p.findtext('measure/attributes/divisions') for p in parts}
    if len(divs) != 1:
        raise SystemExit('merge_parts: parts disagree on divisions %s' % sorted(divs))
    base = parts[0]
    taken = {}
    for i, m in enumerate(base.findall('measure')):
        num = m.get('number')
        if pitched(m):
            taken[num] = 0
            continue
        for k, p in enumerate(parts[1:], 1):
            src = next((x for x in p.findall('measure') if x.get('number') == num), None)
            if src is not None and pitched(src):
                new = copy.deepcopy(src)
                base.remove(m)
                base.insert(i, new)
                taken[num] = k
                break
    for p in parts[1:]:
        root.remove(p)
    pl = root.find('part-list')
    if pl is not None:
        sp = pl.findall('score-part')
        for s in sp[1:]:
            pl.remove(s)
    log.append('parts: merged %d parts into one; %d measures taken from other parts, %d empty in all'
               % (len(parts), sum(1 for v in taken.values() if v), len(base.findall('measure')) - len(taken)))
