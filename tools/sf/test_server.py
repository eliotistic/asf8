#!/usr/bin/env python3 -I
"""Drive `computingmusic.SFServer` from outside, as any client would, and check what comes back.

    cd ~/shape/fingering && make build && python3 -I tools/sf/test_server.py

Needs JJ (bin/jj_cmd) and the Caprice MusicXML from tools/ly2mxml.  The checks are the figures established
for Paganini Caprice No. 1; they change only when the constraints do, which is what some of the checks do on
purpose (and put back).
"""
import json
import os
import subprocess
import sys
import unittest

HOME = os.path.expanduser('~')
APP = os.path.join(HOME, 'shape/fingering')
ABC_JAR = os.path.join(HOME, 'abc/abcMaine.jar')
CAPRICE = os.path.join(HOME, 'libmusic/Paganini/mutopia_caprice01/caprice01_ly2mxml.musicxml')


class Client:
    def __init__(self):
        self.proc = subprocess.Popen(
            ['java', '-Xmx3g', '-cp', 'build/classes:' + ABC_JAR, 'computingmusic.SFServer'],
            cwd=APP, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True)
        self.next_id = 0

    def ask(self, cmd, **fields):
        self.next_id += 1
        self.proc.stdin.write(json.dumps(dict(id=self.next_id, cmd=cmd, **fields)) + '\n')
        self.proc.stdin.flush()
        reply = json.loads(self.proc.stdout.readline())
        assert reply['id'] == self.next_id, reply
        return reply

    def close(self):
        try:
            self.ask('quit')
        finally:
            self.proc.wait(timeout=30)


class Server(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.c = Client()

    @classmethod
    def tearDownClass(cls):
        cls.c.close()

    def test_00_ping_and_errors(self):
        self.assertTrue(self.c.ask('ping')['ok'])
        bad = self.c.ask('no_such_command')
        self.assertFalse(bad['ok'])
        self.assertIn('unknown command', bad['error'])
        self.assertFalse(self.c.ask('run')['ok'])            # nothing loaded yet
        self.assertTrue(self.c.ask('ping')['ok'])            # and the server is still there

    def test_10_a_chord_depends_on_the_height_limits(self):
        chord = ['^f', "d'", "a'"]                            # F#5 D6 A6: two of them can only be on the E string
        before = self.c.ask('constraints')
        self.assertEqual(before['maxHeights'], [24, 28, 32, 35])
        self.assertTrue(self.c.ask('chord', pitches=chord)['playable'])
        self.c.ask('set_constraints', maxHeights=[15, 15, 15, 25], stringRange=25)
        try:
            r = self.c.ask('chord', pitches=chord)
            self.assertFalse(r['playable'])
            places = {n['abc']: [(p['string'], p['height']) for p in n['places']] for n in r['eachNoteCouldBe']}
            self.assertEqual(places["d'"], [('E', 10)])       # only the E string reaches D6 under the old limits
            self.assertEqual(places["a'"], [('E', 17)])
        finally:
            self.c.ask('set_constraints', maxHeights=before['maxHeights'], stringRange=before['stringRange'])
        self.assertTrue(self.c.ask('chord', pitches=chord)['playable'])

    def test_20_load_and_run_the_caprice(self):
        loaded = self.c.ask('load', path=CAPRICE)
        self.assertTrue(loaded['ok'], loaded)
        self.assertEqual((loaded['notes'], loaded['items'], loaded['chords'], loaded['printedFingerings']),
                         (1241, 1085, 134, 252))      # the last bar was corrected at source: one chord, not a note and a chord
        run = self.c.ask('run')
        self.assertTrue(run['ok'], run)
        self.assertEqual(run['items'], 1085)
        self.assertEqual(sum(run['ends'].values()), run['segments'])
        self.assertNotIn('IMPOSSIBLE', run['ends'])           # the whole caprice is playable within the limits
        checked = sum(v for k, v in run['printed'].items() if k != 'TWO_PRINTED')
        self.assertEqual(checked, 249)                        # 252 fingered notes, 3 with two numbers

    def test_30_segments_items_and_checks(self):
        segs = self.c.ask('segments', **{'from': 0, 'to': 5})
        self.assertEqual(len(segs['list']), 5)
        first = segs['list'][0]
        self.assertEqual(first['firstItem'], 0)
        item = self.c.ask('item', index=373)                  # the triple stop g e' c'' of measure 26
        self.assertTrue(item['playable'])
        self.assertEqual([n['abc'] for n in item['notes']], ['g', "e'", "c''"])
        self.assertEqual({n['string'] for n in item['notes']}, {'D', 'A', 'E'})
        broken = self.c.ask('check', verdict='ORDER_BROKEN', limit=5)
        self.assertGreater(broken['counts']['ORDER_BROKEN'], 0)
        self.assertLessEqual(len(broken['list']), 5)
        self.assertFalse(self.c.ask('item', index=99999)['ok'])


if __name__ == '__main__':
    sys.exit(0 if unittest.main(exit=False).result.wasSuccessful() else 1)
