#!/usr/bin/env python3 -I
"""Add digit fingerings (from DigitAttach's .tsv) to a COPY of an Audiveris book.

Each digit becomes a <fingering> inter in the same system block as its head, plus a
head-fingering relation, so Audiveris' own exporter decides which note it belongs to.
Digits whose head already has a fingering of the same value are skipped, and a head
gets at most one digit per side (above / below / beside); extras are reported.

Usage: add_fingerings.py in.omr digits.tsv out.omr
Complexity: O(D * size of sheet XML).
"""
import re, sys, zipfile

def main(src, tsv, dst):
    zin = zipfile.ZipFile(src)
    name = 'sheet#1/sheet#1.xml'
    s = zin.read(name).decode('utf-8')
    top = max(int(i) for i in re.findall(r' id="(\d+)"', s))
    blocks = [(m.start(), m.end()) for m in re.finditer(r'<inters>.*?</inters>', s, re.S)]
    rels = [(m.start(), m.end()) for m in re.finditer(r'<relations>.*?</relations>', s, re.S)]
    existing = {}
    for m in re.finditer(r'<relation source="(\d+)" target="(\d+)">\s*<head-fingering', s):
        existing.setdefault(m.group(1), []).append(m.group(2))
    fvals = {m.group(1): m.group(2) for m in re.finditer(r'<fingering value="(\d)"[^>]* id="(\d+)"', s) for m in [type('M', (), {'group': lambda self, k, _m=m: (_m.group(2) if k == 1 else _m.group(1))})()]}
    rows = [l.split('\t') for l in open(tsv).read().split('\n')[1:] if l]
    ins_inter, ins_rel, skipped, used = {}, {}, [], set()
    added = 0
    for digit, x, y, head, pitch_staff, how in rows:
        if not head or head == '0' or how == 'unmatched': continue
        side = how.split('-')[0]
        if (head, side) in used: skipped.append((digit, x, y, head, 'second digit on the same side')); continue
        if any(fvals.get(f) == digit for f in existing.get(head, [])): skipped.append((digit, x, y, head, 'already present')); continue
        hm = re.search(r'<head [^>]*\bid="%s"' % head, s)
        if not hm: skipped.append((digit, x, y, head, 'head not found')); continue
        bi = next(i for i, (a, b) in enumerate(blocks) if a <= hm.start() < b)
        staff = pitch_staff.split('/')[1]
        top += 1
        used.add((head, side))
        ins_inter.setdefault(bi, []).append(
            '          <fingering value="%s" shape="DIGIT_%s" grade="1" manual="true" staff="%s" id="%d">\n'
            '            <bounds x="%d" y="%d" w="9" h="15"/>\n          </fingering>\n' % (digit, digit, staff, top, int(x) - 4, int(y) - 7))
        ins_rel.setdefault(bi, []).append(
            '          <relation source="%s" target="%d">\n            <head-fingering dx="0" dy="0" grade="1"/>\n          </relation>\n' % (head, top))
        added += 1
    # insert from the back so offsets stay valid
    edits = []
    for bi, items in ins_inter.items():
        a, b = blocks[bi]; edits.append((b - len('</inters>'), ''.join(items)))
        ra, rb = next((ra, rb) for ra, rb in rels if ra >= b)
        edits.append((rb - len('</relations>'), ''.join(ins_rel[bi])))
    for pos, text in sorted(edits, reverse=True): s = s[:pos] + text + s[pos:]
    with zipfile.ZipFile(dst, 'w', zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            zout.writestr(item, s.encode('utf-8') if item.filename == name else zin.read(item.filename))
    print('added %d fingering inters; skipped %d' % (added, len(skipped)))
    for k in skipped: print('  skip', k)

if __name__ == '__main__':
    main(*sys.argv[1:4])
