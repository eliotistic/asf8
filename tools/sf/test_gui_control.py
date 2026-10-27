#!/usr/bin/env python3 -I
"""Drive the real String Fingering window from outside, on a private virtual display, and check it responds.

    cd ~/shape/fingering && make build && python3 -I tools/sf/test_gui_control.py

Starts tools/sf/private_window.sh (Xvfb plus the window with --control, on a display of its own, so nothing
appears on the desktop), talks to it with the same protocol as the headless server, and stops it again.
"""
import json
import os
import socket
import struct
import subprocess
import sys
import unittest

APP = os.path.expanduser('~/shape/fingering')
STATE = os.path.expanduser('~/.string_fingering/private')
SCRIPT = os.path.join(APP, 'tools/sf/private_window.sh')
ABC = os.path.join(STATE, 'test_gui_control.abc')


class Window(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        os.makedirs(STATE, exist_ok=True)
        with open(ABC, 'w') as fh:
            fh.write('X:1\nT:test\nM:4/4\nL:1/8\nK:C\nCEGc EGce|[CEG]4 GEC2|\n')
        subprocess.run([SCRIPT, 'start'], check=True, capture_output=True)
        cls.port = int(open(os.path.join(STATE, 'control.port')).read())

    @classmethod
    def tearDownClass(cls):
        subprocess.run([SCRIPT, 'stop'], check=False, capture_output=True)

    def ask(self, cmd, **fields):
        with socket.create_connection(('127.0.0.1', self.port), timeout=120) as s:
            s.sendall((json.dumps(dict(cmd=cmd, **fields)) + '\n').encode())
            return json.loads(s.makefile('r').readline())

    def test_00_state_and_menus_of_the_empty_window(self):
        st = self.ask('gui_state')
        self.assertTrue(st['ok'] and st['visible'])
        self.assertEqual(st['items'], 0)
        names = [m['name'] for m in self.ask('gui_menus')['menus']]
        for want in ('File', 'Instrument', 'Anchors', 'Solve', 'Help'):
            self.assertIn(want, names)

    def test_10_a_command_before_a_piece_is_an_error_not_a_crash(self):
        r = self.ask('gui_next')
        self.assertFalse(r['ok'])
        self.assertIn('no piece is loaded', r['error'])
        self.assertTrue(self.ask('gui_state')['ok'])

    def test_20_load_step_extend_and_look(self):
        loaded = self.ask('gui_load_abc', path=ABC)
        self.assertTrue(loaded['ok'] and loaded['loaded'], loaded)
        self.assertEqual(loaded['items'], 12)
        self.assertEqual(self.ask('gui_state')['masterIndex'], 0)
        self.assertEqual(self.ask('gui_next')['masterIndex'], 1)
        self.assertTrue(self.ask('gui_extend')['ok'])
        self.assertEqual(self.ask('gui_jump', index=5)['masterIndex'], 5)
        self.assertEqual(self.ask('gui_start')['masterIndex'], 0)
        shot = os.path.join(STATE, 'test_gui_control.png')
        self.assertTrue(self.ask('gui_screenshot', path=shot)['ok'])
        with open(shot, 'rb') as fh:
            head = fh.read(24)
        self.assertEqual(head[:8], b'\x89PNG\r\n\x1a\n')
        width, height = struct.unpack('>II', head[16:24])
        self.assertGreater(width, 1000)
        self.assertGreater(height, 500)

    def test_25_a_musicxml_score_is_drawn_by_verovio_and_steps_like_any_other(self):
        caprice = os.path.expanduser('~/libmusic/Paganini/mutopia_caprice01/caprice01_ly2mxml.musicxml')
        loaded = self.ask('gui_load', path=caprice)
        self.assertTrue(loaded['ok'] and loaded['loaded'], loaded)
        self.assertEqual(loaded['items'], 1086)
        self.assertIn('caprice01', self.ask('gui_state')['title'])
        for _ in range(3):
            self.assertTrue(self.ask('gui_next')['ok'])
        for _ in range(4):
            self.assertTrue(self.ask('gui_extend')['ok'])
        self.assertEqual(self.ask('gui_state')['masterIndex'], 3)
        self.assertEqual(self.ask('gui_jump', index=373)['masterIndex'], 373)   # the triple stop of measure 26
        shot = os.path.join(STATE, 'test_gui_musicxml.png')
        self.assertTrue(self.ask('gui_screenshot', path=shot)['ok'])
        self.assertGreater(os.path.getsize(shot), 50000)                       # a drawn score, not a blank window
        marked = self.ask('gui_unique', on=True)
        self.assertTrue(marked['ok'], marked)
        self.assertGreater(len(marked['stretches']), 30)                       # forced stretches (one trail), found note by note: 41 on the caprice
        first = marked['stretches'][0]
        self.assertEqual((first['firstItem'], first['lastItem']), (59, 64))     # the first stretch with one trail only (the pickup arpeggio has 9 trails)
        self.assertIn('one trail', first['label'])
        self.assertTrue(self.ask('gui_screenshot', path=shot)['ok'])
        self.assertEqual(self.ask('gui_unique', on=False)['stretches'], [])
        self.assertFalse(self.ask('gui_load', path='/no/such/score.musicxml')['loaded'])

    def test_30_the_model_commands_work_through_the_window_too(self):
        r = self.ask('chord', pitches=['C', 'G'])
        self.assertTrue(r['ok'] and r['playable'], r)
        self.assertEqual(self.ask('constraints')['maxHeights'], [24, 28, 32, 35])


if __name__ == '__main__':
    sys.exit(0 if unittest.main(exit=False).result.wasSuccessful() else 1)
