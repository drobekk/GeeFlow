#!/usr/bin/env python3
"""Prepare a v2 catalog and byte-for-byte backup locally. Never replace a live file."""
import argparse
import json
from pathlib import Path
from server import validate_catalog

def prepare(source: Path, destination: Path):
    raw=source.read_bytes();catalog=json.loads(raw);validate_catalog(catalog)
    if destination.exists() or destination.with_suffix('.backup.json').exists():
        raise ValueError('Choose unused output and backup paths')
    migrated=json.loads(raw)
    if migrated['schemaVersion']==1:
        migrated['schemaVersion']=2;migrated['revision']+=1
    validate_catalog(migrated)
    destination.parent.mkdir(parents=True,exist_ok=True)
    with destination.with_suffix('.backup.json').open('xb') as file:file.write(raw)
    with destination.open('x',encoding='utf-8') as file:json.dump(migrated,file,ensure_ascii=False,indent=2);file.write('\n')
    return migrated

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('source',type=Path);parser.add_argument('destination',type=Path)
    args=parser.parse_args();prepare(args.source,args.destination)
    print('Prepared new catalog and backup. No live file was replaced.')
