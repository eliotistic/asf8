#!/usr/bin/env python3 -I
"""Rebuild the working MusicXML from the master Audiveris book.

The .omr is the master (edited in the Audiveris GUI).  Everything else is
derived by this script, so nothing is done by hand twice:

  0. optional --digits FILE.tsv (from DigitAttach): add those digits to a COPY of the book
     as fingering inters, so Audiveris' own exporter places them on the right notes
  1. export a COPY of the .omr (the open book is never touched)
  2. patch measures listed with --copy-measure DST=SRC (rhythm solver failures
     that the GUI cannot clear): DST's notes are replaced by deep copies of SRC's
  3. graft fingerings from a switch-on Audiveris run (--fing-mxl) onto notes
     that have none, matched by (measure, pitch, n-th occurrence of that pitch)
  4. rebuild the clefs from the book's clef inters (clefs.py): Audiveris' export loses
     mid-measure clef changes (skip with --no-clefs)
  5. write <out>.mxl and <out>.report.txt (what was patched, grafted, unmatched)

Usage:
  rebuild_score.py BOOK.omr OUTDIR [--fing-mxl FING.mxl] [--copy-measure 4=24]...

Complexity: O(notes).  Standard library only.
"""
import argparse, copy, os, re, shutil, subprocess, sys, tempfile, zipfile
import xml.etree.ElementTree as ET

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))   # -I drops the script dir
import clefs, parts

AUDIVERIS = os.path.expanduser('~/.local/opt/audiveris-5.11.0/opt/audiveris/bin/Audiveris')

def export(omr, workdir, digits=None):
    """Export a copy of the book (optionally with digits added); return the MusicXML text."""
    cp = os.path.join(workdir, 'book.omr')
    if digits:
        here = os.path.dirname(os.path.abspath(__file__))
        subprocess.run([sys.executable, '-I', os.path.join(here, 'add_fingerings.py'), omr, digits, cp], check=True)
    else:
        shutil.copy(omr, cp)
    env = dict(os.environ, DISPLAY=os.environ.get('DISPLAY', ':0'))
    subprocess.run([AUDIVERIS, '-batch', '-export', '-output', workdir, cp],
                   check=True, env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    return read_mxl(os.path.join(workdir, 'book.mxl'))

def read_mxl(path):
    with zipfile.ZipFile(path) as z:
        name = [n for n in z.namelist() if n.endswith('.xml') and not n.startswith('META')][0]
        return z.read(name).decode('utf-8')

def write_mxl(path, text, inner='score.xml'):
    cont = ('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n<container><rootfiles>'
            '<rootfile full-path="%s" media-type="application/vnd.recordare.musicxml+xml"/>'
            '</rootfiles></container>' % inner)
    with zipfile.ZipFile(path, 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr('META-INF/container.xml', cont)
        z.writestr(inner, text)

def header_of(text):
    return text[:text.index('<score-partwise')]

def pitch_key(n):
    p = n.find('pitch')
    if p is None: return None
    return (p.findtext('step'), p.findtext('alter') or '0', p.findtext('octave'))

def measures(root):
    return {m.get('number'): m for m in root.iter('measure')}

def notes_by_key(m):
    """(pitch, n-th occurrence) -> note, for pitched notes in one measure."""
    seen, out = {}, {}
    for n in m.findall('note'):
        k = pitch_key(n)
        if k is None: continue
        seen[k] = seen.get(k, 0) + 1
        out[(k, seen[k])] = n
    return out

def fingering_of(n):
    f = n.find('notations/technical/fingering')
    return f

def copy_measure(root, dst, src, log):
    ms = measures(root)
    d, s = ms[dst], ms[src]
    first = None
    for ch in list(d):
        if ch.tag in ('note', 'backup', 'forward'):
            if first is None: first = list(d).index(ch)
            d.remove(ch)
    idx = first if first is not None else len(d)
    for ch in s:
        if ch.tag in ('note', 'backup', 'forward'):
            d.insert(idx, copy.deepcopy(ch)); idx += 1
    log.append('patched measure %s := notes of measure %s' % (dst, src))

def graft(root, fing_root, skip, log):
    src, dst = measures(fing_root), measures(root)
    done, miss = 0, []
    for num, sm in src.items():
        if num in skip or num not in dst: continue
        tgt = notes_by_key(dst[num])
        for key, n in notes_by_key(sm).items():
            f = fingering_of(n)
            if f is None: continue
            t = tgt.get(key)
            if t is None or fingering_of(t) is not None:
                if t is None: miss.append((num, key, f.text))
                continue
            nt = t.find('notations')
            if nt is None:
                nt = ET.Element('notations')
                lyr = t.find('lyric')
                if lyr is not None: t.insert(list(t).index(lyr), nt)
                else: t.append(nt)
            tech = nt.find('technical')
            if tech is None: tech = ET.SubElement(nt, 'technical')
            tech.append(copy.deepcopy(f)); done += 1
    log.append('grafted %d fingerings; %d had no matching note:' % (done, len(miss)))
    for num, key, txt in miss:
        step, alt, octv = key[0]
        log.append('  m%s %s%s%s (#%d) finger %s' % (num, step, '' if alt == '0' else '(%s)' % alt, octv, key[1], txt))

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('omr'); ap.add_argument('outdir')
    ap.add_argument('--fing-mxl'); ap.add_argument('--no-clefs', action='store_true'); ap.add_argument('--digits'); ap.add_argument('--copy-measure', action='append', default=[])
    a = ap.parse_args()
    os.makedirs(a.outdir, exist_ok=True)
    log = []
    with tempfile.TemporaryDirectory() as wd:
        text = export(a.omr, wd, a.digits)
    hdr = header_of(text)
    root = ET.fromstring(text[len(hdr):])
    parts.merge_parts(root, log)
    log.append('exported %s: %d notes, %d fingerings' % (a.omr, len(list(root.iter('note'))), len(list(root.iter('fingering')))))
    skip = {spec.split('=')[0] for spec in a.copy_measure}
    if a.fing_mxl:
        ft = read_mxl(a.fing_mxl)
        froot = ET.fromstring(ft[len(header_of(ft)):]); parts.merge_parts(froot, log)
        graft(root, froot, skip, log)
    for spec in a.copy_measure:   # after the graft, so DST inherits SRC's fingerings
        dst, src = spec.split('='); copy_measure(root, dst, src, log)
    if not a.no_clefs:
        clefs.restore_clefs(root, a.omr, log)
    for f in root.iter('fingering'):   # OCR offsets float digits far from their notes
        for k in ('default-x', 'default-y', 'relative-x', 'relative-y'): f.attrib.pop(k, None)
    # OCR'd digits come out as page <credit> text; they float above system 1.
    # Remove short ones from the score, keep them (with page position) for digit attachment.
    rows = []
    for c in list(root.findall('credit')):
        w = c.find('credit-words')
        if w is not None and len((w.text or '').strip()) <= 2:
            rows.append('%s\t%s\t%s\t%s' % (w.text.strip(), w.get('default-x'), w.get('default-y'), w.get('font-size')))
            root.remove(c)
    log.append('moved %d short credit texts (OCR digits) to <out>.credits.tsv' % len(rows))
    open(os.path.join(a.outdir, os.path.splitext(os.path.basename(a.omr))[0] + '.credits.tsv'), 'w').write(
        'text\tx\ty\tfont\n' + '\n'.join(rows) + '\n')
    log.append('result: %d fingerings' % len(list(root.iter('fingering'))))
    base = os.path.splitext(os.path.basename(a.omr))[0]
    out = os.path.join(a.outdir, base + '.work')
    write_mxl(out + '.mxl', hdr + ET.tostring(root, encoding='unicode'))
    open(out + '.report.txt', 'w').write('\n'.join(log) + '\n')
    print('\n'.join(log)); print('wrote', out + '.mxl')

if __name__ == '__main__':
    main()
