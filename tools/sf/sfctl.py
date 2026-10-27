#!/usr/bin/env python3 -I
"""Send one command to a running String Fingering window (started with --control) and print the reply.

    sfctl.py gui_state
    sfctl.py gui_load_abc path=/home/eliot/abc_scores/tune.abc
    sfctl.py gui_next
    sfctl.py gui_screenshot path=/home/eliot/shot.png
    sfctl.py chord 'pitches=["C","E","G"]'

Values after name= are read as JSON when they parse as JSON (numbers, lists), else as text.  The port is read
from ~/.string_fingering/control.port, or from the file in $SF_CONTROL_FILE.  The reply is printed as one JSON line; the exit status is 1 if "ok" is false.
"""
import json
import os
import socket
import sys


def main(argv):
    if len(argv) < 2:
        sys.exit(__doc__)
    port_file = os.environ.get('SF_CONTROL_FILE', os.path.expanduser('~/.string_fingering/control.port'))
    try:
        port = int(open(port_file).read())
    except OSError:
        sys.exit('no running window: %s does not exist (start the window with --control)' % port_file)
    request = {'cmd': argv[1]}
    for arg in argv[2:]:
        name, _, value = arg.partition('=')
        try:
            request[name] = json.loads(value)
        except ValueError:
            request[name] = value
    with socket.create_connection(('127.0.0.1', port), timeout=120) as s:
        s.sendall((json.dumps(request) + '\n').encode())
        reply = s.makefile('r').readline()
    print(reply.strip())
    return 0 if json.loads(reply).get('ok') else 1


if __name__ == '__main__':
    sys.exit(main(sys.argv))
