#!/usr/bin/env python3 -I
"""Instruments as a parameter: the registry, the engine made from a spec, and a scale on each instrument.

    cd ~/shape/fingering && make build && python3 -I tools/sf/test_instruments.py

Runs computingmusic.InstrumentCheck (no JJ), then writes a two-octave scale as MusicXML under
~/.string_fingering/instruments/ and loads it through the server for the cello, the viola and the violin: the
placements must be on the strings of the instrument asked for.  A violin piece loaded with instrument 'violin' must give
what loading it with no instrument gives.
"""
import os
import subprocess
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from test_server import ABC_JAR, APP, Client, HOME  # noqa: E402

SCRATCH = os.path.join(HOME, '.string_fingering', 'instruments')
STEPS = [('C', 0), ('D', 0), ('E', 0), ('F', 0), ('G', 0), ('A', 0), ('B', 0)]


def scale_musicxml(path, first_octave, octaves=2):
    notes = []
    for o in range(octaves):
        for step, alter in STEPS:
            notes.append((step, first_octave + o))
    notes.append(('C', first_octave + octaves))
    body = ''.join(
        '<note><pitch><step>%s</step><octave>%d</octave></pitch><duration>4</duration><type>quarter</type></note>' % n
        for n in notes)
    xml = ('<?xml version="1.0" encoding="UTF-8"?><score-partwise version="3.1"><part-list><score-part id="P1">'
           '<part-name>Scale</part-name></score-part></part-list><part id="P1"><measure number="1"><attributes>'
           '<divisions>4</divisions><time><beats>%d</beats><beat-type>4</beat-type></time>'
           '<clef><sign>G</sign><line>2</line></clef></attributes>%s</measure></part></score-partwise>' % (len(notes), body))
    os.makedirs(SCRATCH, exist_ok=True)
    with open(path, 'w') as f:
        f.write(xml)


class Registry(unittest.TestCase):
    def test_instrument_check(self):
        r = subprocess.run(['java', '-Xmx3g', '-Djava.awt.headless=true', '-cp', 'build/classes:' + ABC_JAR,
                            'computingmusic.InstrumentCheck'], cwd=APP, capture_output=True, text=True, timeout=300)
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        self.assertIn('all passed', r.stdout)


class ServerInstruments(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.c = Client()
        cls.cello = os.path.join(SCRATCH, 'scale_cello.musicxml')
        cls.viola = os.path.join(SCRATCH, 'scale_viola.musicxml')
        scale_musicxml(cls.cello, 2)      # C2 up two octaves
        scale_musicxml(cls.viola, 3)      # C3 up two octaves

    @classmethod
    def tearDownClass(cls):
        cls.c.close()

    def placements(self, path, instrument):
        fields = dict(path=path)
        if instrument is not None:
            fields['instrument'] = instrument
        r = self.c.ask('load', **fields)
        self.assertTrue(r['ok'], r)
        self.assertTrue(self.c.ask('run')['ok'])
        out = []
        for i in range(r['items']):
            item = self.c.ask('item', index=i)
            self.assertTrue(item['ok'], item)
            out.append([(n.get('string'), n.get('height')) for n in item['notes']])
        return r, out

    def test_cello_scale(self):
        r, out = self.placements(self.cello, 'cello')
        self.assertEqual(r['instrument'], 'Cello')
        self.assertEqual(self.c.ask('constraints')['openStrings'], [36, 43, 50, 57])
        self.assertEqual(out[0], [('C', 0)])           # C2 is the open C string
        self.assertEqual(out[4], [('G', 0)])           # G2 the open G string
        self.assertTrue(all(n[0][0] is not None for n in out))

    def test_viola_scale(self):
        r, out = self.placements(self.viola, 'viola')
        self.assertEqual(out[0], [('C', 0)])           # C3 is the viola's open C
        self.assertEqual(out[4], [('G', 0)])
        self.assertTrue(all(n[0][0] is not None for n in out))

    def test_violin_by_name_equals_default(self):
        _, by_name = self.placements(self.viola, 'violin')
        c2 = Client()
        try:
            r = c2.ask('load', path=self.viola)
            self.assertEqual(r['instrument'], 'Violin')
            c2.ask('run')
            default = [[(n.get('string'), n.get('height')) for n in c2.ask('item', index=i)['notes']] for i in range(r['items'])]
        finally:
            c2.close()
        self.assertEqual(by_name, default)

    def test_unknown_instrument_is_an_error(self):
        r = self.c.ask('load', path=self.viola, instrument='banjo')
        self.assertFalse(r['ok'])
        self.assertIn('unknown instrument', r['error'])


if __name__ == '__main__':
    unittest.main()
