#!/usr/bin/env python3
"""Create a versioned, credential-free local release bundle; never deploy."""
import argparse
import hashlib
import json
import subprocess
import zipfile
from pathlib import Path
ROOT=Path(__file__).parent
WEB=['index.html','catalog.schema.json','editor-ui.js','editor-core.js','editor-workflow.js','recipe-contract.js','recipe-runtime.js','recipe-inspector.js','decent-import.js','recipe-controls.js','decent-editor.html','decent-editor.js']
SERVICE=['server.py','program_validation.py','migrate_catalog.py','geeflow-profile-library.service','geeflow-profile-library-nginx.conf','README.md']

def package(destination):
    if destination.exists():raise ValueError('Destination already exists; choose a new version')
    destination.mkdir(parents=True)
    manifest={'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'deployed':False,'files':{}}
    with zipfile.ZipFile(destination/'profile-library-release.zip','x',zipfile.ZIP_DEFLATED) as archive:
        entries=[('web/'+name,ROOT/name) for name in WEB]+[('service/'+name,ROOT/name) for name in SERVICE]
        entries += [('web/recipes/decent/'+p.name,p) for p in (ROOT/'recipes/decent').glob('*')]
        for name,path in entries:
            data=path.read_bytes();manifest['files'][name]={'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()};archive.writestr(name,data)
        archive.writestr('manifest.json',json.dumps(manifest,indent=2)+'\n')
    (destination/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    return manifest
if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('destination',type=Path);args=parser.parse_args();package(args.destination);print('Local release bundle prepared; nothing deployed.')
