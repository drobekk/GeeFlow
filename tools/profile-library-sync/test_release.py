"""Offline migration and release safety checks; all catalogs are temporary fixtures."""
import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from migrate_catalog import prepare
from package_release import package, WEB

class ReleaseTests(unittest.TestCase):
    def test_migration_keeps_original_and_backup_and_refuses_overwrite(self):
        with tempfile.TemporaryDirectory() as root:
            source=Path(root)/'original.json';destination=Path(root)/'catalog-v02.json'
            raw=b'{"schemaVersion":1,"revision":7,"profiles":[]}\n'
            source.write_bytes(raw)
            migrated=prepare(source,destination)
            self.assertEqual(source.read_bytes(),raw)
            self.assertEqual(destination.with_suffix('.backup.json').read_bytes(),raw)
            self.assertEqual(migrated,{'schemaVersion':2,'revision':8,'profiles':[]})
            with self.assertRaises(ValueError):prepare(source,destination)
    def test_bundle_has_all_modules_and_no_catalog_or_credentials(self):
        with tempfile.TemporaryDirectory() as root:
            destination=Path(root)/'release-v01';manifest=package(destination)
            with zipfile.ZipFile(destination/'profile-library-release.zip') as archive:
                names=set(archive.namelist())
                self.assertTrue(all('web/'+name in names for name in WEB))
                self.assertIn('service/program_validation.py',names)
                self.assertIn('web/recipes/decent/best_practice.json',names)
                self.assertIn('web/decent-editor.html', names)
                self.assertIn('web/decent-editor.js', names)
                self.assertIn(b'href="./decent-editor.html"', archive.read('web/index.html'))
                self.assertIn(b'src="./decent-editor.js"', archive.read('web/decent-editor.html'))
                self.assertFalse(any('catalog.json' in name or '.env' in name or 'token' in name for name in names))
                self.assertFalse(manifest['deployed'])
                self.assertEqual(len(manifest['sourceCommit']),40)
                self.assertEqual(len(names),len(manifest['files'])+1)
            with self.assertRaises(ValueError):package(destination)

if __name__=='__main__':unittest.main()
