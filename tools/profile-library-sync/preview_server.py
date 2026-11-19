#!/usr/bin/env python3
"""Loopback editor sandbox for temporary catalogs and uploaded app database copies."""
import argparse
from contextlib import closing
import json
import mimetypes
import shutil
import sqlite3
import tempfile
import threading
import uuid
from http.server import ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlsplit
import server
from package_release import ROOT, WEB

MAX_APP_DATABASE_BYTES = 64 * 1024 * 1024
MAX_DATABASE_UPDATE_BYTES = 16 * 1024 * 1024
DATABASE_STATUS_PATH = "/profile-library/api/local-db/status"
DATABASE_OPEN_PATH = "/profile-library/api/local-db/open"
DATABASE_DOWNLOAD_PATH = "/profile-library/api/local-db/download"
DATABASE_SESSIONS: dict[str, dict] = {}
DATABASE_SESSIONS_LOCK = threading.RLock()
DATABASE_SESSION_ROOT: Path | None = None


class PreviewHandler(server.Handler):
    def do_GET(self):
        path = urlsplit(self.path).path
        if path == DATABASE_STATUS_PATH:
            self._send_json(200, {"available": DATABASE_SESSION_ROOT is not None})
            return
        if path.startswith("/profile-library/api/"):
            return super().do_GET()
        relative = path.removeprefix("/profile-library/").strip("/") or "index.html"
        allowed = set(WEB) | {f"recipes/decent/{item.name}" for item in (ROOT / "recipes/decent").glob("*.json")}
        if not path.startswith("/profile-library/") or relative not in allowed:
            self.send_error(404)
            return
        data = (ROOT / relative).read_bytes()
        content_type = mimetypes.guess_type(relative)[0] or "application/octet-stream"
        self.send_response(200)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(data)

    def do_POST(self):
        path = urlsplit(self.path).path
        if path == DATABASE_OPEN_PATH:
            self._open_app_database()
            return
        if path == DATABASE_DOWNLOAD_PATH:
            self._download_app_database()
            return
        self._send_json(404, {"error": "not_found"})

    def _open_app_database(self):
        if DATABASE_SESSION_ROOT is None:
            self._send_json(404, {"error": "local_database_editor_unavailable"})
            return
        session_directory = None
        session_id = None
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length <= 0 or length > MAX_APP_DATABASE_BYTES:
                self._send_json(413, {
                    "error": "invalid_database_size",
                    "message": "The app database must be smaller than 64 MB.",
                })
                return
            raw_database = self.rfile.read(length)
            if not raw_database.startswith(b"SQLite format 3\x00"):
                raise ValueError("Choose the app.db SQLite database file.")

            session_id = uuid.uuid4().hex
            session_directory = DATABASE_SESSION_ROOT / session_id
            session_directory.mkdir()
            database_path = session_directory / "app.db"
            database_path.write_bytes(raw_database)
            records, row_ids = read_app_profiles(database_path)
            with DATABASE_SESSIONS_LOCK:
                DATABASE_SESSIONS[session_id] = {
                    "directory": session_directory,
                    "database_path": database_path,
                    "row_ids": row_ids,
                }
            filename = unquote(self.headers.get("X-Database-Name", "app.db"))
            response = {
                "sessionId": session_id,
                "fileName": Path(filename).name or "app.db",
                "profileCount": len(records),
                "catalog": {"schemaVersion": 2, "revision": 0, "profiles": records},
            }
            self._send_json(200, response)
        except (OSError, sqlite3.DatabaseError, ValueError, json.JSONDecodeError) as error:
            if session_directory is not None and session_id is not None:
                with DATABASE_SESSIONS_LOCK:
                    if session_id not in DATABASE_SESSIONS:
                        shutil.rmtree(session_directory, ignore_errors=True)
            self._send_json(400, {"error": "invalid_app_database", "message": str(error)})

    def _download_app_database(self):
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length <= 0 or length > MAX_DATABASE_UPDATE_BYTES:
                self._send_json(413, {
                    "error": "invalid_update_size",
                    "message": "The edited profiles exceed the local editor's request size limit.",
                })
                return
            request = json.loads(self.rfile.read(length))
            session_id = request.get("sessionId") if isinstance(request, dict) else None
            updates = request.get("updates") if isinstance(request, dict) else None
            if not isinstance(session_id, str) or not isinstance(updates, list):
                raise ValueError("The database edit session is invalid.")
            with DATABASE_SESSIONS_LOCK:
                session = DATABASE_SESSIONS.get(session_id)
            if session is None:
                self._send_json(410, {"error": "database_session_expired"})
                return
            database_path = session["directory"] / f"edited-{uuid.uuid4().hex}.db"
            shutil.copyfile(session["database_path"], database_path)
            try:
                update_app_profiles(database_path, session["row_ids"], updates)
                database_bytes = database_path.read_bytes()
            finally:
                database_path.unlink(missing_ok=True)
            self.send_response(200)
            self.send_header("Content-Type", "application/vnd.sqlite3")
            self.send_header("Content-Length", str(len(database_bytes)))
            self.send_header("Content-Disposition", 'attachment; filename="geeflow-app-edited.db"')
            self.send_header("Cache-Control", "no-store")
            self.send_header("X-Content-Type-Options", "nosniff")
            self.end_headers()
            self.wfile.write(database_bytes)
        except (OSError, sqlite3.DatabaseError, ValueError, json.JSONDecodeError) as error:
            self._send_json(400, {"error": "database_update_failed", "message": str(error)})


def read_app_profiles(database_path: Path) -> tuple[list[dict], dict[str, int]]:
    with closing(sqlite3.connect(f"{database_path.as_uri()}?mode=ro", uri=True)) as connection:
        connection.row_factory = sqlite3.Row
        check = connection.execute("PRAGMA quick_check").fetchone()[0]
        if check != "ok":
            raise ValueError("The app database failed SQLite's integrity check.")
        tables = {row[0] for row in connection.execute("SELECT name FROM sqlite_master WHERE type='table'")}
        if "brew_profiles" not in tables:
            raise ValueError("This database does not contain GeeFlow brew profiles.")
        columns = {row[1] for row in connection.execute("PRAGMA table_info(brew_profiles)")}
        required = {
            "id", "name", "description", "finishCondition", "autoLinkOpen",
            "singleDoseEnabled", "singleDoseGrindingSize", "singleDoseGrindingSpeed", "program", "position",
        }
        if missing := required - columns:
            raise ValueError(f"This app database is missing profile fields: {', '.join(sorted(missing))}.")
        rows = connection.execute(
            "SELECT id, name, description, finishCondition, autoLinkOpen, singleDoseEnabled, "
            "singleDoseGrindingSize, singleDoseGrindingSpeed, program FROM brew_profiles ORDER BY position, id"
        ).fetchall()

    records = []
    row_ids = {}
    for row in rows:
        record_id = f"appdb-{row['id']}"
        finish_condition = json.loads(row["finishCondition"]) if row["finishCondition"] else None
        program = json.loads(row["program"])
        if not isinstance(program, dict) or program.get("type") not in ("phases", "recording"):
            raise ValueError(f"Profile {row['name']!r} has an unsupported program format.")
        row_ids[record_id] = int(row["id"])
        records.append({
            "id": record_id,
            "revision": 0,
            "favorite": False,
            "deleted": False,
            "profile": {
                "name": row["name"],
                "description": row["description"],
                "finishCondition": finish_condition,
                "autoLinkOpen": bool(row["autoLinkOpen"]),
                "singleDoseSettings": {
                    "enabled": bool(row["singleDoseEnabled"]),
                    "grindingSize": int(row["singleDoseGrindingSize"]),
                    "grindingSpeed": int(row["singleDoseGrindingSpeed"]),
                },
                "program": program,
            },
        })
    return records, row_ids


def update_app_profiles(database_path: Path, row_ids: dict[str, int], updates: list[dict]) -> None:
    allowed_fields = {"name", "description", "finishCondition", "autoLinkOpen", "singleDoseSettings", "program"}
    seen = set()
    with closing(sqlite3.connect(database_path)) as connection:
        connection.execute("PRAGMA trusted_schema=OFF")
        connection.execute("BEGIN IMMEDIATE")
        for update in updates:
            if not isinstance(update, dict):
                raise ValueError("A profile edit is malformed.")
            record_id = update.get("id")
            fields = update.get("fields")
            profile = update.get("profile")
            if (
                not isinstance(record_id, str)
                or record_id not in row_ids
                or record_id in seen
                or not isinstance(fields, list)
                or any(not isinstance(field, str) for field in fields)
                or not isinstance(profile, dict)
            ):
                raise ValueError("A profile edit does not match the opened database.")
            seen.add(record_id)
            if not fields or len(fields) != len(set(fields)) or set(fields) - allowed_fields:
                raise ValueError("A profile edit contains unsupported fields.")
            assignments = []
            values = []
            for field in fields:
                if field == "name":
                    value = profile.get(field)
                    if not isinstance(value, str) or not value.strip() or len(value) > 100:
                        raise ValueError("Profile names must contain 1 to 100 characters.")
                    assignments.append("name = ?")
                    values.append(value)
                elif field == "description":
                    value = profile.get(field)
                    if not isinstance(value, str):
                        raise ValueError("Profile descriptions must be text.")
                    assignments.append("description = ?")
                    values.append(value)
                elif field == "finishCondition":
                    value = profile.get(field)
                    if value is not None and (
                        not isinstance(value, dict)
                        or value.get("type") not in ("weight", "volume")
                        or set(value) != {"type", "target"}
                        or type(value.get("target")) not in (int, float)
                        or value["target"] <= 0
                    ):
                        raise ValueError("The finish condition is invalid.")
                    assignments.append("finishCondition = ?")
                    values.append(json.dumps(value, ensure_ascii=False, separators=(",", ":")) if value is not None else None)
                elif field == "autoLinkOpen":
                    value = profile.get(field)
                    if not isinstance(value, bool):
                        raise ValueError("Auto-link must be on or off.")
                    assignments.append("autoLinkOpen = ?")
                    values.append(int(value))
                elif field == "singleDoseSettings":
                    settings = profile.get(field)
                    if not isinstance(settings, dict):
                        raise ValueError("Single-dose settings are invalid.")
                    enabled = settings.get("enabled")
                    size = settings.get("grindingSize")
                    speed = settings.get("grindingSpeed")
                    if not isinstance(enabled, bool) or type(size) is not int or not 0 <= size <= 600 or type(speed) is not int or not 200 <= speed <= 1000:
                        raise ValueError("Single-dose settings are outside the app's supported range.")
                    assignments.extend(("singleDoseEnabled = ?", "singleDoseGrindingSize = ?", "singleDoseGrindingSpeed = ?"))
                    values.extend((int(enabled), size, speed))
                elif field == "program":
                    value = profile.get(field)
                    if (
                        not isinstance(value, dict)
                        or value.get("type") not in ("phases", "recording")
                        or value.get("version", 1) not in (1, 2)
                    ):
                        raise ValueError("The brew program is invalid.")
                    assignments.append("program = ?")
                    values.append(json.dumps(value, ensure_ascii=False, separators=(",", ":")))
            cursor = connection.execute(
                f"UPDATE brew_profiles SET {', '.join(assignments)} WHERE id = ?",
                (*values, row_ids[record_id]),
            )
            if cursor.rowcount != 1:
                raise ValueError("A profile row could not be updated.")
        connection.commit()
        connection.execute("PRAGMA wal_checkpoint(TRUNCATE)")
        connection.execute("PRAGMA journal_mode=DELETE")
        check = connection.execute("PRAGMA quick_check").fetchone()[0]
        if check != "ok":
            raise ValueError("The edited database failed SQLite's integrity check.")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--seed", type=Path, help="Copy a local catalog into the temporary sandbox")
    args = parser.parse_args()
    with tempfile.TemporaryDirectory(prefix="geeflow-editor-sandbox-") as temporary:
        temporary_path = Path(temporary)
        DATABASE_SESSION_ROOT = temporary_path / "app-databases"
        DATABASE_SESSION_ROOT.mkdir()
        server.DATA_FILE = temporary_path / "catalog.json"
        server.PROCESS_LOCK_FILE = temporary_path / "catalog.lock"
        server.TOKEN = "preview-only"
        if args.seed:
            raw = args.seed.read_bytes()
            server.validate_catalog(json.loads(raw))
            server.DATA_FILE.write_bytes(raw)
        print(
            f"Editor sandbox: http://127.0.0.1:{args.port}/profile-library/",
            "Token: preview-only",
            "All saves are temporary and disappear when this process stops.",
            sep="\n",
            flush=True,
        )
        ThreadingHTTPServer(("127.0.0.1", args.port), PreviewHandler).serve_forever()
