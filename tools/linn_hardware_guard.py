"""Save/restore the actual renderer for explicitly authorized muted app QA."""
import argparse
import json
import time
from pathlib import Path
from linn_session import Session
from linn_muted_test import ids, muted
ROOT=Path(__file__).resolve().parents[1]
FILE=ROOT/'research/linn-app-qa-backup.json'

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('action',choices=['begin','status','restore']);a=p.parse_args()
    s=Session((ROOT/'research/linn-session-location.txt').read_text())
    if a.action=='begin':
        if FILE.exists() and not json.loads(FILE.read_text(encoding='utf-8')).get('restored'):
            raise SystemExit('An unrestored QA backup already exists')
        if s.call('Playlist','TransportState')['Value']!='Stopped':raise SystemExit('Refusing to interrupt active playback')
        original=ids(s)
        backup=dict(ids=original,tracks=[s.call('Playlist','Read',Id=i) for i in original],
                    selected=s.call('Playlist','Id')['Value'],source=s.call('Product','SourceIndex')['Value'],
                    mute=s.call('Volume','Mute')['Value'],volume=s.call('Volume','Volume')['Value'],
                    shuffle=s.call('Playlist','Shuffle')['Value'],repeat=s.call('Playlist','Repeat')['Value'],restored=False)
        FILE.write_text(json.dumps(backup),encoding='utf-8')
        s.call('Volume','SetMute',Value=1);muted(s)
        print(json.dumps({'muteConfirmed':True,'savedQueueEntries':len(original)}))
    elif a.action=='status':
        print(json.dumps({'mute':s.call('Volume','Mute')['Value'],'volume':s.call('Volume','Volume')['Value'],
              'state':s.call('Playlist','TransportState')['Value'],'queueEntries':len(ids(s)),
              'time':s.call('Time','Time')}))
    else:
        backup=json.loads(FILE.read_text(encoding='utf-8'))
        s.call('Volume','SetMute',Value=1);muted(s);s.call('Playlist','Stop')
        selected=backup['selected']
        if ids(s)!=backup['ids']:
            s.call('Playlist','DeleteAll');after=0
            for old_id,track in zip(backup['ids'],backup['tracks']):
                result=s.call('Playlist','Insert',AfterId=after,Uri=track['Uri'],Metadata=track['Metadata']);after=int(result['NewId'])
                if str(old_id)==backup['selected']:selected=str(after)
        if selected!='0':s.call('Playlist','SeekId',Value=selected)
        s.call('Playlist','SetShuffle',Value=backup['shuffle']);s.call('Playlist','SetRepeat',Value=backup['repeat'])
        s.call('Playlist','Stop');s.call('Product','SetSourceIndex',Value=backup['source'])
        restored_ids=ids(s)
        if len(restored_ids)!=len(backup['ids']):raise SystemExit('Queue restore failed; mute remains enabled')
        for restored_id,original_track in zip(restored_ids,backup['tracks']):
            restored_track=s.call('Playlist','Read',Id=restored_id)
            if restored_track!=original_track:raise SystemExit('Queue content restore failed; mute remains enabled')
        for attempt in range(10):
            if s.call('Playlist','TransportState')['Value']=='Stopped':break
            time.sleep(0.3)
        else:raise SystemExit('Transport restore failed; mute remains enabled')
        if s.call('Playlist','Shuffle')['Value']!=backup['shuffle'] or s.call('Playlist','Repeat')['Value']!=backup['repeat']:
            raise SystemExit('Queue mode restore failed; mute remains enabled')
        if s.call('Volume','Volume')['Value']!=backup['volume']:raise SystemExit('Volume changed externally; mute remains enabled')
        if s.call('Product','SourceIndex')['Value']!=backup['source']:raise SystemExit('Source restore failed; mute remains enabled')
        s.call('Volume','SetMute',Value=backup['mute']);backup['restored']=True
        FILE.write_text(json.dumps(backup),encoding='utf-8');print('Original queue, source, stopped transport and mute restored')

if __name__=='__main__':main()
