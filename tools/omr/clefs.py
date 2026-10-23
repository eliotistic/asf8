#!/usr/bin/env python3 -I
"""Put the clefs of the master book back where the printed page has them.

Audiveris' MusicXML export loses clef changes inside a measure: a mid-measure clef is
written at the start of the measure and a clef at the end of a measure after a
`backup`, so it lands mid-measure.  The pitches are right (Audiveris reads each
note against the clef geometrically in force) but MuseScore draws them against the
wrong clef.  The book itself has every clef with its position, so the export's
clefs are discarded and rebuilt from the book:

  - each clef inter belongs to the stack (measure) containing its centre;
  - its time in the measure is the start of the first note whose head is to the
    right of the clef; a clef after the last note (or in the system's cautionary
    stack) takes effect at the start of the next measure;
  - a clef is only written when it differs from the clef in force.

Usage (library):  restore_clefs(root, omr_path, log)   root = ElementTree root of the export.
Complexity: O(notes + clefs).  Standard library only.
"""
import xml.etree.ElementTree as ET
import zipfile

SIGN_LINE = {'BASS': ('F', '4'), 'TENOR': ('C', '4'), 'ALTO': ('C', '3'), 'TREBLE': ('G', '2'),
             'BARITONE': ('C', '5'), 'SOPRANO': ('C', '1'), 'MEZZO_SOPRANO': ('C', '2')}


def book_clefs(omr):
    """[(measure_number, order_key, x_left_px, x_centre_px, interline, stack_left, kind, pushed)]
    where pushed means the clef belongs at the start of the next measure."""
    with zipfile.ZipFile(omr) as z:
        root = ET.fromstring(z.read('sheet#1/sheet#1.xml'))
    il = float(root.find('.//interline').get('main'))
    out = []
    for si, system in enumerate(root.iter('system')):
        stacks = [(int(s.get('id')), int(s.get('left')), int(s.get('right')), s.get('special'))
                  for s in system.findall('stack')]
        real = [s for s in stacks if s[3] is None]
        if not real:
            continue
        for c in system.iter('clef'):
            b = c.find('bounds')
            if b is None or c.get('kind') is None:   # not a clef inter (e.g. a reference)
                continue
            x, w = int(b.get('x')), int(b.get('w'))
            cx = x + w / 2.0
            if cx < real[0][1]:
                num, pushed = real[0][0], False
            else:
                num, pushed = None, False
                for sid, left, right, _ in real:
                    if left <= cx < right:
                        num = sid
                        break
                if num is None:             # right of the last barline: cautionary clef
                    num, pushed = real[-1][0], True
            stack_left = [s[1] for s in real if s[0] == num][0]
            out.append((num, (si, x), x, cx, il, stack_left, c.get('kind'), pushed))
    return out


def timeline(m):
    """[(note, start, x_tenths)] for non-chord notes of a measure in document order."""
    cur, out = 0, []
    for e in m:
        if e.tag == 'note':
            if e.find('chord') is None:
                dx = e.get('default-x')
                out.append((e, cur, float(dx) if dx is not None else None))
                if e.find('grace') is None:
                    cur += int(e.findtext('duration', '0'))
        elif e.tag == 'backup':
            cur -= int(e.findtext('duration'))
        elif e.tag == 'forward':
            cur += int(e.findtext('duration'))
    return out


def clef_element(kind):
    sign, line = SIGN_LINE[kind]
    a = ET.Element('attributes')
    c = ET.SubElement(a, 'clef')
    ET.SubElement(c, 'sign').text = sign
    ET.SubElement(c, 'line').text = line
    return a


def restore_clefs(root, omr, log):
    part = root.find('part')
    by_num = {m.get('number'): m for m in part.findall('measure')}
    order = sorted(by_num, key=int)
    removed = 0
    for m in by_num.values():
        for a in list(m.findall('attributes')):
            for c in a.findall('clef'):
                a.remove(c); removed += 1
            if len(a) == 0:
                m.remove(a)
    plan = {n: [] for n in order}       # measure -> [(time, order_key, kind)]
    for num, key, x, cx, il, stack_left, kind, pushed in book_clefs(omr):
        m = by_num.get(str(num))
        if m is None or kind not in SIGN_LINE:
            continue
        t = None
        if not pushed:
            for _, start, dx in timeline(m):
                if dx is not None and stack_left + dx * il / 10.0 >= cx:
                    t = start if t is None else min(t, start)
                    break
        if t is not None:
            plan[str(num)].append((t, key, kind))
        else:                           # after the last note: start of the next measure
            nxt = str(num + 1)
            if nxt in plan:
                plan[nxt].append((0, key, kind))
    cur, written = None, 0
    for n in order:
        m = by_num[n]
        # same time: the later clef in reading order wins
        at = {}
        for t, key, kind in sorted(plan[n], key=lambda p: (p[0], p[1])):
            at[t] = kind
        tl = timeline(m)
        for t in sorted(at):
            kind = at[t]
            if kind == cur:
                continue
            cur = kind
            target = next((e for e, s, _ in tl if s == t), None)
            el = clef_element(kind)
            if target is None:
                m.append(el)
            else:
                m.insert(list(m).index(target), el)
            written += 1
    log.append('clefs: removed %d exported, wrote %d from the book' % (removed, written))
