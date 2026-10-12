"""Explicit hardware feasibility test. Keeps mute confirmed during every transport action."""
import argparse
import base64
import json
import struct
import time
import xml.etree.ElementTree as ET
from pathlib import Path
from linn_session import Session

ROOT = Path(__file__).resolve().parents[1]
STATE = ROOT / 'research/linn-muted-test-state.json'


def ids(session):
    raw = base64.b64decode(session.call('Playlist', 'IdArray')['Array'])
    return list(struct.unpack('>' + str(len(raw) // 4) + 'I', raw))


def muted(session):
    if session.call('Volume', 'Mute')['Value'] not in ('1', 'true'):
        raise RuntimeError('Mute confirmation failed; transport refused')


def save(state):
    STATE.write_text(json.dumps(state, indent=2), encoding='utf-8')


def cleanup(s, state):
    s.call('Volume', 'SetMute', Value=1)
    muted(s)
    s.call('Playlist', 'Stop')
    for track in state['inserted']:
        if track in ids(s):
            s.call('Playlist', 'DeleteId', Value=track)
    if state['originalId'] in ids(s):
        s.call('Playlist', 'SeekId', Value=state['originalId'])
    s.call('Product', 'SetSourceIndex', Value=state['source'])
    if ids(s) != state['originalIds']:
        raise RuntimeError('Queue restoration mismatch; leaving mute enabled')
    if s.call('Volume', 'Volume')['Value'] != state['volume']:
        raise RuntimeError('Volume changed externally; leaving mute enabled')
    if s.call('Product', 'SourceIndex')['Value'] != state['source']:
        raise RuntimeError('Source restoration failed; leaving mute enabled')
    if s.call('Playlist', 'TransportState')['Value'] != 'Stopped':
        raise RuntimeError('Playlist not stopped; leaving mute enabled')
    s.call('Volume', 'SetMute', Value=state['mute'])
    state['restored'] = True
    save(state)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('action', choices=['start', 'observe', 'cleanup'])
    args = p.parse_args()
    s = Session((ROOT / 'research/linn-session-location.txt').read_text())
    if args.action != 'start':
        state = json.loads(STATE.read_text())
        try:
            if args.action == 'observe':
                deadline = time.monotonic() + 45
                samples = []
                while time.monotonic() < deadline:
                    muted(s)
                    current = s.call('Playlist', 'Id')['Value']
                    transport = s.call('Playlist', 'TransportState')['Value']
                    timing = s.call('Time', 'Time')
                    samples.append(dict(id=current, transport=transport, time=timing))
                    if current == str(state['inserted'][1]) and transport == 'Playing' and int(timing['Seconds']) >= 2:
                        state['autonomousNextTrack'] = True
                        break
                    time.sleep(2)
                state['samples'] = samples
                save(state)
                print(json.dumps({'autonomousNextTrack': state.get('autonomousNextTrack', False), 'samples': len(samples)}))
        finally:
            cleanup(s, state)
        return
    original = ids(s)
    state = dict(originalIds=original, originalId=int(s.call('Playlist', 'Id')['Value']),
                 source=s.call('Product', 'SourceIndex')['Value'], volume=s.call('Volume', 'Volume')['Value'],
                 mute=s.call('Volume', 'Mute')['Value'], inserted=[], restored=False)
    if s.call('Playlist', 'TransportState')['Value'] != 'Stopped':
        raise RuntimeError('Refusing to interrupt an active playlist')
    if len(original) < 2 or len(original) + 2 > int(s.call('Playlist', 'TracksMax')['Value']):
        raise RuntimeError('Insufficient test tracks or queue capacity')
    save(state)
    try:
        tracks = [s.call('Playlist', 'Read', Id=i) for i in original[:2]]
        if any(not t['Uri'].startswith('tidal://') for t in tracks):
            raise RuntimeError('Test requires existing firmware-produced TIDAL references')
        s.call('Volume', 'SetMute', Value=1)
        muted(s)
        after = original[-1]
        for track in tracks:
            muted(s)
            result = s.call('Playlist', 'Insert', AfterId=after, Uri=track['Uri'], Metadata=track['Metadata'])
            after = int(result['NewId'])
            state['inserted'].append(after)
            save(state)
        s.call('Product', 'SetSourceIndexByName', Value='Playlist')
        muted(s)
        s.call('Playlist', 'SeekId', Value=state['inserted'][0])
        muted(s)
        s.call('Playlist', 'Play')
        deadline = time.monotonic() + 15
        while time.monotonic() < deadline:
            muted(s)
            timing = s.call('Time', 'Time')
            if s.call('Playlist', 'TransportState')['Value'] == 'Playing' and int(timing['Seconds']) >= 2:
                duration = int(timing['Duration'])
                if duration < 12:
                    raise RuntimeError('Unexpected duration')
                s.call('Playlist', 'SeekSecondAbsolute', Value=duration-8)
                state['firstTrackPlayingMuted'] = True
                save(state)
                print('Muted TIDAL playback confirmed; controller exits now. Run observe immediately.')
                return
            time.sleep(1)
        raise RuntimeError('TIDAL playback was not confirmed')
    except BaseException:
        cleanup(s, state)
        raise


if __name__ == '__main__':
    main()
