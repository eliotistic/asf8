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
import time
import subprocess
import sys
import unittest

APP = os.path.expanduser('~/shape/fingering')
STATE = os.path.expanduser('~/.string_fingering/private')
SCRIPT = os.path.join(APP, 'tools/sf/private_window.sh')
ABC = os.path.join(STATE, 'test_gui_control.abc')
RECENT = os.path.join(STATE, 'recent.txt')
CAPRICE1 = os.path.expanduser('~/libmusic/Paganini/mutopia_caprice01/caprice01_ly2mxml.musicxml')
CAPRICE2 = os.path.expanduser('~/libmusic/Paganini/mutopia_caprice02/caprice02_ly2mxml.musicxml')


class Window(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        os.makedirs(STATE, exist_ok=True)
        with open(ABC, 'w') as fh:
            fh.write('X:1\nT:test\nM:4/4\nL:1/8\nK:C\nCEGc EGce|[CEG]4 GEC2|\n')
        if os.path.exists(RECENT):
            os.remove(RECENT)
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

    def wait_loaded(self, seconds=120):
        end = time.time() + seconds
        while time.time() < end:
            st = self.ask('gui_state')
            if not st['loading']:
                return st
            time.sleep(0.3)
        self.fail('still loading')

    def test_26_dropping_caprice02_works_off_the_swing_thread(self):
        # the cause of "drag and drop does not work": JJ has 1009 notes, Verovio draws 1011 (two grace notes)
        # and the repeat is played out in its timemap; the load was refused with an error
        r = self.ask('gui_drop', path=CAPRICE2)
        self.assertTrue(r['ok'] and r['accepted'], r)
        st = self.ask('gui_state')                      # the window answers while the score is read
        self.assertTrue(st['ok'])
        st = self.wait_loaded()
        self.assertIn('caprice02', st['title'])
        self.assertGreater(st['items'], 900)
        self.assertEqual(self.ask('gui_next')['masterIndex'], 1)

    def test_27_a_text_uri_list_drop_is_read_too(self):
        r = self.ask('gui_drop', path=CAPRICE1, **{'as': 'uri-list'})
        self.assertTrue(r['ok'] and r['accepted'], r)
        st = self.wait_loaded()
        self.assertEqual(st['items'], 1086)
        self.assertIn('caprice01', st['title'])

    def test_28_a_bad_drop_is_refused_with_a_reason(self):
        r = self.ask('gui_drop', path='/no/such/score.musicxml')
        self.assertTrue(r['ok'])
        self.assertFalse(r['accepted'])
        self.assertIn('no file', r['error'])

    def test_29_recent_files_newest_first_menu_and_clear(self):
        recent = self.ask('gui_recent')['recent']
        self.assertEqual(recent[:3], [CAPRICE1, CAPRICE2, ABC])      # ABC, then the two drops, newest first
        with open(RECENT) as fh:
            self.assertEqual(fh.read().split('\n')[:2], [CAPRICE1, CAPRICE2])
        # a file that has gone is shown disabled (the file is reread when the list changes)
        with open(RECENT, 'a') as fh:
            fh.write('/no/such/gone.musicxml\n')
        self.assertTrue(self.ask('gui_load_abc', path=ABC)['loaded'])
        menus = self.ask('gui_menus')['menus']
        file_menu = [m for m in menus if m['name'] == 'File'][0]
        sub = [m for m in file_menu['items'] if m['name'] == 'Open recent'][0]['items']
        by_name = dict((m['name'], m['enabled']) for m in sub if m['name'])
        self.assertTrue(by_name['caprice01_ly2mxml.musicxml'])
        self.assertFalse(by_name['gone.musicxml'])
        self.assertIn('Clear recent', by_name)
        # choosing an entry opens it
        self.ask('gui_menu', path='File/Open recent/caprice02_ly2mxml.musicxml')
        time.sleep(0.5)
        st = self.wait_loaded()
        self.assertIn('caprice02', st['title'])
        self.assertEqual(self.ask('gui_recent')['recent'][0], CAPRICE2)
        self.ask('gui_menu', path='File/Open recent/Clear recent')
        time.sleep(0.5)
        self.assertEqual(self.ask('gui_recent')['recent'], [])
        self.assertTrue(self.ask('gui_load_abc', path=ABC)['loaded'])
        self.assertEqual(self.ask('gui_recent')['recent'], [ABC])

    def test_30_the_model_commands_work_through_the_window_too(self):
        r = self.ask('chord', pitches=['C', 'G'])
        self.assertTrue(r['ok'] and r['playable'], r)
        self.assertEqual(self.ask('constraints')['maxHeights'], [24, 28, 32, 35])

    # ---- zoom of the Verovio score view (Shift + wheel) ----

    def test_40_zoom_sizes_and_clamps(self):
        self.assertTrue(self.ask('gui_load', path=CAPRICE1)['loaded'])
        st = self.ask('gui_zoom', reset=True)
        self.assertTrue(st['ok'], st)
        self.assertFalse(st['userZoomed'])
        self.assertAlmostEqual(st['zoomRelative'], 1.0, places=3)
        w1, h1 = st['prefWidth'], st['prefHeight']
        z2 = self.ask('gui_zoom', factor=2.0, x=100, y=100)
        self.assertTrue(z2['userZoomed'])
        self.assertAlmostEqual(z2['zoomRelative'], 2.0, places=3)
        self.assertAlmostEqual((z2['prefWidth'] - 8) / (w1 - 8), 2.0, delta=0.01)
        self.assertGreater(z2['prefHeight'], 1.8 * h1)
        self.assertAlmostEqual(self.ask('gui_zoom', factor=100.0, x=0, y=0)['zoomRelative'], 4.0, places=3)
        self.assertAlmostEqual(self.ask('gui_zoom', factor=0.001, x=0, y=0)['zoomRelative'], 0.25, places=3)
        self.assertAlmostEqual(self.ask('gui_state')['zoomRelative'], 0.25, places=3)
        shot = os.path.join(STATE, 'test_gui_zoom.png')
        self.assertTrue(self.ask('gui_screenshot', path=shot)['ok'])
        self.assertGreater(os.path.getsize(shot), 20000)

    def test_41_zoom_keeps_the_point_under_the_cursor_and_clicks_still_land(self):
        self.ask('gui_zoom', reset=True)
        item = 373
        p = self.ask('gui_note_point', item=item)
        self.assertEqual(p['hit'], item)
        for factor in (1.5, 3.0, 0.5):
            self.ask('gui_zoom', factor=1.0, x=p['x'], y=p['y'])
            p = self.ask('gui_note_point', item=item)
            # put the note in view first, so that the anchor is a real point of the window
            ax, ay = p['x'], p['y']
            before = (ax - p['viewX'], ay - p['viewY'])
            after = self.ask('gui_zoom', factor=factor, x=ax, y=ay)
            q = self.ask('gui_note_point', item=item)
            self.assertEqual(q['hit'], item, (factor, q))
            self.assertEqual(self.ask('gui_item_at', x=q['x'], y=q['y'])['item'], item)
            where = (q['x'] - after['viewX'], q['y'] - after['viewY'])
            # a page narrower than the view cannot scroll sideways (0.5), and 3x may hit the page edge: x is only
            # checked at 1.5; y is free to move, so it is checked whenever the zoom grows
            if factor == 1.5:
                self.assertAlmostEqual(where[0], before[0], delta=4)
            if factor > 1:
                self.assertAlmostEqual(where[1], before[1], delta=4)

    def test_42_shift_wheel_zooms_by_ten_percent_a_notch_plain_wheel_does_not(self):
        self.ask('gui_zoom', reset=True)
        r = self.ask('gui_wheel', rotation=-1, x=200, y=200)
        self.assertAlmostEqual(r['zoomRelative'], 1.1, places=3)
        r = self.ask('gui_wheel', rotation=1, x=200, y=200)
        self.assertAlmostEqual(r['zoomRelative'], 1.0, places=3)
        r = self.ask('gui_wheel', rotation=-0.5, x=200, y=200)       # a precision wheel
        self.assertAlmostEqual(r['zoomRelative'], 1.1 ** 0.5, places=3)
        z = r['zoomRelative']
        r = self.ask('gui_wheel', rotation=-3, x=200, y=200, shift=False)   # plain wheel scrolls, never zooms
        self.assertAlmostEqual(r['zoomRelative'], z, places=6)
        r = self.ask('gui_wheel', rotation=-3, x=200, y=200, ctrl=True)     # Ctrl+Shift+wheel is left alone
        self.assertAlmostEqual(r['zoomRelative'], z, places=6)
        for _ in range(40):
            r = self.ask('gui_wheel', rotation=-1, x=200, y=200)
        self.assertAlmostEqual(r['zoomRelative'], 4.0, places=3)

    def test_43_zoom_survives_loading_a_score_and_ctrl0_style_reset_refits(self):
        self.ask('gui_zoom', factor=1.7, x=0, y=0)
        self.assertTrue(self.ask('gui_load', path=CAPRICE2)['loaded'])
        st = self.ask('gui_state')
        self.assertTrue(st['userZoomed'])
        self.assertAlmostEqual(st['zoomRelative'], 1.7, delta=0.05)
        r = self.ask('gui_zoom', reset=True)
        self.assertFalse(r['userZoomed'])
        self.assertAlmostEqual(r['zoomRelative'], 1.0, places=3)


if __name__ == '__main__':
    sys.exit(0 if unittest.main(exit=False).result.wasSuccessful() else 1)
