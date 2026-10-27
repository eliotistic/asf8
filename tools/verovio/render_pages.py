#!/usr/bin/env python3 -I
"""Render a MusicXML / MEI / kern score with Verovio into page images plus a box for every note.

    render_pages.py SCORE OUTDIR [PNG_WIDTH]

writes OUTDIR/page-N.png and OUTDIR/notation.json:

    {"pages": [{"png": "page-1.png", "width": W, "height": H}, ...],
     "notes": [{"id": "...", "page": 1, "x": px, "y": px, "w": px, "h": px,
                "pitch": 64, "on": 0.125, "grace": false}, ...]}

"on" is Verovio's own onset for the note in quarter notes (its timemap's qstamp) and "pitch" its MIDI
pitch.  The notes are written sorted by (on, pitch), so a consumer can pair them with another sorted list
of the same score and check the pitches agree, without relying on Verovio's random xml:ids.

The box is the notehead's position in the page image: the glyph's origin plus one staff space of height.
Needs libverovio.so (VEROVIO_LIB, default ~/shape/lib/verovio/build) and rsvg-convert.
Complexity: O(notes) calls into Verovio, one per note.
"""
import ctypes
import json
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

LIB = os.environ.get('VEROVIO_LIB', os.path.expanduser('~/shape/lib/verovio/build/libverovio.so'))
DATA = os.environ.get('VEROVIO_DATA', os.path.expanduser('~/shape/lib/verovio/data'))
OPTIONS = {'breaks': 'auto', 'scale': 40, 'pageHeight': 2970, 'pageWidth': 2100, 'adjustPageHeight': True,
           'transposeToSoundingPitch': True, 'spacingSystem': 14}
SVG = '{http://www.w3.org/2000/svg}'


def toolkit():
    lib = ctypes.CDLL(LIB)
    c = ctypes
    lib.vrvToolkit_constructorResourcePath.restype = c.c_void_p
    lib.vrvToolkit_constructorResourcePath.argtypes = [c.c_char_p]
    for name in ('setOptions', 'loadData'):
        getattr(lib, 'vrvToolkit_' + name).argtypes = [c.c_void_p, c.c_char_p]
    lib.vrvToolkit_loadData.restype = c.c_bool
    lib.vrvToolkit_getPageCount.argtypes = [c.c_void_p]
    lib.vrvToolkit_getPageCount.restype = c.c_int
    lib.vrvToolkit_renderToSVG.argtypes = [c.c_void_p, c.c_int, c.c_bool]
    lib.vrvToolkit_renderToSVG.restype = c.c_char_p
    lib.vrvToolkit_renderToTimemap.argtypes = [c.c_void_p, c.c_char_p]
    lib.vrvToolkit_renderToTimemap.restype = c.c_char_p
    lib.vrvToolkit_getMIDIValuesForElement.argtypes = [c.c_void_p, c.c_char_p]
    lib.vrvToolkit_getMIDIValuesForElement.restype = c.c_char_p
    lib.vrvToolkit_getElementAttr.argtypes = [c.c_void_p, c.c_char_p]
    lib.vrvToolkit_getElementAttr.restype = c.c_char_p
    return lib, lib.vrvToolkit_constructorResourcePath(DATA.encode())


def load_score(lib, tk, path):
    options = dict(OPTIONS)
    ext = os.path.splitext(path)[1].lower()
    if ext in ('.xml', '.musicxml'):
        options['inputFrom'] = 'musicxml'
    lib.vrvToolkit_setOptions(tk, json.dumps(options).encode())
    with open(path, 'rb') as fh:
        data = fh.read().decode('utf-8', 'replace').encode('utf-8')
    if not lib.vrvToolkit_loadData(tk, data):
        raise SystemExit('Verovio could not load %s' % path)


def staff_space(root):
    """Distance between two staff lines, in the SVG's own units."""
    for staff in root.iter(SVG + 'g'):
        if staff.get('class') == 'staff':
            ys = []
            for p in staff.findall(SVG + 'path'):
                m = re.match(r'M\s*[\d.]+\s+([\d.]+)', p.get('d', ''))
                if m:
                    ys.append(float(m.group(1)))
            if len(ys) >= 2:
                return abs(ys[1] - ys[0])
    raise SystemExit('no staff lines found in the SVG')


def note_origins(root):
    """(id, x, y) for every note's notehead, in the SVG's units with the page margin applied."""
    margin_x = margin_y = 0.0
    for g in root.iter(SVG + 'g'):
        if g.get('class') == 'page-margin':
            m = re.search(r'translate\(\s*([-\d.]+)[ ,]+([-\d.]+)\)', g.get('transform', ''))
            if m:
                margin_x, margin_y = float(m.group(1)), float(m.group(2))
    out = []
    for g in root.iter(SVG + 'g'):
        if g.get('class') != 'note':
            continue
        for head in g.iter(SVG + 'g'):
            if head.get('class') == 'notehead':
                use = head.find(SVG + 'use')
                m = re.search(r'translate\(\s*([-\d.]+)[ ,]+([-\d.]+)\)', use.get('transform', ''))
                out.append((g.get('id'), float(m.group(1)) + margin_x, float(m.group(2)) + margin_y))
                break
    return out


def main(score, outdir, width=1400):
    lib, tk = toolkit()
    load_score(lib, tk, score)
    # the timemap lists, for every instant, the ids of the notes that begin there (qstamp = quarters)
    # The timemap plays the repeats out: the replayed notes carry ids ending -rendN, and everything after a replay
    # is later than where it is written.  The data JJ reads is in written time, so skip the replays and take the
    # length of each replay as an offset to subtract from what follows (with first and second endings the replay
    # is the repeated part only, and the second ending's notes come after it, so the same rule holds).
    onset, offset, replay_from = {}, 0.0, None
    for entry in json.loads(lib.vrvToolkit_renderToTimemap(tk, b'{"includeRests": false}')):
        ons = entry.get('on', [])
        if not ons:
            continue
        if all('-rend' in i for i in ons):
            if replay_from is None:
                replay_from = entry['qstamp']
            continue
        if replay_from is not None:
            offset += entry['qstamp'] - replay_from
            replay_from = None
        for note_id in ons:
            onset[note_id] = entry['qstamp'] - offset
    os.makedirs(outdir, exist_ok=True)
    pages, notes = [], []
    for page in range(1, lib.vrvToolkit_getPageCount(tk) + 1):
        svg = lib.vrvToolkit_renderToSVG(tk, page, False).decode('utf-8')
        svg_path = os.path.join(outdir, 'page-%d.svg' % page)
        png_path = os.path.join(outdir, 'page-%d.png' % page)
        with open(svg_path, 'w', encoding='utf-8') as fh:
            fh.write(svg)
        subprocess.run(['rsvg-convert', '-w', str(width), '-o', png_path, svg_path], check=True)
        os.remove(svg_path)
        root = ET.fromstring(svg)
        box = [float(v) for v in re.search(r'viewBox="([^"]+)"', svg).group(1).split()]
        scale = width / box[2]
        height = int(round(box[3] * scale))
        pages.append({'png': os.path.basename(png_path), 'width': width, 'height': height})
        space = staff_space(root)
        for note_id, x, y in note_origins(root):
            midi = json.loads(lib.vrvToolkit_getMIDIValuesForElement(tk, note_id.encode()))
            # a grace note has no duration in the data JJ reads (it is not a note of the piece's rhythm)
            grace = 'grace' in json.loads(lib.vrvToolkit_getElementAttr(tk, note_id.encode()))
            notes.append({'id': note_id, 'page': page, 'grace': grace,
                          'x': round(x * scale, 1), 'y': round((y - 0.6 * space) * scale, 1),
                          'w': round(1.4 * space * scale, 1), 'h': round(1.2 * space * scale, 1),
                          'pitch': midi['pitch'], 'on': onset[note_id]})
    notes.sort(key=lambda n: (round(n['on'], 6), n['pitch']))
    with open(os.path.join(outdir, 'notation.json'), 'w') as fh:
        json.dump({'pages': pages, 'notes': notes}, fh)
    print('pages %d notes %d' % (len(pages), len(notes)))


if __name__ == '__main__':
    if len(sys.argv) not in (3, 4):
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2], int(sys.argv[3]) if len(sys.argv) == 4 else 1400)
