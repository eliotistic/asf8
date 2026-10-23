#!/usr/bin/env python3 -I
"""Join single-page MusicXML files (one per scanned page) into one score.

The first file is the base.  Measures of the following files are appended and renumbered; their
<attributes> are dropped when key, time and clef equal those in force, otherwise kept (so a real
change of key or clef survives).  Complexity: O(notes).  Standard library only.

Usage: join_pages.py OUT.musicxml PAGE1.musicxml PAGE2.musicxml ...
"""
import sys
from math import lcm
import xml.etree.ElementTree as ET


def state(a):
    """(key fifths, time beats/type, clef sign/line) written in an <attributes>, else None per field."""
    return (a.findtext('key/fifths'), (a.findtext('time/beats'), a.findtext('time/beat-type')) if a.find('time') is not None else None,
            (a.findtext('clef/sign'), a.findtext('clef/line')) if a.find('clef') is not None else None)


def scale(root, f):
    """Multiply every duration in a score by the integer f (to a common divisions value)."""
    if f == 1:
        return
    for d in root.iter('duration'):
        d.text = str(int(d.text) * f)
    for d in root.iter('divisions'):
        d.text = str(int(d.text) * f)


def main(out, pages):
    roots = [ET.parse(p).getroot() for p in pages]
    L = lcm(*[int(r.findtext('.//divisions')) for r in roots])
    for r in roots:
        scale(r, L // int(r.findtext('.//divisions')))
    base = roots[0]
    part = base.find('part')
    div = str(L)
    cur = {}
    for a in part.iter('attributes'):
        s = state(a)
        for k, v in zip(('key', 'time', 'clef'), s):
            if v is not None:
                cur[k] = v
    n = len(part.findall('measure'))
    for r in roots[1:]:
        for m in r.find('part').findall('measure'):
            n += 1
            m.set('number', str(n))
            for a in list(m.findall('attributes')):
                s = dict(zip(('key', 'time', 'clef'), state(a)))
                for tag in ('key', 'time', 'clef'):
                    el = a.find(tag)
                    if el is not None and s[tag] == cur.get(tag):
                        a.remove(el)
                    elif el is not None:
                        cur[tag] = s[tag]
                if a.find('divisions') is not None:
                    a.remove(a.find('divisions'))
                if len(a) == 0:
                    m.remove(a)
            part.append(m)
    ET.indent(base, space='  ')
    ET.ElementTree(base).write(out, encoding='UTF-8', xml_declaration=True)
    print('%d measures from %d pages -> %s' % (n, len(pages), out))


if __name__ == '__main__':
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2:])
