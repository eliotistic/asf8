#!/usr/bin/env python3 -I
"""The viola, cello and double bass: their own hand limits, thumb position reported, real music, violin unchanged.

    cd ~/shape/fingering && make build && python3 -I tools/sf/test_cello.py

1. computingmusic.eval.StringsCheck (synthetic melodies, no JJ, no window): a cello scale in first position, the open-string arpeggio of the
   Suite 1 prelude, a scale up the A string that needs the thumb position (reported, not modelled), notes beyond the board, the viola scale,
   the bass's fingers 1-2-4 on consecutive semitones, a bariolage that needs the extended hand.  Every CHECK line must be ok.
2. Real music, if converted (~/libmusic/Bach_cello, Brey): Suite 1 prelude on the cello and its viola transposition have a fingering for
   every item and no thumb position; Brey's cadenza reports the thumb position and the reasons items have no fingering.
3. Violin regression: Solve on Caprice 1 and the Reger sonata gives exactly the summary recorded before the cello work
   (~/.string_fingering/cello/before_Solve_*.txt), if it exists.
"""
import os
import subprocess
import unittest

HOME = os.path.expanduser('~')
APP = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ABC_JAR = os.path.join(HOME, 'abc', 'abcMaine.jar')
LIB = os.path.join(HOME, 'libmusic')
BEFORE = os.path.join(HOME, '.string_fingering', 'cello')


def java(*args, props=()):
    env = dict(os.environ)
    env['PATH'] = os.path.join(APP, 'tools', 'jj', 'nojump') + os.pathsep + env['PATH']
    return subprocess.run(['java', '-Xmx3g', '-Djava.awt.headless=true'] + list(props) + ['-cp', os.path.join(APP, 'build', 'classes') + os.pathsep + ABC_JAR]
                          + list(args), cwd=APP, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, universal_newlines=True, timeout=900)


class SyntheticCases(unittest.TestCase):
    def test_strings_check(self):
        r = java('computingmusic.eval.StringsCheck')
        fails = [l for l in r.stdout.splitlines() if l.startswith('CHECK FAIL')]
        self.assertEqual(fails, [], r.stdout[-3000:])
        self.assertEqual(r.returncode, 0, r.stdout[-3000:])
        self.assertIn('ALL CHECKS PASSED', r.stdout)
        self.assertGreaterEqual(r.stdout.count('CHECK ok'), 50)


class RealMusic(unittest.TestCase):
    def solve(self, path, instrument):
        f = os.path.join(LIB, path)
        if not os.path.isfile(f):
            self.skipTest('no ' + f)
        r = java('computingmusic.solve.Solve', f, '1.0', instrument)
        self.assertEqual(r.returncode, 0, r.stdout[-2000:])
        return r.stdout

    def test_suite1_prelude_cello(self):
        out = self.solve('Bach_cello/BWV1007/bwv1007_mov1_prelude_ly2mxml.musicxml', 'cello')
        self.assertIn('items with no fingering: 0', out)
        self.assertIn('thumb position, not modelled): 0', out)

    def test_suite1_prelude_viola(self):
        out = self.solve('Bach_cello/BWV1007/viola/bwv1007-viola_mov1_prelude_ly2mxml.musicxml', 'viola')
        self.assertIn('items with no fingering: 0', out)

    def test_brey_cadenza_needs_the_thumb(self):
        out = self.solve('Brey/cadenza/cadenza_ly2mxml.musicxml', 'cello')
        self.assertRegex(out, r'thumb position, not modelled\): [1-9]')
        self.assertRegex(out, r'NO FINGERING for item \d+ \(measure \d+\): .*\[(range|hand reach)')


class ViolinRegression(unittest.TestCase):
    def same(self, path, name):
        f = os.path.join(LIB, path)
        before = os.path.join(BEFORE, 'before_Solve_%s.txt' % name)
        if not (os.path.isfile(f) and os.path.isfile(before)):
            self.skipTest('no ' + f + ' or no recorded baseline')
        r = java('computingmusic.solve.Solve', f, '1.0')
        with open(before) as fh:
            self.assertEqual(r.stdout, fh.read())

    def test_caprice01(self):
        self.same('Paganini/mutopia_caprice01/caprice01_ly2mxml.musicxml', 'caprice01')

    def test_reger_sonata(self):
        self.same('Reger/op42_sonate/sonate_mov1_ly2mxml.musicxml', 'sonate_mov1')


if __name__ == '__main__':
    unittest.main()
