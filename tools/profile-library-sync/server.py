#!/usr/bin/env python3
"""Small authenticated JSON catalog API for the GeeFlow profile library."""

from __future__ import annotations

from program_validation import validate_program, fields, integer, number
import hmac
import json
import os
import tempfile
import threading
import uuid
from contextlib import contextmanager
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any
from typing import Iterator


SCHEMA_VERSION = 1
MAX_BODY_BYTES = 1_000_000
DATA_FILE = Path(os.environ.get("PROFILE_LIBRARY_DATA_FILE", "/var/lib/geeflow-profile-library/catalog.json"))
TOKEN = os.environ.get("PROFILE_LIBRARY_TOKEN", "")
CATALOG_LOCK = threading.RLock()
PROCESS_LOCK_FILE = DATA_FILE.with_name("catalog.lock")


def empty_catalog() -> dict[str, Any]:
    return {"schemaVersion": SCHEMA_VERSION, "revision": 0, "profiles": []}


def load_catalog() -> dict[str, Any]:
    try:
        value = json.loads(DATA_FILE.read_text(encoding="utf-8"))
    except FileNotFoundError:
        return empty_catalog()
    validate_catalog(value)
    return value


def validate_catalog(value: Any) -> None:
    if not isinstance(value, dict) or (type(value.get("schemaVersion")) is not int or value.get("schemaVersion") not in (1, 2)):
        raise ValueError("Unsupported catalog schema")
    if not integer(value.get("revision")) or value["revision"] < 0:
        raise ValueError("Catalog revision must be a non-negative integer")
    fields(value,"schemaVersion revision profiles","catalog")
    records = value.get("profiles")
    if not isinstance(records, list):
        raise ValueError("Catalog profiles must be an array")

    seen: set[str] = set()
    for record in records:
        if not isinstance(record, dict):
            raise ValueError("Each profile record must be an object")
        fields(record,"id revision favorite deleted profile","catalog record")
        profile_id = record.get("id")
        try:
            uuid.UUID(profile_id)
        except (ValueError, TypeError, AttributeError) as error:
            raise ValueError("Profile id must be a UUID") from error
        if profile_id in seen:
            raise ValueError("Profile ids must be unique")
        seen.add(profile_id)
        if not integer(record.get("revision")) or record["revision"] < 0:
            raise ValueError("Profile revision must be a non-negative integer")
        if not isinstance(record.get("favorite"), bool) or not isinstance(record.get("deleted"), bool):
            raise ValueError("Profile favorite and deleted fields must be booleans")
        body = record.get("profile")
        if not isinstance(body, dict):
            raise ValueError("Profile body must be an object")
        fields(body,"name description autoLinkOpen singleDoseSettings program finishCondition","profile")
        for key in ("name", "description", "autoLinkOpen", "singleDoseSettings", "program"):
            if key not in body:
                raise ValueError(f"Profile body is missing {key}")
        if not isinstance(body["name"], str) or not body["name"].strip():
            raise ValueError("Profile name cannot be empty")
        if not isinstance(body["description"], str):
            raise ValueError("Profile description must be a string")
        if not isinstance(body["autoLinkOpen"], bool):
            raise ValueError("autoLinkOpen must be a boolean")
        if not isinstance(body["singleDoseSettings"], dict) or not isinstance(body["program"], dict):
            raise ValueError("singleDoseSettings and program must be objects")
        dose = body['singleDoseSettings']
        fields(dose,'enabled grindingSize grindingSpeed','single-dose settings')
        if not isinstance(dose.get('enabled'), bool) or not integer(dose.get('grindingSize'),0,600) or not integer(dose.get('grindingSpeed'),200,1000):
            raise ValueError('Invalid single-dose settings')
        validate_program(body['program'])
        if body['program'].get('version', 1) == 2 and value['schemaVersion'] != 2:
            raise ValueError('Program v2 requires catalog schema v2')
        finish = body.get('finishCondition')
        if finish is not None:
            if not isinstance(finish, dict): raise ValueError('Finish condition must be an object')
            fields(finish,"type target","finish condition")
            if finish.get('type') not in ('weight','volume') or not number(finish.get('target'), 0.0001):
                raise ValueError('Finish target must be positive')
        if body.get("finishCondition") is not None and not isinstance(body["finishCondition"], dict):
            raise ValueError("finishCondition must be an object or null")


def write_catalog(value: dict[str, Any]) -> None:
    DATA_FILE.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_path = tempfile.mkstemp(prefix="catalog-", suffix=".json", dir=DATA_FILE.parent)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8") as temporary:
            json.dump(value, temporary, ensure_ascii=False, indent=2, sort_keys=True)
            temporary.write("\n")
            temporary.flush()
            os.fsync(temporary.fileno())
        os.replace(temporary_path, DATA_FILE)
        os.chmod(DATA_FILE, 0o640)
    finally:
        if os.path.exists(temporary_path):
            os.unlink(temporary_path)


@contextmanager
def process_catalog_lock() -> Iterator[None]:
    import fcntl

    PROCESS_LOCK_FILE.parent.mkdir(parents=True, exist_ok=True)
    descriptor = os.open(PROCESS_LOCK_FILE, os.O_CREAT | os.O_RDWR, 0o640)
    try:
        fcntl.flock(descriptor, fcntl.LOCK_EX)
        yield
    finally:
        fcntl.flock(descriptor, fcntl.LOCK_UN)
        os.close(descriptor)


def normalize_revisions(old: dict[str, Any], incoming: dict[str, Any]) -> dict[str, Any]:
    old_records = {record["id"]: record for record in old["profiles"]}
    updated_records: list[dict[str, Any]] = []
    for record in incoming["profiles"]:
        previous = old_records.get(record["id"])
        if previous is None:
            item_revision = 1
        else:
            unchanged = all(
                previous.get(key) == record.get(key)
                for key in ("favorite", "deleted", "profile")
            )
            item_revision = previous["revision"] if unchanged else previous["revision"] + 1
        updated_records.append({**record, "revision": item_revision})
    incoming["profiles"] = updated_records
    incoming["revision"] = old["revision"] + 1
    return incoming


class Handler(BaseHTTPRequestHandler):
    server_version = "GeeFlowProfileLibrary/1"
    api_path = "/profile-library/api/v1/catalog"
    health_path = "/profile-library/api/v1/health"

    def do_GET(self) -> None:  # noqa: N802
        if self.path == self.health_path:
            self._send_json(200, {"ok": True, "schemaVersion": SCHEMA_VERSION})
            return
        if self.path != self.api_path:
            self._send_json(404, {"error": "not_found"})
            return
        if not self._authorized():
            self._send_json(401, {"error": "unauthorized"})
            return
        try:
            catalog = load_catalog()
        except (OSError, ValueError, json.JSONDecodeError) as error:
            self._send_json(500, {"error": "catalog_invalid", "message": str(error)})
            return
        self._send_json(200, catalog, etag=str(catalog["revision"]))

    def do_PUT(self) -> None:  # noqa: N802
        if self.path != self.api_path:
            self._send_json(404, {"error": "not_found"})
            return
        if not self._authorized():
            self._send_json(401, {"error": "unauthorized"})
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length <= 0 or length > MAX_BODY_BYTES:
                self._send_json(413, {"error": "invalid_body_size"})
                return
            incoming = json.loads(self.rfile.read(length))
            validate_catalog(incoming)
        except (ValueError, json.JSONDecodeError) as error:
            self._send_json(400, {"error": "invalid_catalog", "message": str(error)})
            return

        with CATALOG_LOCK, process_catalog_lock():
            try:
                current = load_catalog()
            except (OSError, ValueError, json.JSONDecodeError) as error:
                self._send_json(500, {"error": "catalog_invalid", "message": str(error)})
                return

            if self.headers.get("If-Match", "").strip('"') != str(current["revision"]):
                self._send_json(
                    409,
                    {"error": "revision_conflict", "catalog": current},
                    etag=str(current["revision"]),
                )
                return

            if current['schemaVersion'] == 2 and incoming['schemaVersion'] != 2:
                self._send_json(400, {"error": "schema_downgrade_forbidden"})
                return
            current_ids = {record["id"] for record in current["profiles"]}
            incoming_ids = {record["id"] for record in incoming["profiles"]}
            if not current_ids.issubset(incoming_ids):
                self._send_json(400, {"error": "records_must_use_tombstones"})
                return

            try:
                updated = normalize_revisions(current, incoming)
                validate_catalog(updated)
                write_catalog(updated)
            except (OSError, ValueError) as error:
                self._send_json(500, {"error": "catalog_write_failed", "message": str(error)})
                return
        self._send_json(200, updated, etag=str(updated["revision"]))

    def log_message(self, format: str, *args: object) -> None:
        # Do not log request headers or bodies because they can contain the bearer token.
        print(f"profile-library {self.client_address[0]} {format % args}")

    def _authorized(self) -> bool:
        header = self.headers.get("Authorization", "")
        supplied = header.removeprefix("Bearer ").strip()
        return bool(TOKEN) and bool(supplied) and hmac.compare_digest(supplied, TOKEN)

    def _send_json(self, status: int, value: dict[str, Any], *, etag: str | None = None) -> None:
        body = json.dumps(value, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        if etag is not None:
            self.send_header("ETag", f'"{etag}"')
        self.end_headers()
        self.wfile.write(body)


def main() -> None:
    if not TOKEN:
        raise SystemExit("PROFILE_LIBRARY_TOKEN must be set")
    bind = os.environ.get("PROFILE_LIBRARY_BIND", "127.0.0.1")
    port = int(os.environ.get("PROFILE_LIBRARY_PORT", "8093"))
    server = ThreadingHTTPServer((bind, port), Handler)
    print(f"profile-library listening on {bind}:{port}")
    server.serve_forever()


if __name__ == "__main__":
    main()
