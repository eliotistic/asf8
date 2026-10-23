#!/usr/bin/env python3 -I
"""Carry printed fingerings from a baseline MusicXML score onto an edited copy of it.

The baseline is the OCR score with its fingerings on notes.  The edited file is the same
score after notes were fixed by hand (MuseScore re-exports everything).  A note is
identified by a key that does not change when other notes are added or deleted:

    (measure number, onset in whole notes inside the measure, sounding pitch)

  kept      the key exists in both and the edited note has the same fingering  -> nothing to do
  restored  the key exists in both, the edited note lost its fingering        -> put back
  changed   the key exists in both, the fingering differs                     -> reported, never overwritten
  moved     the note was moved or re-pitched and its fingering went with it -> reported only
  orphan    a baseline fingering whose note is gone; if exactly one fingering-less note in the
            same measure has the same pitch (moved) or the same onset (re-pitched), it is
            PROPOSED, never applied
  new       notes of the edited score with no fingering and no baseline counterpart

Only `restored` is written.  Everything else is listed in the report with its measure.

Usage:  match_fingerings.py BASELINE EDITED OUT.musicxml        (.xml/.musicxml/.mxl all read)
Complexity: O(notes) per file.  Standard library only.
"""
import sys
import zipfile
import xml.etree.ElementTree as ET
from collections import defaultdict
from fractions import Fraction

STEP = {'C': 0, 'D': 2, 'E': 4, 'F': 5, 'G': 7, 'A': 9, 'B': 11}
DOCTYPE = ('<!DOCTYPE score-partwise PUBLIC "-//Recordare//DTD MusicXML 3.0 Partwise//EN" '
           '"http://www.musicxml.org/dtds/partwise.dtd">\n')


def load(path):
    if path.endswith('.mxl'):
        with zipfile.ZipFile(path) as z:
            names = [n for n in z.namelist() if n.endswith('.xml') and not n.startswith('META-INF')]
            return ET.fromstring(z.read(names[0]))
    return ET.parse(path).getroot()


def pitch_of(n):
    p = n.find('pitch')
    if p is None:
        return None
    alter = p.findtext('alter')
    return (12 * (int(p.findtext('octave')) + 1) + STEP[p.findtext('step')]
            + (int(float(alter)) if alter else 0))


def pitch_name(m):
    """ABC notation: C=60, c=72, C,=48; _ flat, ^ sharp."""
    name = 'C ^C D _E E F ^F G _A A _B B'.split()[m % 12]
    acc, letter = name[:-1], name[-1]
    o = m // 12 - 1
    if o >= 5:
        return acc + letter.lower() + "'" * (o - 5)
    return acc + letter + ',' * (4 - o)


def notes_of(root):
    """[(key, note_element, measure_element, fingering_text_or_None)], parts concatenated."""
    out = []
    for part in root.findall('part'):
        div = 1
        for m in part.findall('measure'):
            cur = Fraction(0)
            last_start = Fraction(0)
            for e in m:
                if e.tag == 'attributes' and e.findtext('divisions'):
                    div = int(e.findtext('divisions'))
                elif e.tag == 'note':
                    if e.find('grace') is not None:
                        continue
                    dur = Fraction(int(e.findtext('duration', '0')), div * 4)
                    if e.find('chord') is None:
                        last_start = cur
                        cur += dur
                    p = pitch_of(e)
                    if p is None:
                        continue
                    f = e.find('notations/technical/fingering')
                    out.append(((m.get('number'), last_start, p), e, m,
                                f.text.strip() if f is not None and f.text else None))
                elif e.tag == 'backup':
                    cur -= Fraction(int(e.findtext('duration')), div * 4)
                elif e.tag == 'forward':
                    cur += Fraction(int(e.findtext('duration')), div * 4)
    return out


def put_fingering(note, text, placement):
    nt = note.find('notations')
    if nt is None:
        nt = ET.Element('notations')
        idx = len(note)
        for i, c in enumerate(note):
            if c.tag in ('lyric', 'play'):
                idx = i
                break
        note.insert(idx, nt)
    tech = nt.find('technical')
    if tech is None:
        tech = ET.SubElement(nt, 'technical')
    f = ET.SubElement(tech, 'fingering')
    if placement:
        f.set('placement', placement)
    f.text = text


def fmt(key):
    meas, t, p = key
    return 'm%s @%s %s' % (meas, t, pitch_name(p))


def main(base_path, edit_path, out_path):
    base = notes_of(load(base_path))
    root = load(edit_path)
    edit = notes_of(root)
    ebykey = defaultdict(list)
    for k, n, m, f in edit:
        ebykey[k].append([n, f, False])           # note, fingering, matched-by-baseline
    log = []
    kept = restored = 0
    orphans = []
    for k, n, m, f in base:
        if f is None:
            continue
        cands = ebykey.get(k)
        if not cands:
            orphans.append((k, f, n.find('notations/technical/fingering')))
            continue
        slot = next((c for c in cands if not c[2]), None)
        if slot is None:
            orphans.append((k, f, n.find('notations/technical/fingering')))
            continue
        slot[2] = True
        if slot[1] == f:
            kept += 1
        elif slot[1] is None:
            fe = n.find('notations/technical/fingering')
            put_fingering(slot[0], f, fe.get('placement') if fe is not None else None)
            slot[1] = f
            restored += 1
            log.append('restored  %s  finger %s' % (fmt(k), f))
        else:
            log.append('CHANGED   %s  baseline %s, edited %s' % (fmt(k), f, slot[1]))
    # orphans: propose, never apply
    free = defaultdict(list)                      # measure -> fingering-less edited notes
    for k, n, m, f in edit:
        if f is None and not any(c[0] is n and c[1] for c in ebykey[k]):
            free[k[0]].append(k)
    carried = defaultdict(list)                   # fingered edited notes no baseline note claims
    for k, n, m, f in edit:
        if f is not None and not any(c[0] is n and c[2] for c in ebykey[k]):
            carried[k[0]].append((k, f))
    proposals = []
    for k, f, _ in orphans:
        twin = [c for c, cf in carried[k[0]] if cf == f and (c[1] == k[1] or c[2] == k[2])]
        if len(twin) == 1:
            log.append('moved     %s  finger %s travelled with its note to %s' % (fmt(k), f, fmt(twin[0])))
            continue
        same_p = [c for c in free[k[0]] if c[2] == k[2]]
        same_t = [c for c in free[k[0]] if c[1] == k[1]]
        if len(same_p) == 1:
            proposals.append('propose  %s  finger %s -> %s (same pitch, moved)' % (fmt(k), f, fmt(same_p[0])))
        elif len(same_t) == 1:
            proposals.append('propose  %s  finger %s -> %s (same onset, re-pitched)' % (fmt(k), f, fmt(same_t[0])))
        else:
            log.append('ORPHAN    %s  finger %s: note gone, no unique candidate' % (fmt(k), f))
    log.extend(proposals)
    nbase = {k for k, _, _, _ in base}
    newn = [k for k, n, m, f in edit if f is None and k not in nbase]
    if newn:
        by = defaultdict(list)
        for k in newn:
            by[k[0]].append('@%s %s' % (k[1], pitch_name(k[2])))
        for meas in sorted(by, key=lambda x: int(x) if x.isdigit() else 0):
            log.append('NEW       m%s  no fingering yet: %s' % (meas, ', '.join(by[meas])))
    nfing_edit = sum(1 for k, n, m, f in edit if f is not None)
    nfing_base = sum(1 for k, n, m, f in base if f is not None)
    summary = ('baseline %d notes %d fingerings; edited %d notes %d fingerings; '
               'kept %d, restored %d, orphans %d (proposed %d), new notes without fingering %d'
               % (len(base), nfing_base, len(edit), nfing_edit, kept, restored,
                  len(orphans), len(proposals), len(newn)))
    ET.indent(root, space='  ') if hasattr(ET, 'indent') else None
    with open(out_path, 'w', encoding='utf-8') as fh:
        fh.write('<?xml version="1.0" encoding="UTF-8"?>\n' + DOCTYPE)
        fh.write(ET.tostring(root, encoding='unicode'))
    rep = out_path + '.report.txt'
    with open(rep, 'w') as fh:
        fh.write(summary + '\n' + '\n'.join(log) + '\n')
    print(summary)
    print('\n'.join(log[:60]))
    print('wrote %s and %s' % (out_path, rep))


if __name__ == '__main__':
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    main(*sys.argv[1:])
