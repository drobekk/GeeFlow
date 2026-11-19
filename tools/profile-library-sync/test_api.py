"""Loopback tests for the same authenticated compare-and-swap API used by the editor."""

from __future__ import annotations

import importlib.util
import json
import tempfile
import threading
import unittest
import urllib.error
import urllib.request
from http.server import ThreadingHTTPServer
from pathlib import Path
from uuid import uuid4


SERVER_PATH = Path(__file__).with_name("server.py")
SPEC = importlib.util.spec_from_file_location("geeflow_profile_library_server", SERVER_PATH)
server_module = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(server_module)


class CatalogApiTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory(prefix="geeflow-profile-api-")
        root = Path(self.temporary.name)
        self.old_data_file = server_module.DATA_FILE
        self.old_lock_file = server_module.PROCESS_LOCK_FILE
        self.old_token = server_module.TOKEN
        server_module.DATA_FILE = root / "catalog.json"
        server_module.PROCESS_LOCK_FILE = root / "catalog.lock"
        server_module.TOKEN = "loopback-test-token"
        self.httpd = ThreadingHTTPServer(("127.0.0.1", 0), server_module.Handler)
        self.thread = threading.Thread(target=self.httpd.serve_forever, daemon=True)
        self.thread.start()
        self.url = f"http://127.0.0.1:{self.httpd.server_port}{server_module.Handler.api_path}"

    def tearDown(self) -> None:
        self.httpd.shutdown()
        self.httpd.server_close()
        self.thread.join(timeout=3)
        server_module.DATA_FILE = self.old_data_file
        server_module.PROCESS_LOCK_FILE = self.old_lock_file
        server_module.TOKEN = self.old_token
        self.temporary.cleanup()

    def request(self, method: str = "GET", *, token: str | None = "loopback-test-token", revision: int | None = None, body: dict | None = None):
        headers = {"Accept": "application/json"}
        if token is not None:
            headers["Authorization"] = f"Bearer {token}"
        if revision is not None:
            headers["If-Match"] = str(revision)
        encoded = None
        if body is not None:
            encoded = json.dumps(body).encode("utf-8")
            headers["Content-Type"] = "application/json"
        request = urllib.request.Request(self.url, data=encoded, headers=headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=3) as response:
                return response.status, json.loads(response.read()), response.headers
        except urllib.error.HTTPError as error:
            code = error.code
            headers = error.headers
            body = json.loads(error.read())
            error.close()
            return code, body, headers

    @staticmethod
    def profile_record() -> dict:
        return {
            "id": str(uuid4()),
            "revision": 0,
            "favorite": True,
            "deleted": False,
            "profile": {
                "name": "API curve",
                "description": "all fields survive catalog writes",
                "finishCondition": {"type": "weight", "target": 36.5},
                "autoLinkOpen": True,
                "singleDoseSettings": {"enabled": True, "grindingSize": 440, "grindingSpeed": 720},
                "program": {
                    "type": "phases",
                    "version": 1,
                    "phases": [{
                        "id": "phase-a",
                        "name": "Bloom",
                        "control": {"type": "pressure", "bar": 3.5, "location": "Group"},
                        "maximumDurationMillis": 9000,
                        "minimumDurationMillis": 1000,
                        "ramp": {"style": "Linear", "durationMillis": 5000, "start": "CurrentMeasurement"},
                        "exitConditions": [{"metric": "PumpFlow", "comparison": "Above", "threshold": 1.2, "scope": "Total"}],
                        "conditionOperator": "And",
                        "adaptiveFlowProbe": None,
                    }],
                },
            },
        }

    def test_catalog_get_requires_bearer_token(self) -> None:
        status, body, _ = self.request(token=None)
        self.assertEqual(status, 401)
        self.assertEqual(body["error"], "unauthorized")

        status, body, _ = self.request()
        self.assertEqual(status, 200)
        self.assertEqual(body, {"schemaVersion": 1, "revision": 0, "profiles": []})

    def test_save_and_get_round_trip_preserves_profile_fields(self) -> None:
        record = self.profile_record()
        initial = {"schemaVersion": 1, "revision": 0, "profiles": [record]}
        status, saved, headers = self.request("PUT", revision=0, body=initial)
        self.assertEqual(status, 200)
        self.assertEqual(headers.get("ETag"), '"1"')
        self.assertEqual(saved["revision"], 1)
        self.assertEqual(saved["profiles"][0]["revision"], 1)
        self.assertEqual(saved["profiles"][0]["profile"], record["profile"])

        status, fetched, _ = self.request()
        self.assertEqual(status, 200)
        self.assertEqual(fetched["profiles"][0], saved["profiles"][0])

    def test_stale_revision_returns_current_catalog_without_overwriting_it(self) -> None:
        record = self.profile_record()
        status, saved, _ = self.request("PUT", revision=0, body={"schemaVersion": 1, "revision": 0, "profiles": [record]})
        self.assertEqual(status, 200)

        stale = json.loads(json.dumps(saved))
        stale["profiles"][0]["profile"]["name"] = "stale edit"
        status, conflict, headers = self.request("PUT", revision=0, body=stale)
        self.assertEqual(status, 409)
        self.assertEqual(conflict["error"], "revision_conflict")
        self.assertEqual(conflict["catalog"]["profiles"][0]["profile"]["name"], "API curve")
        self.assertEqual(headers.get("ETag"), '"1"')

        status, current, _ = self.request()
        self.assertEqual(status, 200)
        self.assertEqual(current["profiles"][0]["profile"]["name"], "API curve")

    def test_feedback_range_round_trip(self):
        record=self.profile_record();program=record['profile']['program'];program['version']=2
        program['phases'][0]['secondaryLimit']={'metric':'PumpFlow','minimum':1.5,'ceiling':2.5}
        catalog={'schemaVersion':2,'revision':0,'profiles':[record]}
        status,saved,_=self.request('PUT',revision=0,body=catalog)
        self.assertEqual(status,200)
        self.assertEqual(saved['profiles'][0]['profile']['program'],program)
        self.assertEqual(self.request()[1]['profiles'][0]['profile']['program'],program)

    def test_feedback_rejects_invalid_ranges_and_dual_controller_fields(self):
        for minimum in (-1,2.5,3):
            record=self.profile_record();program=record['profile']['program'];program['version']=2
            program['phases'][0]['secondaryLimit']={'metric':'PumpFlow','minimum':minimum,'ceiling':2.5}
            self.assertEqual(self.request('PUT',revision=0,body={'schemaVersion':2,'revision':0,'profiles':[record]})[0],400)
        record=self.profile_record();record['profile']['program']['phases'][0]['control']['millilitresPerSecond']=2
        self.assertEqual(self.request('PUT',revision=0,body={'schemaVersion':1,'revision':0,'profiles':[record]})[0],400)
        self.assertEqual(self.request()[1]['revision'],0)

    def v2_catalog(self):
        return json.loads((Path(__file__).parents[2] / 'shared/app/src/jvmTest/resources/catalog-v2.json').read_text())

    def test_v2_round_trip_preserves_source_and_prevents_schema_downgrade(self):
        catalog=self.v2_catalog()
        status,saved,_=self.request('PUT',revision=0,body=catalog)
        self.assertEqual(status,200)
        self.assertEqual(saved['profiles'][0]['profile'],catalog['profiles'][0]['profile'])
        legacy=self.profile_record();legacy['id']=saved['profiles'][0]['id']
        status,error,_=self.request('PUT',revision=1,body={'schemaVersion':1,'revision':1,'profiles':[legacy]})
        self.assertEqual(status,400)
        self.assertEqual(error['error'],'schema_downgrade_forbidden')
        self.assertEqual(self.request()[1]['schemaVersion'],2)

    def test_unsafe_or_unknown_execution_fields_and_missing_captures_are_rejected(self):
        for mutate in (
            lambda p: p.update(version=3),
            lambda p: p['phases'][0].update(exitConditions=None),
            lambda p: p['recipe'].update(diagnostics='invalid report'),
            lambda p: p['recipe']['source'].update(sha256='z'*64),
            lambda p: p.update(execution={'maximumTotalDurationMillis':75000}),
            lambda p: p.update(unrecognizedController='unsafe'),
            lambda p: p['phases'][-1]['ramp'].update(source={'kind':'CapturedValue','capture':'undefined'}),
        ):
            catalog=self.v2_catalog();mutate(catalog['profiles'][0]['profile']['program'])
            status,_,_=self.request('PUT',revision=0,body=catalog)
            self.assertEqual(status,400)
        self.assertEqual(self.request()[1]['revision'],0)


if __name__ == "__main__":
    unittest.main()
