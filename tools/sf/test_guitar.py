#!/usr/bin/env python3 -I
"""The guitar: chords on any strings, the barre, held notes.

    cd ~/shape/fingering && make build && python3 -I tools/sf/test_guitar.py

Runs computingmusic.eval.GuitarCheck (synthetic items, no JJ, no window): open C, E, Am, G chords, the F barre 133211, a chord
no four fingers hold, a six-note chord, a held bass under moving melody notes (the held finger and string stay), and a held
note that forbids a hand shift.  Every CHECK line must be ok.  Then, if the converted Sor files exist, solves sor_op1_2 with the
guitar and sor_op1_3 with drop D and requires every item to have a fingering.  Violin regression is separate (doc/INSTRUMENTS.md).
"""
import os
import subprocess
import sys
import unittest

HOME = os.path.expanduser('~')
APP = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ABC_JAR = os.path.join(HOME, 'abc', 'abcMaine.jar')
SOR = os.path.join(HOME, 'libmusic', 'guitar_survey', 'sor')


def java(*args):
    env = dict(os.environ)
    env['PATH'] = os.path.join(APP, 'tools', 'jj', 'nojump') + os.pathsep + env['PATH']
    return subprocess.run(['java', '-Xmx3g', '-Djava.awt.headless=true', '-cp', os.path.join(APP, 'build', 'classes') + os.pathsep + ABC_JAR]
                          + list(args), cwd=APP, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, universal_newlines=True, timeout=600)


class GuitarChecks(unittest.TestCase):
    def test_synthetic_cases(self):
        r = java('computingmusic.eval.GuitarCheck')
        fails = [l for l in r.stdout.splitlines() if l.startswith('CHECK FAIL')]
        self.assertEqual(r.returncode, 0, r.stdout[-3000:])
        self.assertEqual(fails, [])
        self.assertIn('ALL CHECKS PASSED', r.stdout)
        self.assertGreaterEqual(r.stdout.count('CHECK ok'), 20)

    def solve(self, name, instrument):
        f = os.path.join(SOR, name, name + '_ly2mxml.musicxml')
        if not os.path.isfile(f):
            self.skipTest('no ' + f)
        r = java('computingmusic.solve.Solve', f, '1.0', instrument)
        self.assertEqual(r.returncode, 0, r.stdout[-2000:])
        return r.stdout

    def test_sor_op1_2_guitar(self):
        out = self.solve('sor_op1_2', 'guitar')
        self.assertIn('items with no fingering: 0', out)

    def test_sor_op1_3_drop_d(self):
        out = self.solve('sor_op1_3', 'guitar-dropd')
        self.assertIn('items with no fingering: 0', out)


if __name__ == '__main__':
    unittest.main()
